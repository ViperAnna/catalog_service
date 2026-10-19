# integration-events Specification

## Purpose
Сводная baseline (as-is) модель асинхронного взаимодействия через Kafka: брокер
и топики, контракты сообщений `user-created-events-topic` и
`seller-events-topic`, ключи сообщений, transactional outbox и публикация,
гарантии доставки, поведение потребителей и обработка ошибок с DLT.
Спецификация описывает фактическое текущее состояние и сводит его из сервисных
specs, не вводя новых требований.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Kafka-код в рабочей копии
  не изменен. В рабочей копии change `notify-moderator-on-update-deletion-applications`
  есть незакоммиченное изменение seller-service: событие
  `SELLER_APPLICATION_CREATED` записывается и для заявок UPDATE и DELETION. В
  baseline `seller-onboarding` это еще не внесено, поэтому здесь описан состав
  событий по baseline, где оно записывается только для REGISTRATION.
- Метод: статический анализ `OutboxPublisher` (user-service, seller-service),
  `KafkaConfig` (user, seller, notification), классов событий, консьюмеров
  notification-service, `application.yaml` и `.env.kafka`. На dev-стенде ранее
  наблюдались список топиков, состав consumer group и поведение
  notification-service при недоступной Kafka.
- Нормативные детали бизнес-событий описаны в `seller-onboarding`
  (`Outbox event recording`, `Outbox publishing to seller-events-topic`,
  `Seller events message format`, `Seller event email notifications`,
  `Seller events consumer error handling`), `user-profile`
  (`USER_CREATED outbox event`, `USER_CREATED publishing`) и `notifications`.
  Здесь они сведены на уровне общей модели. Безопасность каналов описана в
  `access-control`.
- Секция Requirements нормативна. Секции Event Catalog, Observations,
  Assumptions и Open Questions нормативными не являются. Изменять
  зафиксированное поведение нужно отдельным change.

## Requirements

### Requirement: Kafka broker
Система SHALL использовать один брокер Kafka (`apache/kafka`, режим KRaft,
роли broker и controller) по адресу `kafka:9092` с протоколом PLAINTEXT, с
replication factor 1 и числом партиций по умолчанию 3 (`KAFKA_NUM_PARTITIONS`).
Producer и consumer SHALL подключаться через `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

#### Scenario: Подключение сервисов
- **WHEN** user-service, seller-service или notification-service стартует в compose
- **THEN** он подключается к `kafka:9092` без аутентификации

**Observation**: OBS-12

### Requirement: Topic declarations
`user-created-events-topic` SHALL объявляться бинами `NewTopic` (3 партиции,
replication factor 1) одновременно в user-service и notification-service,
`seller-events-topic` и `store-events-topic` (по 3 партиции, replication
factor 1) — в seller-service, `product-events-topic` (3 партиции, replication
factor 1) — в catalog-service. Топики `*.DLT` SHALL NOT объявляться явно.

#### Scenario: Список топиков на стенде
- **WHEN** запрашивается список топиков брокера dev-стенда без ошибок обработки
- **THEN** он содержит `user-created-events-topic`, `seller-events-topic`, `store-events-topic` и `product-events-topic`, а топиков `*.DLT` нет

**Observation**: OBS-09, OBS-11
**Assumption**: ASM-02

### Requirement: user-created-events-topic message contract
Сообщение в `user-created-events-topic` SHALL иметь строковый ключ, равный
внутреннему UUID профиля (userId), и строковое значение — JSON без обертки
`{userId, keycloakUserId, email, username}`. Тип события в сообщении SHALL NOT
передаваться: топик содержит только события создания профиля.

#### Scenario: Сообщение о создании профиля
- **WHEN** user-service публикует событие для профиля с id `u-1`
- **THEN** ключ сообщения `u-1`, значение — JSON с полями userId, keycloakUserId, email, username

**Observation**: OBS-01, OBS-04, OBS-10

### Requirement: seller-events-topic message contract
Сообщение в `seller-events-topic` SHALL иметь строковый ключ (id заявки) и
строковое значение — обертку `{aggregateType, eventType, payload}`:
aggregateType = `SELLER_APPLICATION`, eventType — SELLER_APPLICATION_CREATED,
SELLER_APPROVED или SELLER_REJECTED, payload — JSON события строкой. Payload
SELLER_APPROVED SHALL содержать applicationType (REGISTRATION, UPDATE,
DELETION или UNBLOCK); consumer SHALL допускать его отсутствие.

#### Scenario: Сообщение об одобрении
- **WHEN** seller-service публикует SELLER_APPROVED для заявки 7 типа DELETION
- **THEN** ключ `7`, значение — обертка, у которой поле payload — строка с JSON `{applicationId, keycloakUserId, sellerName, email, applicationType}` и applicationType = "DELETION"

#### Scenario: Сообщение об одобрении без типа
- **WHEN** notification-service получает SELLER_APPROVED, payload которого не содержит applicationType
- **THEN** сообщение обрабатывается без ошибки и не уходит в DLT

#### Scenario: Сообщение об одобрении с неизвестным типом
- **WHEN** notification-service получает SELLER_APPROVED с applicationType "FOO"
- **THEN** сообщение сразу уходит в `seller-events-topic.DLT`, без повторов

#### Scenario: Одобрение разблокировки
- **WHEN** notification-service получает SELLER_APPROVED с applicationType "UNBLOCK"
- **THEN** сообщение обрабатывается без DLT

**Observation**: OBS-01, OBS-04, OBS-05, OBS-10

### Requirement: Transactional outbox recording
user-service и seller-service SHALL записывать событие в собственную таблицу
outbox (`outbox_event` и `outbox_events`) в той же транзакции БД, что и
бизнес-изменение, с полями aggregateType, aggregateId, eventType, payload,
createdAt и processed = false. Обработанные записи SHALL NOT удаляться. Payload
записей `SELLER_APPLICATION` seller-service SHALL обезличиваться только после
подтверждения публикации Kafka, в одной транзакции с processed = true.

#### Scenario: Откат бизнес-операции
- **WHEN** транзакция бизнес-операции откатывается
- **THEN** запись outbox не сохраняется и сообщение не публикуется

#### Scenario: Обезличивание после публикации
- **WHEN** seller-service получил подтверждение Kafka для SELLER_APPROVED
- **THEN** запись помечена processed = true, ее payload содержит только applicationId и redacted

#### Scenario: Публикация не подтверждена
- **WHEN** отправка в Kafka завершилась ошибкой или таймаутом
- **THEN** запись остается processed = false с исходным payload и отправляется повторно

#### Scenario: События магазинов
- **WHEN** опубликовано событие с aggregateType STORE
- **THEN** его payload не меняется

**Observation**: OBS-03, OBS-06

### Requirement: Outbox publishing
Каждый producer SHALL раз в 3 секунды (user-service — fixed delay,
seller-service — fixed rate) в одной транзакции выбирать до 100 записей с
processed = false по возрастанию createdAt и по одной синхронно отправлять в
топик с ожиданием подтверждения до 10 секунд. Запись SHALL помечаться processed
после подтверждения, при ошибке отправки цикл SHALL прекращаться.

#### Scenario: Ошибка отправки
- **WHEN** отправка третьей записи цикла завершилась ошибкой
- **THEN** первые две помечены processed, третья и следующие остаются processed = false до следующего цикла

#### Scenario: Неизвестный тип в seller-service
- **WHEN** eventType записи outbox seller-service не является значением SellerEventType
- **THEN** запись помечается processed без публикации

**Observation**: OBS-03, OBS-07, OBS-08
**Assumption**: ASM-01

### Requirement: Delivery and ordering guarantees
Доставка из outbox в топик SHALL быть не строже at-least-once: событие может
быть опубликовано повторно, если подтверждение Kafka получено, а фиксация
транзакции outbox не прошла. Порядок SHALL сохраняться только для сообщений с
одним ключом. Дедупликация на стороне producer или consumer SHALL NOT
выполняться.

#### Scenario: События одной заявки
- **WHEN** для заявки 7 публикуются SELLER_APPLICATION_CREATED, затем SELLER_REJECTED
- **THEN** оба попадают в одну партицию и потребляются в этом порядке

#### Scenario: Повторная доставка
- **WHEN** одно и то же событие доставлено дважды
- **THEN** notification-service обрабатывает его дважды

**Observation**: OBS-04, OBS-15

### Requirement: Event consumers
Потребителями событий SHALL быть notification-service — по одному
`@KafkaListener` на `user-created-events-topic` и `seller-events-topic` в
consumer group `notification-group`, catalog-service — `@KafkaListener` на
`store-events-topic` в consumer group `catalog-group`, и user-service —
`@KafkaListener` на `product-events-topic` в consumer group
`user-service-group`. Используются строковые десериализаторы и настройки
concurrency, подтверждения и `auto-offset-reset` по умолчанию.

#### Scenario: Состав группы на стенде
- **WHEN** notification-service работает и подключен к Kafka
- **THEN** группа `notification-group` содержит двух участников, каждому назначены 3 партиции своего топика

#### Scenario: Группа catalog-service
- **WHEN** catalog-service работает и подключен к Kafka
- **THEN** группа `catalog-group` содержит участника, которому назначены 3 партиции `store-events-topic`

#### Scenario: Группа user-service
- **WHEN** user-service работает и подключен к Kafka
- **THEN** группа `user-service-group` содержит участника, которому назначены 3 партиции `product-events-topic`

**Assumption**: ASM-03, ASM-04

### Requirement: Consumer error handling and DLT
Исключение в listener SHALL повторяться 3 раза с паузой 1 секунда
(`FixedBackOff(1000, 3)`), после чего сообщение SHALL публиковаться в
`<topic>.DLT` в партицию с тем же номером. Исключение для неизвестного типа
заявки (`UnknownApplicationTypeException`) SHALL публиковать сообщение в DLT
без повторов. Ошибки отправки email и Telegram SHALL перехватываться и SHALL
NOT вызывать повторов. Обертка seller-события без eventType или payload SHALL
пропускаться без DLT. У DLT потребителей SHALL NOT быть.

#### Scenario: Невалидный JSON
- **WHEN** значение сообщения в `seller-events-topic` не является JSON
- **THEN** после 4 попыток (1 + 3 повтора) сообщение уходит в `seller-events-topic.DLT`

#### Scenario: Неизвестный eventType
- **WHEN** eventType в обертке не входит в enum notification-service
- **THEN** десериализация падает, и сообщение уходит в DLT

#### Scenario: Неизвестный тип заявки
- **WHEN** SELLER_APPROVED содержит applicationType, неизвестный notification-service
- **THEN** после первой попытки сообщение уходит в `seller-events-topic.DLT`, повторов нет

#### Scenario: Обертка без payload
- **WHEN** в обертке нет payload
- **THEN** сообщение пропускается с предупреждением в логе, в DLT не попадает

**Observation**: OBS-09, OBS-14, OBS-15
**Assumption**: ASM-02, ASM-05

### Requirement: Services without event processing
catalog-service SHALL NOT публиковать сообщения Kafka, кроме публикации в
`store-events-topic.DLT` при ошибках обработки и PRODUCT_DELETED в
`product-events-topic`. seller-service SHALL NOT потреблять сообщения;
user-service SHALL потреблять только `product-events-topic`.
Consumer-настройки seller-service SHALL NOT использоваться.

#### Scenario: catalog-service
- **WHEN** в catalog-service создается или изменяется товар
- **THEN** сообщения в Kafka не публикуются

#### Scenario: Удаление товара
- **WHEN** в catalog-service удаляется товар
- **THEN** публикуется только PRODUCT_DELETED в `product-events-topic`

**Observation**: OBS-13

### Requirement: store-events-topic message contract
Сообщение в `store-events-topic` SHALL иметь строковый ключ, равный id
магазина, и строковое значение — конверт `{aggregateType, eventType, payload}`
того же формата, что в `seller-events-topic`, где aggregateType = `STORE`,
eventType — STORE_PURGED, STORE_BLOCKED, STORE_UNBLOCKED, SELLER_BLOCKED,
SELLER_UNBLOCKED или устаревший STORE_DELETED, а payload — JSON
`{storeId, sellerId}` строкой без персональных данных. Producer —
seller-service, consumer — catalog-service.

#### Scenario: Сообщение об удалении магазина
- **WHEN** seller-service удаляет магазин 3 продавца 7
- **THEN** ключ `3`, значение — конверт с aggregateType "STORE", eventType "STORE_PURGED" и payload-строкой с JSON `{"storeId":3,"sellerId":7}`

#### Scenario: Блокировка продавца
- **WHEN** seller-service блокирует продавца 7 с магазином 3
- **THEN** публикуется конверт с ключом `3`, eventType "SELLER_BLOCKED" и payload `{"storeId":3,"sellerId":7}`

### Requirement: product-events-topic message contract
Сообщение в `product-events-topic` SHALL иметь строковый ключ, равный id
товара, и значение — конверт `{aggregateType, eventType, payload}` формата
`seller-events-topic`, где aggregateType = `PRODUCT`, eventType —
PRODUCT_DELETED, а payload — JSON-строка `{eventId, occurredAt, productId,
storeId}` без персональных данных. Producer — catalog-service, consumer —
user-service.

#### Scenario: Сообщение об удалении товара
- **WHEN** catalog-service публикует PRODUCT_DELETED для товара `p-1` магазина 3
- **THEN** ключ `p-1`, конверт с aggregateType "PRODUCT", eventType "PRODUCT_DELETED" и payload с productId `p-1` и storeId 3

### Requirement: Product deletion event delivery
catalog-service SHALL записывать событие PRODUCT_DELETED в outbox-коллекцию
MongoDB до удаления товара и SHALL публиковать его только после того, как
товара с этим id в каталоге нет. Если товар остается в каталоге 1 час и
дольше после записи события, событие SHALL отбрасываться с ERROR в логе;
повторное удаление того же товара SHALL снова ставить событие в очередь.
Доставка SHALL быть at-least-once; повторная обработка STORE_PURGED SHALL NOT
создавать второе событие для того же товара.

#### Scenario: Удаление не выполнено
- **WHEN** событие записано в outbox, а удаление документа товара завершилось ошибкой
- **THEN** событие не публикуется, через 1 час отбрасывается с ERROR в логе

#### Scenario: Повторное удаление после неудачи
- **WHEN** событие товара было отброшено, а затем товар удален успешно
- **THEN** PRODUCT_DELETED публикуется

#### Scenario: Товар существует
- **WHEN** запись события есть, а документ товара в каталоге существует
- **THEN** событие не публикуется

#### Scenario: Kafka недоступна
- **WHEN** товар удален, а Kafka недоступна
- **THEN** событие публикуется после восстановления Kafka

### Requirement: Product deletion event consumption
user-service SHALL потреблять `product-events-topic` в consumer group
`user-service-group` и на PRODUCT_DELETED SHALL удалять productId из всех
вишлистов. Обработка SHALL быть идемпотентной. Сообщение с неизвестным
eventType SHALL пропускаться с предупреждением; ошибка разбора или обработки
SHALL повторяться 3 раза с паузой 1 секунда, затем сообщение SHALL
публиковаться в `product-events-topic.DLT`.

#### Scenario: Удаление из вишлистов
- **WHEN** товар `p-1` есть в вишлистах двух пользователей, и приходит PRODUCT_DELETED для `p-1`
- **THEN** `p-1` удален из обоих вишлистов, остальные товары не меняются

#### Scenario: Повторная доставка
- **WHEN** PRODUCT_DELETED для `p-1` доставлен второй раз
- **THEN** обработка завершается без ошибки, вишлисты не меняются

## Event Catalog (informative)

| Топик | Ключ | eventType | Producer | Когда | Payload | Consumer -> эффект |
|---|---|---|---|---|---|---|
| `user-created-events-topic` | userId (UUID профиля) | — (подразумевается USER_CREATED) | user-service | первое `GET /users/me` | `{userId, keycloakUserId, email, username}` | notification: ссылка Telegram, сообщение в Telegram, письмо "Welcome!" |
| `seller-events-topic` | applicationId | SELLER_APPLICATION_CREATED | seller-service | подача заявки любого типа | `{applicationId, keycloakUserId, sellerName, email}` | notification: письмо модератору |
| `seller-events-topic` | applicationId | SELLER_APPROVED | seller-service | approve заявки любого типа | `{applicationId, keycloakUserId, sellerName, email, applicationType}` | notification: письмо продавцу по applicationType; неизвестный непустой тип — без письма, в DLT без повторов |
| `seller-events-topic` | applicationId | SELLER_REJECTED | seller-service | reject | `{..., moderatorComment}` | notification: письмо продавцу |
| `store-events-topic` | storeId | STORE_PURGED | seller-service | удаление магазина владельцем, одобрение удаления продавца | `{storeId, sellerId}` | catalog: удаление неиспользуемых изображений и документов товаров магазина |
| `store-events-topic` | storeId | STORE_BLOCKED, STORE_UNBLOCKED | seller-service | блокировка и разблокировка магазина модератором | `{storeId, sellerId}` | catalog: признак storeBlocked в товарах магазина |
| `store-events-topic` | storeId | SELLER_BLOCKED, SELLER_UNBLOCKED | seller-service | блокировка и разблокировка продавца (по событию на магазин) | `{storeId, sellerId}` | catalog: признак sellerBlocked в товарах магазина |
| `store-events-topic` | storeId | STORE_DELETED (устарело, не публикуется) | seller-service до change `seller-store-lifecycle` | удаление магазина | `{storeId, sellerId}` | catalog: товары магазина -> ARCHIVED |
| `product-events-topic` | productId | PRODUCT_DELETED | catalog-service (outbox в MongoDB) | физическое удаление товара владельцем или при STORE_PURGED; публикуется только когда товара уже нет | `{eventId, occurredAt, productId, storeId}` | user-service: id товара удаляется из всех вишлистов |
| `<topic>.DLT` | исходный | исходный | notification (recoverer) | после 3 повторов; для неизвестного applicationType — сразу | исходное значение | нет |

## Operational Procedures (informative)

Ручные операции из change `seller-store-lifecycle`; инструментов для них нет.

**Повторная отправка события магазина из `store-events-topic.DLT`.** После DLT
следующие события того же магазина продолжают обрабатываться, поэтому
переотправка старого сообщения может откатить более новое состояние.
1. Найти исходную запись `outbox_events` seller-service (aggregateId =
   storeId, eventType, processed = true) и записи того же aggregateId с более
   поздним `created_at` (id записи — UUID и порядка не задает).
2. Проверить текущее состояние в seller-service: статус магазина и продавца,
   существует ли магазин.
3. `STORE_PURGED` переотправлять всегда. Событие блокировки или
   разблокировки — только если после него нет записей того же признака и оно
   совпадает с текущим статусом; иначе переотправить последнее по
   `created_at` событие этого признака, если оно совпадает с текущим статусом.
   Для удаленного магазина — только `STORE_PURGED`.
4. Переотправка: `processed = false` у выбранной записи outbox (payload
   событий STORE не обезличивается) или отправка сообщения из DLT в
   `store-events-topic` с тем же ключом.
5. Проверить, что признаки товаров магазина в MongoDB совпадают со
   статусами в seller-service.

**Записи Seller в статусе DELETED, созданные до change `seller-store-lifecycle`.** Не
очищаются автоматически. 1) Проверка: такие продавцы, их магазины, товары в
MongoDB (`store_id`), заявки. 2) Резервная копия: `pg_dump` таблиц
`sellers`, `stores`, `seller_applications`, `outbox_events`, `mongodump`
коллекции `products`, список объектов MinIO. 3) Очистка в транзакции на
продавца: `STORE_PURGED` в outbox на каждый магазин, обезличивание его
записей outbox, удаление магазинов, заявок и Seller; повторный запуск ничего
не находит. 4) Проверка: записей нет, товаров этих `store_id` нет, записи
outbox обработаны, DLT пуст.

## Observations (non-normative)
Подтверждено кодом, конфигурацией или наблюдением на стенде. Это потенциальные
проблемы или технический долг. Изменять их нужно отдельным change после решения.

- **OBS-01. Обертки событий в двух топиках разные.** В
  `user-created-events-topic` значение — голый JSON события без типа. В
  `seller-events-topic` — обертка `{aggregateType, eventType, payload}`. У
  потребителя два разных способа разбора.
- **OBS-02. Классы событий скопированы между сервисами (архитектурное
  наблюдение).**
  - `KafkaEvent`, `SellerEventType`, `SellerApplicationCreatedEvent`,
    `SellerApprovedEvent` и `SellerRejectedEvent` продублированы в
    seller-service и notification-service.
  - `UserCreatedEvent` продублирован в user-service и notification-service.
  - `EventMapper` продублирован в трех сервисах.
  - Общего модуля контрактов, схем и schema registry нет, поэтому
    расхождение копий (например, новое значение enum) обнаружится только в
    runtime и приведет к попаданию сообщения в DLT.
- **OBS-03. Реализации outbox разошлись.** Различаются имена таблиц
  (`outbox_event` и `outbox_events`), расписание (fixedDelay и fixedRate),
  обработка неизвестного типа (есть только в seller-service) и формат значения
  (без обертки и с оберткой). Общей библиотеки нет.
- **OBS-04. В сообщениях нет метаданных.** Нет eventId, времени события и
  версии схемы. Потребитель не может отличить повтор от нового события и не
  может выбрать версию разбора.
- **OBS-05. Двойное кодирование payload.** В `seller-events-topic` payload —
  JSON-строка внутри JSON. Его приходится разбирать дважды.
- **OBS-06. Outbox не очищается.** Записи outbox не удаляются, таблицы растут
  без ограничения.
- **OBS-07. Длинные транзакции и дубли при масштабировании.** Публикатор
  держит транзакцию БД, пока синхронно ждет Kafka (до 10 с на каждую из 100
  записей). Блокировки строк нет, поэтому несколько экземпляров сервиса
  опубликуют одни и те же записи (`seller-onboarding` ASM-02).
- **OBS-08. Head-of-line blocking.** Одна неотправляемая запись прекращает цикл
  и задерживает все последующие события этого producer.
- **OBS-09. DLT без обслуживания.** DLT-топики не объявлены заранее,
  потребителей, мониторинга и процедуры повторной обработки у них нет.
- **OBS-10. Выбор ключей.** Ключ `user-created-events-topic` — внутренний
  userId, который меняется при повторном создании профиля (`user-profile`
  OBS-05). Ключ `seller-events-topic` — id заявки, поэтому порядок событий
  одного продавца между разными заявками не гарантирован.
- **OBS-11. Двойное владение топиком.** `user-created-events-topic` объявляют
  два сервиса (user-service и notification-service, оба бина называются
  `testTopic`).
- **OBS-12. Kafka — единая точка отказа без защиты.** Один брокер с replication
  factor 1, без аутентификации (`access-control` OBS-08). В dev у контейнера
  kafka нет `restart`: после перезапуска хоста он остается остановленным, а
  notification-service после неудачного старта продолжает висеть как живой
  контейнер (`notifications` OBS-08, наблюдалось на стенде).
- **OBS-13. Неиспользуемая Kafka-конфигурация.** _Для catalog-service устранено
  change `archive-products-of-deleted-store`: он потребляет `store-events-topic`;
  change `product-deletion-sync` — catalog-service публикует
  `product-events-topic`, user-service его потребляет._
  Остается: в seller-service задана consumer-конфигурация с неразрешенным
  `${KAFKA_CONSUMER_GROUPID}` при отсутствии listener. В user-service
  `MessageProducer` пустой.
- **OBS-14. Ошибки формата обрабатываются по-разному.** Обертка без eventType
  или payload молча пропускается, а невалидный JSON или неизвестный eventType
  уходит в DLT.
- **OBS-15. Уведомления фактически at-most-once.** Доставка в топик
  at-least-once, но ошибка SMTP или Telegram при обработке не повторяется.
  Уведомление теряется, а offset фиксируется. Повторная доставка того же
  события, наоборот, дает дубликат письма.

## Assumptions (unverified)
- **ASM-01. Настройки producer.** acks, idempotence и retries явно не заданы.
  Предполагается, что действуют значения по умолчанию kafka-clients из Spring
  Boot 3.3.4 (acks=all, enable.idempotence=true). Не проверялось.
- **ASM-02. Автосоздание DLT.** Топики `*.DLT` создаются автоматически при
  первой публикации (`auto.create.topics.enable` не переопределен) с 3
  партициями (`KAFKA_NUM_PARTITIONS`), поэтому публикация в ту же партицию
  проходит. Если автосоздание в prod выключено, recoverer не сможет опубликовать
  сообщение.
- **ASM-03. auto-offset-reset.** `auto-offset-reset` не задан, поэтому, вероятно,
  действует `latest`. Новая группа или потерянные offsets приведут к пропуску
  уже опубликованных событий.
- **ASM-04. Фиксация offset.** Offset фиксируется после успешной обработки
  (AckMode по умолчанию BATCH, auto-commit выключен). Сбой процесса посреди
  пачки, вероятно, приведет к повторной обработке.
- **ASM-05. Сбой recoverer.** Если публикация в DLT не удалась, сообщение,
  вероятно, обрабатывается повторно по правилам DefaultErrorHandler.
- **ASM-06. Kafka в prod.** Конфигурация Kafka в prod совпадает с dev:
  env-файлы передаются через Jenkins Credentials.
- **ASM-07. Retention.** Срок хранения сообщений не задан. Действует значение
  брокера по умолчанию (7 дней).

## Open Questions
- **OQ-01. OBS-01–OBS-15.** Какие из них дефекты, а какие — осознанные решения?
- **OQ-02. Единый формат событий.** Нужен ли единый конверт (eventId, type,
  occurredAt, version, payload-объект) для всех топиков?
- **OQ-03. Общий модуль контрактов.** Выносить классы событий в общий модуль
  или переходить на schema registry?
- **OQ-04. Идемпотентность и DLT.** Нужна ли идемпотентность потребителей и
  процесс разбора DLT?
- **OQ-05. Отказоустойчивость Kafka.** Нужна ли отказоустойчивая Kafka
  (несколько брокеров, RF > 1) и политика перезапуска в dev?
