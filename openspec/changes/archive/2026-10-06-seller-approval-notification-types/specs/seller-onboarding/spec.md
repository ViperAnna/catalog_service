## MODIFIED Requirements

### Requirement: Outbox event recording
seller-service SHALL записывать событие в таблицу `outbox_events` в той же
транзакции, что и изменение заявки, с aggregateType `SELLER_APPLICATION`,
aggregateId равным id заявки, eventType, payload в JSON, createdAt текущего
времени и processed = false. События SHALL записываться только при подаче
заявки любого типа (REGISTRATION, UPDATE, DELETION), при одобрении и при
отклонении заявки. Payload SELLER_APPROVED SHALL содержать applicationType
одобренной заявки.

#### Scenario: Payload событий
- **WHEN** записывается SELLER_APPLICATION_CREATED или SELLER_APPROVED
- **THEN** payload содержит applicationId, keycloakUserId, sellerName, email из заявки

#### Scenario: Одинаковое событие для всех типов заявок
- **WHEN** подается заявка REGISTRATION, UPDATE или DELETION
- **THEN** записывается событие с eventType SELLER_APPLICATION_CREATED и одним и тем же набором полей payload
- **AND** payload не содержит тип заявки

#### Scenario: Тип заявки в одобрении
- **WHEN** одобряется заявка типа REGISTRATION, UPDATE или DELETION
- **THEN** payload SELLER_APPROVED дополнительно содержит applicationType с этим типом

#### Scenario: Payload отклонения
- **WHEN** записывается SELLER_REJECTED
- **THEN** payload дополнительно содержит moderatorComment и не содержит applicationType

#### Scenario: Откат транзакции
- **WHEN** транзакция подачи или модерации откатывается
- **THEN** событие в outbox не сохраняется

### Requirement: Seller event email notifications
notification-service SHALL потреблять `seller-events-topic` (consumer group
`notification-group`) и отправлять email от `spring.mail.from`: на
SELLER_APPLICATION_CREATED — на `notification.moderator.email`, на
SELLER_APPROVED и SELLER_REJECTED — на email из события. Письмо на
SELLER_APPROVED SHALL выбираться по applicationType. Сообщение без eventType
или payload SHALL пропускаться с предупреждением в логе.

#### Scenario: Новая заявка
- **WHEN** получено SELLER_APPLICATION_CREATED
- **THEN** модератору отправляется письмо "New application!" с sellerName и email заявителя

#### Scenario: Одобрение
- **WHEN** получено SELLER_APPROVED с applicationType REGISTRATION
- **THEN** на email из события отправляется письмо "Seller application approved!" о том, что магазин можно использовать

#### Scenario: Одобрение изменения данных
- **WHEN** получено SELLER_APPROVED с applicationType UPDATE
- **THEN** на email из события отправляется письмо "Seller data change approved" о том, что новые данные продавца вступили в силу

#### Scenario: Одобрение удаления
- **WHEN** получено SELLER_APPROVED с applicationType DELETION
- **THEN** на email из события отправляется письмо "Seller account deletion approved" о том, что аккаунт продавца закрыт
- **AND** письмо не содержит фразы "Your shop can now be used as a seller shop"

#### Scenario: Одобрение без типа заявки
- **WHEN** получено SELLER_APPROVED без applicationType или с пустым значением (например, событие записано до этого change)
- **THEN** отправляется письмо "Seller application approved!", как для REGISTRATION

#### Scenario: Одобрение с неизвестным типом заявки
- **WHEN** получено SELLER_APPROVED с непустым applicationType, отличным от REGISTRATION, UPDATE и DELETION (например, "FOO" или "update")
- **THEN** письмо не отправляется, в лог пишется ошибка с applicationId и значением типа
- **AND** сообщение обрабатывается по правилам `Seller events consumer error handling` для неизвестного типа заявки

#### Scenario: Отклонение
- **WHEN** получено SELLER_REJECTED
- **THEN** на email из события отправляется письмо "Seller application rejected" с moderatorComment в качестве причины

#### Scenario: Telegram
- **WHEN** обрабатывается любое событие seller-events-topic
- **THEN** уведомление в Telegram не отправляется

### Requirement: Seller events consumer error handling
Ошибка отправки email (MailException) SHALL логироваться без повторной попытки,
и сообщение SHALL считаться обработанным. Ошибка обработки сообщения (например,
десериализации) SHALL повторяться 3 раза с интервалом 1 секунда, после чего
сообщение SHALL публиковаться в топик `seller-events-topic.DLT`. Сообщение
SELLER_APPROVED с неизвестным непустым applicationType SHALL публиковаться в
`seller-events-topic.DLT` сразу, без повторов. Потребителя DLT SHALL NOT
существовать.

#### Scenario: SMTP недоступен
- **WHEN** отправка письма завершилась MailException
- **THEN** ошибка логируется, повторной отправки нет

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения не является валидным конвертом
- **THEN** после 3 повторов сообщение публикуется в seller-events-topic.DLT

#### Scenario: Неизвестный тип заявки
- **WHEN** получено SELLER_APPROVED с applicationType "FOO"
- **THEN** сообщение после первой попытки публикуется в seller-events-topic.DLT с исходными ключом и значением, повторов нет, письмо не отправлено

**Assumption**: ASM-03, ASM-04, ASM-07
