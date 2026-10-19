# Tasks

## 1. seller-service: событие STORE_DELETED

- [x] 1.1 Добавить в `StoreServiceImplTest` тесты (до изменения кода): удаление своего магазина 3 продавцом 7 сохраняет outbox-запись aggregateType `STORE`, aggregateId "3", eventType `STORE_DELETED`, processed = false, payload `{storeId: 3, sellerId: 7}`; удаление чужого магазина (404) outbox не пишет. Проверка: тест не компилируется или падает на текущем коде
- [x] 1.2 Добавить `event/StoreEventType` и `event/StoreDeletedEvent`; в `StoreServiceImpl.deleteStore` записывать outbox-событие в той же транзакции (design.md, D1). Проверка: `mvn test -Dtest=StoreServiceImplTest` в seller-service проходит

## 2. seller-service: маршрутизация outbox

- [x] 2.1 Добавить `outbox/OutboxPublisherTest` (Mockito, до изменения кода): запись `SELLER_APPLICATION`/`SELLER_APPROVED` публикуется в `seller-events-topic`; запись `STORE`/`STORE_DELETED` — в `store-events-topic` с ключом aggregateId и конвертом `{aggregateType, eventType, payload}`; неизвестный aggregateType и неизвестный eventType помечаются processed без публикации; при ошибке отправки цикл прекращается. Тест сериализации: JSON конверта для `SELLER_APPROVED` после изменения типа поля совпадает с прежним (`"eventType":"SELLER_APPROVED"`). Проверка: тесты маршрутизации падают на текущем коде
- [x] 2.2 Изменить тип `KafkaEvent.eventType` на `String`, реализовать маршрутизацию в `OutboxPublisher` и добавить `NewTopic store-events-topic` (3 партиции, RF 1) в `KafkaConfig` (design.md, D2). Проверка: `mvn test` в seller-service зеленый

## 3. catalog-service: архивация товаров

> Решение при реализации: в `catalog-service/pom.xml` литерал `<skipTests>true</skipTests>` в surefire заменен на свойство `${skipTests}` (по умолчанию `true`), чтобы новые тесты можно было запускать явно. Поведение сборки по умолчанию не изменилось.

- [x] 3.1 Добавить unit-тест сервиса архивации (Mockito, `MongoTemplate`): `archiveProductsOfStore(3)` вызывает `updateMulti` с критерием `store_id = 3` и `status != ARCHIVED` и обновлением `status = ARCHIVED`, без других полей. Проверка: тест падает на текущем коде
- [x] 3.2 Реализовать сервис архивации через `MongoTemplate.updateMulti` (design.md, D3). Проверка: тест 3.1 проходит
- [x] 3.3 Добавить unit-тест потребителя: конверт `STORE`/`STORE_DELETED` с payload `{"storeId":3,"sellerId":7}` вызывает архивацию магазина 3; неизвестный eventType не вызывает архивацию и не бросает исключение; некорректный JSON бросает исключение (для обработчика ошибок). Проверка: тест падает на текущем коде
- [x] 3.4 Реализовать потребитель `store-events-topic` (`catalog-group`), конфигурацию `spring.kafka` (bootstrap из `SPRING_KAFKA_BOOTSTRAP_SERVERS`, строковые (де)сериализаторы, `auto-offset-reset: earliest`) и `DefaultErrorHandler` с `DeadLetterPublishingRecoverer` и `FixedBackOff(1000, 3)` (design.md, D3, D4). Проверка: тест 3.3 проходит

## 4. catalog-service: старт без Kafka

- [x] 4.1 Включить `spring.kafka.listener.auto-startup: false` и добавить запускатель контейнеров на `ApplicationReadyEvent` с повтором при ошибке (design.md, D5); unit-тест: при исключении старта контейнера повтор планируется, при успехе — нет. Проверка: тест проходит; `mvn test -DskipTests=false -Dtest=<новые тесты catalog-service>` зеленый
- [x] 4.2 Пересобрать и перезапустить только catalog-service на текущем стенде, где Kafka остановлена. Проверка: сервис стартовал, `GET /products` через api-gateway отвечает 200, в логе есть повторные попытки запуска потребителя, а не `Application run failed`

## 5. Проверка

- [x] 5.1 Прогоны: `mvn test` в seller-service зеленый; в catalog-service — `mvn test -DskipTests=false -Dtest=<новые тесты>` зеленый (тесты catalog-service по умолчанию пропускаются, `CatalogServiceApplicationTests` с полным контекстом в выборку не входит). Проверка: вывод обоих прогонов
- [x] 5.2 Убедиться, что изменения ограничены seller-service (store/outbox/event/config и тесты) и catalog-service (Kafka, архивация и тесты), а notification-service, user-service, frontend и формат `seller-events-topic` не тронуты. Проверка: `git diff --stat`, `git status`
- [ ] 5.3 (РУЧНАЯ, требует работающей Kafka и токена продавца) Сквозная проверка на стенде: запустить Kafka; перезапустить seller-service и catalog-service; создать магазин и товар от имени продавца; удалить магазин; через несколько секунд товар имеет статус ARCHIVED (`GET /products/{id}`); в kafka-ui в `store-events-topic` есть сообщение с ключом id магазина; в `store-events-topic.DLT` пусто. Проверка: результаты записаны в отчет к задаче
  > Не выполнено на момент archive (2026-10-06): Kafka на стенде остановлена, сквозная проверка отложена по решению пользователя. Проверено unit-тестами и стартом catalog-service без Kafka (4.2).
- [x] 5.4 Проверка change: `openspec validate archive-products-of-deleted-store --strict` проходит. После archive отметить устраненными `seller-stores` OBS-07, `catalog-products` OBS-15 (частично: товарами по-прежнему нельзя управлять, но они в архиве) и `integration-events` OBS-13 для catalog-service, сохранив идентификаторы. Проверка: `openspec validate --specs --strict` проходит
