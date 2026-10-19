## ADDED Requirements

### Requirement: order-events-topic message contract
Сообщение в `order-events-topic` SHALL иметь строковый ключ, равный UUID
заказа, и значение — конверт `{aggregateType, eventType, payload}` формата
`seller-events-topic`, где aggregateType = `ORDER`, eventType — ORDER_CREATED
или ORDER_CANCELLED, а payload — JSON-строка `{eventId, occurredAt, orderId,
keycloakUserId, status, totalAmount, itemsCount, email}`. Producer —
order-service, consumer — notification-service.

#### Scenario: Сообщение о создании заказа
- **WHEN** order-service публикует ORDER_CREATED для заказа `o-1`
- **THEN** ключ `o-1`, конверт с aggregateType "ORDER", eventType "ORDER_CREATED" и payload со status "CREATED", orderId `o-1` и eventId, равным id записи outbox

#### Scenario: Порядок событий заказа
- **WHEN** для заказа публикуются ORDER_CREATED, затем ORDER_CANCELLED
- **THEN** оба попадают в одну партицию и потребляются в этом порядке

### Requirement: Order event identity
Каждое событие заказа SHALL иметь `eventId` (UUID), который присваивается при
записи в outbox в одной транзакции с изменением заказа и SHALL оставаться
неизменным при любой повторной публикации той же записи. Одному изменению
заказа SHALL соответствовать ровно один `eventId`. `occurredAt` SHALL
равняться времени записи в outbox.

#### Scenario: Повторная публикация
- **WHEN** Kafka подтвердила отправку ORDER_CREATED, а фиксация processed = true не прошла, и запись отправлена снова
- **THEN** оба сообщения имеют одинаковые eventId и payload

#### Scenario: Разные изменения
- **WHEN** заказ создан, затем отменен
- **THEN** ORDER_CREATED и ORDER_CANCELLED имеют разные eventId

### Requirement: Order event personal data
Payload событий `ORDER` SHALL содержать из персональных данных только
`keycloakUserId` (`sub`) и `email`. `email` SHALL браться из claim `email`
access token запроса, вызвавшего событие, только при claim `email_verified` =
true, иначе SHALL быть null. Имена, телефоны, адреса, состав заказа и
названия товаров SHALL NOT передаваться.

#### Scenario: Подтвержденный email
- **WHEN** покупатель с `email` = `buyer@test.com` и `email_verified` = true оформляет заказ
- **THEN** payload ORDER_CREATED содержит `email` = `buyer@test.com`

#### Scenario: Неподтвержденный или отсутствующий email
- **WHEN** у покупателя нет claim `email` или `email_verified` = false
- **THEN** событие публикуется с `email` = null

### Requirement: Order event personal data retention
Payload записи outbox `ORDER` SHALL заменяться на `{orderId, eventId,
redacted: true}` после подтвержденной публикации в той же транзакции, что и
processed = true. Email SHALL NOT храниться в таблицах заказов order-service и
в БД notification-service. В Kafka сообщения SHALL храниться до истечения
retention топика; удаление отдельных сообщений SHALL NOT поддерживаться.

#### Scenario: Обезличивание после публикации
- **WHEN** order-service получил подтверждение Kafka для ORDER_CREATED
- **THEN** запись outbox помечена processed = true, ее payload содержит только orderId, eventId и redacted

#### Scenario: Публикация не подтверждена
- **WHEN** отправка ORDER_CREATED завершилась ошибкой
- **THEN** payload записи outbox не меняется до успешной публикации

### Requirement: Order event logging
order-service и notification-service SHALL NOT записывать в лог email,
payload событий заказа, JWT и заголовок `Authorization`. Записи лога об
обработке событий заказа SHALL содержать только `orderId`, `eventId`,
`eventType`, topic, partition, offset и причину.

#### Scenario: Ошибка отправки письма
- **WHEN** отправка письма по ORDER_CREATED завершилась ошибкой
- **THEN** в логе есть orderId, eventId и eventType и нет адреса получателя и payload

### Requirement: Single order outbox publisher
order-service SHALL работать в одном экземпляре, пока публикатор outbox не
блокирует записи: несколько экземпляров публикуют одни и те же записи и могут
нарушить порядок событий одного заказа. Конфигурация развертывания SHALL NOT
допускать второй экземпляр.

#### Scenario: Конфигурация compose
- **WHEN** проверяется сервис order-service в `docker-compose.yml` и `docker-compose.prod.yml`
- **THEN** у него задан `container_name`, и масштабирование до двух экземпляров невозможно

### Requirement: Order events DLT policy
notification-service SHALL отправлять сообщение `order-events-topic` в
`order-events-topic.DLT` без повторов, если eventType неизвестен или
сообщение некорректно (не JSON; нет eventType, payload, eventId или orderId;
aggregateType ≠ ORDER), и после 3 повторов с паузой 1 с — при временной
ошибке, в том числе SMTP. Каждая отправка в DLT SHALL сопровождаться ERROR в
логе.

#### Scenario: Будущий тип события
- **WHEN** приходит конверт с eventType "ORDER_PAID", которого нет ни в одном списке notification-service
- **THEN** после первой попытки сообщение в `order-events-topic.DLT`, в логе ERROR с eventType и offset

#### Scenario: SMTP недоступен
- **WHEN** отправка письма ORDER_CREATED падает при каждой попытке
- **THEN** после 4 попыток сообщение в `order-events-topic.DLT`, eventId не отмечен обработанным

#### Scenario: Нет eventId
- **WHEN** payload ORDER_CREATED не содержит eventId
- **THEN** сообщение сразу уходит в DLT

## MODIFIED Requirements

### Requirement: Kafka broker
Система SHALL использовать один брокер Kafka (`apache/kafka`, режим KRaft,
роли broker и controller) по адресу `kafka:9092` с протоколом PLAINTEXT, с
replication factor 1 и числом партиций по умолчанию 3 (`KAFKA_NUM_PARTITIONS`).
Producer и consumer SHALL подключаться через `SPRING_KAFKA_BOOTSTRAP_SERVERS`.

#### Scenario: Подключение сервисов
- **WHEN** user-service, seller-service, order-service или notification-service стартует в compose
- **THEN** он подключается к `kafka:9092` без аутентификации

**Observation**: OBS-12

### Requirement: Topic declarations
`user-created-events-topic` SHALL объявляться бинами `NewTopic` (3 партиции,
replication factor 1) одновременно в user-service и notification-service,
`seller-events-topic` и `store-events-topic` (по 3 партиции, replication
factor 1) — в seller-service, `product-events-topic` (3 партиции, replication
factor 1) — в catalog-service, `order-events-topic` (3 партиции, replication
factor 1) — в order-service. Из топиков `*.DLT` SHALL явно объявляться только
`order-events-topic.DLT` (3 партиции, replication factor 1) в
notification-service.

#### Scenario: Список топиков на стенде
- **WHEN** запрашивается список топиков брокера dev-стенда без ошибок обработки
- **THEN** он содержит `user-created-events-topic`, `seller-events-topic`, `store-events-topic`, `product-events-topic`, `order-events-topic` и `order-events-topic.DLT`, а других топиков `*.DLT` нет

**Observation**: OBS-09, OBS-11
**Assumption**: ASM-02

### Requirement: Transactional outbox recording
user-service, seller-service и order-service SHALL записывать событие в
собственную таблицу outbox (`outbox_event`, `outbox_events` и
`outbox_events`) в той же транзакции БД, что и бизнес-изменение, с полями
aggregateType, aggregateId, eventType, payload, createdAt и processed = false.
Обработанные записи SHALL NOT удаляться. Payload записей `SELLER_APPLICATION`
seller-service и `ORDER` order-service SHALL обезличиваться только после
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

#### Scenario: Заказ и событие в одной транзакции
- **WHEN** order-service создает заказ
- **THEN** заказ и запись outbox ORDER_CREATED фиксируются одной транзакцией

**Observation**: OBS-03, OBS-06

### Requirement: Outbox publishing
Каждый producer SHALL раз в 3 секунды (user-service — fixed delay,
seller-service и order-service — fixed rate) в одной транзакции выбирать до
100 записей с processed = false по возрастанию createdAt и по одной синхронно
отправлять в топик с ожиданием подтверждения до 10 секунд. Запись SHALL
помечаться processed после подтверждения, при ошибке отправки цикл SHALL
прекращаться.

#### Scenario: Ошибка отправки
- **WHEN** отправка третьей записи цикла завершилась ошибкой
- **THEN** первые две помечены processed, третья и следующие остаются processed = false до следующего цикла

#### Scenario: Неизвестный тип в seller-service
- **WHEN** eventType записи outbox seller-service не является значением SellerEventType
- **THEN** запись помечается processed без публикации

#### Scenario: Неизвестный тип в order-service
- **WHEN** aggregateType или eventType записи outbox order-service неизвестен
- **THEN** запись помечается processed без публикации, в логе ERROR с id записи и eventType

**Observation**: OBS-03, OBS-07, OBS-08
**Assumption**: ASM-01

### Requirement: Delivery and ordering guarantees
Доставка из outbox в топик SHALL быть не строже at-least-once: событие может
быть опубликовано повторно, если подтверждение Kafka получено, а фиксация
транзакции outbox не прошла. Порядок SHALL сохраняться только для сообщений с
одним ключом. Дедупликация SHALL выполняться только notification-service для
`order-events-topic` по `eventId`; для остальных топиков дедупликация на
стороне producer или consumer SHALL NOT выполняться.

#### Scenario: События одной заявки
- **WHEN** для заявки 7 публикуются SELLER_APPLICATION_CREATED, затем SELLER_REJECTED
- **THEN** оба попадают в одну партицию и потребляются в этом порядке

#### Scenario: Повторная доставка
- **WHEN** одно и то же событие `seller-events-topic` доставлено дважды
- **THEN** notification-service обрабатывает его дважды

#### Scenario: Повторная доставка события заказа
- **WHEN** ORDER_CREATED с одним eventId доставлено дважды
- **THEN** notification-service отправляет письмо один раз

**Observation**: OBS-04, OBS-15

### Requirement: Event consumers
Потребителями событий SHALL быть notification-service — по одному
`@KafkaListener` на `user-created-events-topic`, `seller-events-topic` и
`order-events-topic` в consumer group `notification-group`, catalog-service —
`@KafkaListener` на `store-events-topic` в consumer group `catalog-group`, и
user-service — `@KafkaListener` на `product-events-topic` в consumer group
`user-service-group`. Используются строковые десериализаторы и настройки
concurrency, подтверждения и `auto-offset-reset` по умолчанию.

#### Scenario: Состав группы на стенде
- **WHEN** notification-service работает и подключен к Kafka
- **THEN** группа `notification-group` содержит трех участников, каждому назначены 3 партиции своего топика

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
заявки (`UnknownApplicationTypeException`) и для неизвестного или
некорректного события заказа SHALL публиковать сообщение в DLT без повторов.
Ошибки отправки email и Telegram для `user-created-events-topic` и
`seller-events-topic` SHALL перехватываться и SHALL NOT вызывать повторов;
для `order-events-topic` ошибка SMTP SHALL повторяться. Обертка
seller-события без eventType или payload SHALL пропускаться без DLT. У DLT
потребителей SHALL NOT быть.

#### Scenario: Невалидный JSON
- **WHEN** значение сообщения в `seller-events-topic` не является JSON
- **THEN** после 4 попыток (1 + 3 повтора) сообщение уходит в `seller-events-topic.DLT`

#### Scenario: Неизвестный eventType
- **WHEN** eventType в обертке `seller-events-topic` не входит в enum notification-service
- **THEN** десериализация падает, и сообщение уходит в DLT

#### Scenario: Неизвестный тип заявки
- **WHEN** SELLER_APPROVED содержит applicationType, неизвестный notification-service
- **THEN** после первой попытки сообщение уходит в `seller-events-topic.DLT`, повторов нет

#### Scenario: Обертка без payload
- **WHEN** в обертке `seller-events-topic` нет payload
- **THEN** сообщение пропускается с предупреждением в логе, в DLT не попадает

#### Scenario: Невалидный JSON события заказа
- **WHEN** значение сообщения в `order-events-topic` не является JSON
- **THEN** после первой попытки сообщение уходит в `order-events-topic.DLT`

**Observation**: OBS-09, OBS-14, OBS-15
**Assumption**: ASM-02, ASM-05

### Requirement: Services without event processing
catalog-service SHALL NOT публиковать сообщения Kafka, кроме публикации в
`store-events-topic.DLT` при ошибках обработки и PRODUCT_DELETED в
`product-events-topic`. seller-service и order-service SHALL NOT потреблять
сообщения; user-service SHALL потреблять только `product-events-topic`.
Consumer-настройки seller-service SHALL NOT использоваться.

#### Scenario: catalog-service
- **WHEN** в catalog-service создается или изменяется товар
- **THEN** сообщения в Kafka не публикуются

#### Scenario: Удаление товара
- **WHEN** в catalog-service удаляется товар
- **THEN** публикуется только PRODUCT_DELETED в `product-events-topic`

#### Scenario: order-service
- **WHEN** seller-service публикует STORE_BLOCKED
- **THEN** order-service его не потребляет; доступность товаров проверяется при оформлении через catalog-service

**Observation**: OBS-13
