## ADDED Requirements

### Requirement: store-events-topic message contract
Сообщение в `store-events-topic` SHALL иметь строковый ключ, равный id
магазина, и строковое значение — конверт `{aggregateType, eventType, payload}`
того же формата, что в `seller-events-topic`, где aggregateType = `STORE`,
eventType = `STORE_DELETED`, а payload — JSON `{storeId, sellerId}`,
сериализованный в строку. Producer SHALL быть seller-service, consumer —
catalog-service.

#### Scenario: Сообщение об удалении магазина
- **WHEN** seller-service публикует STORE_DELETED для магазина 3 продавца 7
- **THEN** ключ `3`, значение — конверт с aggregateType "STORE", eventType "STORE_DELETED" и payload-строкой с JSON `{"storeId":3,"sellerId":7}`

## MODIFIED Requirements

### Requirement: Topic declarations
`user-created-events-topic` SHALL объявляться бинами `NewTopic` (3 партиции,
replication factor 1) одновременно в user-service и notification-service, а
`seller-events-topic` и `store-events-topic` (по 3 партиции, replication
factor 1) — в seller-service. Топики `*.DLT` SHALL NOT объявляться явно.

#### Scenario: Список топиков на стенде
- **WHEN** запрашивается список топиков брокера dev-стенда без ошибок обработки
- **THEN** он содержит `user-created-events-topic`, `seller-events-topic` и `store-events-topic`, а топиков `*.DLT` нет

**Observation**: OBS-09, OBS-11
**Assumption**: ASM-02

### Requirement: Event consumers
Потребителями событий SHALL быть notification-service — по одному
`@KafkaListener` на `user-created-events-topic` и `seller-events-topic` в
consumer group `notification-group` — и catalog-service — `@KafkaListener` на
`store-events-topic` в consumer group `catalog-group`. Используются строковые
десериализаторы и настройки concurrency, подтверждения и `auto-offset-reset`
по умолчанию.

#### Scenario: Состав группы на стенде
- **WHEN** notification-service работает и подключен к Kafka
- **THEN** группа `notification-group` содержит двух участников, каждому назначены 3 партиции своего топика

#### Scenario: Группа catalog-service
- **WHEN** catalog-service работает и подключен к Kafka
- **THEN** группа `catalog-group` содержит участника, которому назначены 3 партиции `store-events-topic`

**Assumption**: ASM-03, ASM-04

### Requirement: Services without event processing
catalog-service SHALL NOT публиковать сообщения Kafka, кроме публикации в
`store-events-topic.DLT` при ошибках обработки. seller-service и user-service
SHALL NOT потреблять сообщения. Consumer-настройки seller-service SHALL NOT
использоваться.

#### Scenario: catalog-service
- **WHEN** в catalog-service создается или изменяется товар
- **THEN** сообщения в Kafka не публикуются

**Observation**: OBS-13
