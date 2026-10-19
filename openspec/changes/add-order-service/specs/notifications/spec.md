## ADDED Requirements

### Requirement: Order event consumption
notification-service SHALL потреблять `order-events-topic` в consumer group
`notification-group` и разбирать конверт `{aggregateType, eventType, payload}`
с eventType строкой (контракт `integration-events` `order-events-topic message
contract`). Сообщения в Telegram по событиям заказа SHALL NOT отправляться.

#### Scenario: Заказ оформлен
- **WHEN** приходит ORDER_CREATED с email `buyer@test.com`
- **THEN** на `buyer@test.com` уходит письмо о создании заказа

#### Scenario: Заказ отменен
- **WHEN** приходит ORDER_CANCELLED с email `buyer@test.com`
- **THEN** на `buyer@test.com` уходит письмо об отмене заказа

### Requirement: Order event classification
notification-service SHALL относить каждое сообщение `order-events-topic` к
одному классу: уведомляемое (ORDER_CREATED, ORDER_CANCELLED), известное без
уведомления (явный список, в MVP пуст), неизвестное или некорректное. Известное
без уведомления SHALL отмечаться обработанным без письма с INFO в логе.
Неизвестное и некорректное SHALL обрабатываться по `integration-events`
`Order events DLT policy`. Пропуск без записи в лог SHALL NOT допускаться.

#### Scenario: Известное событие без уведомления
- **WHEN** eventType входит в список событий без уведомления
- **THEN** письмо не отправляется, eventId отмечен обработанным, в логе INFO с eventId и eventType

#### Scenario: Неизвестное событие
- **WHEN** eventType не входит ни в один список
- **THEN** письмо не отправляется, сообщение в `order-events-topic.DLT`, eventId не отмечен обработанным

### Requirement: Order event idempotent processing
notification-service SHALL хранить `eventId` обработанных событий заказа
(без email) и SHALL NOT отправлять письмо повторно для уже обработанного
`eventId`. `eventId` SHALL отмечаться обработанным только после успешной
отправки письма или осознанного пропуска. При сбое между отправкой письма и
отметкой допускается повторное письмо.

#### Scenario: Повторная доставка
- **WHEN** ORDER_CREATED с eventId `e-1` доставлен второй раз после успешной обработки
- **THEN** письмо не отправляется, в логе INFO о дубликате с eventId

#### Scenario: Переотправка из DLT
- **WHEN** сообщение из DLT с уже обработанным eventId переотправлено в `order-events-topic`
- **THEN** повторное письмо не отправляется

### Requirement: Order notification emails
Письмо о создании заказа SHALL иметь тему "Order {orderId} created" и
содержать номер заказа, сумму и число позиций. Письмо об отмене SHALL иметь
тему "Order {orderId} cancelled" и содержать номер заказа. Письма SHALL
отправляться через `Email transport configuration`.

#### Scenario: Содержание письма о заказе
- **WHEN** приходит ORDER_CREATED с orderId `o-1`, totalAmount 250.00 и itemsCount 2
- **THEN** письмо имеет тему "Order o-1 created" и содержит `o-1`, 250.00 и 2

### Requirement: Order notification without recipient
Если `email` в payload уведомляемого события заказа отсутствует или пуст,
письмо SHALL NOT отправляться, в лог SHALL записываться WARN с orderId,
eventId и eventType, а eventId SHALL отмечаться обработанным без DLT.

#### Scenario: Нет адреса
- **WHEN** приходит ORDER_CREATED с `email` = null
- **THEN** письмо не отправляется, сообщение не попадает в DLT, в логе WARN с eventId

### Requirement: Order notification delivery errors
Ошибка отправки письма по событию заказа SHALL считаться временной ошибкой
обработки: сообщение SHALL повторяться и после исчерпания повторов уходить в
`order-events-topic.DLT`, а eventId SHALL NOT отмечаться обработанным. В лог
SHALL NOT попадать адрес получателя.

#### Scenario: SMTP недоступен
- **WHEN** отправка письма о заказе падает при каждой попытке
- **THEN** сообщение в DLT, в логе orderId, eventId и eventType без адреса

## MODIFIED Requirements

### Requirement: Event consumer error handling
Исключение при обработке сообщения из `user-created-events-topic`,
`seller-events-topic` или `order-events-topic` SHALL повторяться 3 раза с
интервалом 1 секунда, после чего сообщение SHALL публиковаться в
`<topic>.DLT`. Ошибки отправки email и Telegram для
`user-created-events-topic` и `seller-events-topic` SHALL NOT считаться
ошибками обработки. Неизвестное или некорректное событие заказа SHALL
публиковаться в DLT без повторов. Потребителей DLT SHALL NOT существовать.

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения в `user-created-events-topic` не является JSON события
- **THEN** после 3 повторов сообщение публикуется в `user-created-events-topic.DLT`

#### Scenario: SMTP недоступен
- **WHEN** отправка приветственного письма завершилась ошибкой
- **THEN** ошибка логируется, сообщение считается обработанным

#### Scenario: Некорректное событие заказа
- **WHEN** в `order-events-topic` приходит конверт без eventType
- **THEN** сообщение сразу публикуется в `order-events-topic.DLT`, в логе ERROR

**Assumption**: ASM-02
