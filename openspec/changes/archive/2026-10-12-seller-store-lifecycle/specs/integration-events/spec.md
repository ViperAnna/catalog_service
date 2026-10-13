## MODIFIED Requirements

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
