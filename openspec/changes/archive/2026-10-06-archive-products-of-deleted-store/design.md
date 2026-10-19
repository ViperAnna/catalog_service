# Design

## Context

Мотивация описана в proposal.md, требования — в delta specs четырех
capabilities.

Текущее состояние, которое определяет подход:
- **seller-service.** Outbox уже есть (`outbox_events`, `OutboxPublisher`,
  fixed rate 3 с). Но `OutboxPublisher` жестко публикует в
  `seller-events-topic`, а `SellerEventType.valueOf(eventType)` для любого
  другого типа бросает `IllegalArgumentException`, после чего запись
  помечается processed без публикации. `KafkaEvent.eventType` имеет тип
  `SellerEventType`. `StoreServiceImpl.deleteStore` удаляет магазин в
  `@Transactional` и outbox не использует.
- **catalog-service.** Зависимость `spring-kafka` и `.env.kafka` (с
  `SPRING_KAFKA_BOOTSTRAP_SERVERS`) подключены, но Kafka не используется,
  конфигурации `spring.kafka` в `application.yaml` нет.
- **Опыт notification-service** (`notifications` OBS-08, наблюдалось на
  стенде). Если при старте Kafka недоступна (`No resolvable bootstrap urls`),
  контейнер `@KafkaListener` не создается, и весь Spring-контекст падает.
  catalog-service — ключевой сервис, повторять это поведение нельзя. На стенде
  Kafka сейчас остановлена.
- `notification-service` потребляет `seller-events-topic` и десериализует
  eventType в свой enum. Новый тип в этом топике ушел бы у него в DLT. Поэтому
  нужен отдельный топик.

## Goals / Non-Goals

**Goals:**
- Атомарная запись события вместе с удалением магазина (outbox).
- Идемпотентная архивация на стороне каталога.
- catalog-service стартует и обслуживает REST без Kafka.
- Формат `seller-events-topic` и поведение notification-service не меняются.

**Non-Goals:**
- Общий модуль контрактов, единый конверт для всех топиков, schema registry.
- Скрытие архивных товаров из выдачи.

## Decisions

### D1. Событие `STORE_DELETED` в outbox seller-service
`StoreServiceImpl.deleteStore` после `storeRepo.delete(store)` сохраняет
`OutboxEvent` (aggregateType `STORE`, aggregateId = id магазина, eventType
`STORE_DELETED`, payload `{storeId, sellerId}` через существующий
`EventMapper`). Транзакция общая, поэтому при откате удаления события нет.
Добавляются `event/StoreEventType` (`STORE_DELETED`) и
`event/StoreDeletedEvent`. В конструктор `StoreServiceImpl` добавляются
`OutboxRepository` и `EventMapper`. Существующий `StoreServiceImplTest`
обновляется соответственно, другие классы этот конструктор не используют.

### D2. Маршрутизация в `OutboxPublisher` по aggregateType
Тип `KafkaEvent.eventType` меняется на `String`. Jackson сериализует enum по
имени, так что JSON в `seller-events-topic` остается тем же. Для каждой записи:
- `SELLER_APPLICATION`: проверка `SellerEventType.valueOf`, топик
  `seller-events-topic`;
- `STORE`: проверка `StoreEventType.valueOf`, топик `store-events-topic`;
- иной aggregateType или неизвестный eventType: запись помечается processed
  без публикации с записью в лог, как сейчас для неизвестного типа.

Остальная логика (top-100, синхронная отправка 10 с, остановка цикла при
ошибке) не меняется. В `KafkaConfig` seller-service добавляется
`NewTopic store-events-topic` (3 партиции, RF 1).

Альтернатива — отдельный outbox и отдельный publisher для магазинов.
Отклонена: дублирование того, что уже работает.

### D3. Потребитель в catalog-service
`@KafkaListener(topics = "store-events-topic", groupId = "catalog-group")`
принимает строку, разбирает конверт и при `STORE_DELETED` разбирает payload и
вызывает архивацию. Неизвестный eventType логируется, сообщение считается
обработанным. Конфигурация через `spring.kafka` в `application.yaml`:
bootstrap из `SPRING_KAFKA_BOOTSTRAP_SERVERS`, строковые десериализаторы.

Архивация — одно массовое обновление MongoDB через `MongoTemplate.updateMulti`
по `{store_id: X, status: {$ne: ARCHIVED}}` с `$set: {status: ARCHIVED}`.
Это идемпотентно: повтор ничего не находит. Атомарно на уровне каждого
документа. Остальные поля не трогаются.

### D4. Обработка ошибок и DLT
Как в notification-service: `DefaultErrorHandler` с
`DeadLetterPublishingRecoverer` и `FixedBackOff(1000, 3)`, публикация в
`store-events-topic.DLT`. Продюсер для DLT — автоконфигурированный
`KafkaTemplate` со строковыми сериализаторами.

### D5. Старт catalog-service без Kafka
`spring.kafka.listener.auto-startup: false`. Компонент-запускатель на
`ApplicationReadyEvent` пытается стартовать контейнеры
`KafkaListenerEndpointRegistry` и при ошибке повторяет попытку по расписанию
(например, раз в 30 с), пока контейнер не запустится. Так ошибка создания
consumer (`No resolvable bootstrap urls`) больше не роняет контекст, а REST
каталога работает сразу. `KafkaAdmin` при недоступной Kafka только логирует
ошибку (как сейчас в seller-service и user-service), старт не прерывает.

Уточнение по реализации: `ConcurrentMessageListenerContainer` помечает себя `running` до создания дочернего consumer. Поэтому после неудачного `start()` запускатель вызывает `container.stop()`, иначе следующая попытка считает контейнер запущенным и повторы молча прекращаются. На стенде без Kafka повторы идут примерно раз в 35 с (30 с паузы плюс неудачный DNS-запрос).

Альтернатива — оставить стандартное поведение. Отклонена: на стенде Kafka
сейчас остановлена, и catalog-service после деплоя не стартовал бы
(наблюдаемый сценарий notification-service OBS-08).

## Risks / Trade-offs

- [Eventual consistency: несколько секунд после удаления магазина товары еще
  не в архиве] → Принято. Задержка определяется циклом outbox (3 с) и
  доставкой.
- [Архивные товары видны в выдаче] → Известное следствие, вынесено за scope
  (proposal).
- [Kafka долго недоступна] → События копятся в `outbox_events` (processed =
  false) и будут доставлены после восстановления. Архивация задерживается, но
  не теряется.
- [Сообщения, пришедшие, пока потребитель не стартовал] → Новая группа
  `catalog-group` читает с `latest` по умолчанию (`integration-events`
  ASM-03). События, опубликованные до первого подключения группы, будут
  пропущены. Для первого запуска задается
  `spring.kafka.consumer.auto-offset-reset: earliest`, чтобы группа прочитала
  топик с начала. После первого коммита offset это значение уже не важно.
- [Изменение типа `KafkaEvent.eventType` на String] → Формат на проводе
  тот же. Это проверяется тестом сериализации.

## Migration Plan

1. Задеплоить seller-service и catalog-service, порядок не важен: события
   копятся в outbox или в топике, пока нет потребителя. Топик создает
   seller-service (`NewTopic`) или брокер (автосоздание).
2. Для сквозной проверки на стенде нужна работающая Kafka (отдельная ручная
   задача).
3. Откат: вернуть предыдущие образы. Записи `STORE` в outbox старый
   `OutboxPublisher` пометит processed без публикации (неизвестный тип для
   `SellerEventType`). Уже заархивированные товары останутся в статусе
   ARCHIVED.
