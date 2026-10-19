## MODIFIED Requirements

### Requirement: Seller update application submission
seller-service SHALL принимать `PUT /seller-applications/update` с параметрами
формы phone, email, paymentAccount и SHALL создавать заявку UPDATE/PENDING,
только если для пользователя существует Seller в статусе ACTIVE. В заявку SHALL
копироваться sellerName и inn текущего Seller, а из запроса SHALL переноситься
только email и paymentAccount. Ответ SHALL иметь код 200. В той же транзакции
SHALL записываться событие SELLER_APPLICATION_CREATED.

#### Scenario: Успешная подача изменения
- **WHEN** ACTIVE-продавец без PENDING-заявок отправляет валидные phone, email, paymentAccount
- **THEN** создается заявка UPDATE/PENDING с sellerName и inn из Seller и email, paymentAccount из запроса
- **AND** phoneNumber заявки равен null
- **AND** в outbox в той же транзакции записывается событие SELLER_APPLICATION_CREATED с applicationId, keycloakUserId, sellerName и email созданной заявки

#### Scenario: Подача изменения без email
- **WHEN** ACTIVE-продавец без PENDING-заявок отправляет валидную заявку UPDATE без email
- **THEN** создается заявка UPDATE/PENDING
- **AND** в outbox записывается событие SELLER_APPLICATION_CREATED с email = null

#### Scenario: Отклоненная подача изменения не создает событие
- **WHEN** подача UPDATE отклоняется с 400, 404 или 409
- **THEN** заявка не создается и событие в outbox не записывается

#### Scenario: Продавец не найден
- **WHEN** для пользователя нет Seller
- **THEN** seller-service отвечает 404

#### Scenario: Продавец не ACTIVE
- **WHEN** Seller пользователя имеет статус DELETED
- **THEN** seller-service отвечает 409

#### Scenario: Невалидный счет
- **WHEN** paymentAccount содержит не 20 цифр
- **THEN** seller-service отвечает 400

**Observation**: OBS-01, OBS-12

### Requirement: Seller deletion application submission
seller-service SHALL принимать `DELETE /seller-applications/deletion` с телом
JSON `{reason}` (обязательно, не длиннее 500 символов) и SHALL создавать заявку
DELETION/PENDING со снимком sellerName, inn, phoneNumber, email, paymentAccount
текущего Seller и requestReason = reason, только если Seller существует и
ACTIVE. Ответ SHALL иметь код 200. В той же транзакции SHALL записываться
событие SELLER_APPLICATION_CREATED.

#### Scenario: Успешная подача удаления
- **WHEN** ACTIVE-продавец без PENDING-заявок отправляет непустую причину
- **THEN** создается заявка DELETION/PENDING с данными Seller и requestReason
- **AND** в outbox в той же транзакции записывается событие SELLER_APPLICATION_CREATED с applicationId, keycloakUserId, sellerName и email созданной заявки

#### Scenario: Отклоненная подача удаления не создает событие
- **WHEN** подача DELETION отклоняется с 400, 404 или 409
- **THEN** заявка не создается и событие в outbox не записывается

#### Scenario: Причина не указана
- **WHEN** reason пуст
- **THEN** seller-service отвечает 400

### Requirement: Outbox event recording
seller-service SHALL записывать событие в таблицу `outbox_events` в той же
транзакции, что и изменение заявки, с aggregateType `SELLER_APPLICATION`,
aggregateId равным id заявки, eventType, payload в JSON, createdAt текущего
времени и processed = false. События SHALL записываться только при подаче
заявки любого типа (REGISTRATION, UPDATE, DELETION), при одобрении и при
отклонении заявки.

#### Scenario: Payload событий
- **WHEN** записывается SELLER_APPLICATION_CREATED или SELLER_APPROVED
- **THEN** payload содержит applicationId, keycloakUserId, sellerName, email из заявки

#### Scenario: Одинаковое событие для всех типов заявок
- **WHEN** подается заявка REGISTRATION, UPDATE или DELETION
- **THEN** записывается событие с eventType SELLER_APPLICATION_CREATED и одним и тем же набором полей payload
- **AND** payload не содержит тип заявки

#### Scenario: Payload отклонения
- **WHEN** записывается SELLER_REJECTED
- **THEN** payload дополнительно содержит moderatorComment

#### Scenario: Откат транзакции
- **WHEN** транзакция подачи или модерации откатывается
- **THEN** событие в outbox не сохраняется
