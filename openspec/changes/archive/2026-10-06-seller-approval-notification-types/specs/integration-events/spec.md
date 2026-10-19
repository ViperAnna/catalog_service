## MODIFIED Requirements

### Requirement: seller-events-topic message contract
Сообщение в `seller-events-topic` SHALL иметь строковый ключ (id заявки) и
строковое значение — обертку `{aggregateType, eventType, payload}`:
aggregateType = `SELLER_APPLICATION`, eventType — SELLER_APPLICATION_CREATED,
SELLER_APPROVED или SELLER_REJECTED, payload — JSON события строкой. Payload
SELLER_APPROVED SHALL содержать applicationType (REGISTRATION, UPDATE или
DELETION); consumer SHALL допускать его отсутствие.

#### Scenario: Сообщение об одобрении
- **WHEN** seller-service публикует SELLER_APPROVED для заявки 7 типа DELETION
- **THEN** ключ `7`, значение — обертка, у которой поле payload — строка с JSON `{applicationId, keycloakUserId, sellerName, email, applicationType}` и applicationType = "DELETION"

#### Scenario: Сообщение об одобрении без типа
- **WHEN** notification-service получает SELLER_APPROVED, payload которого не содержит applicationType
- **THEN** сообщение обрабатывается без ошибки и не уходит в DLT

#### Scenario: Сообщение об одобрении с неизвестным типом
- **WHEN** notification-service получает SELLER_APPROVED с applicationType "FOO"
- **THEN** сообщение сразу уходит в `seller-events-topic.DLT`, без повторов

**Observation**: OBS-01, OBS-04, OBS-05, OBS-10

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
