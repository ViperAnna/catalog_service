## ADDED Requirements

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

## MODIFIED Requirements

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
