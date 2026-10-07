## ADDED Requirements

### Requirement: Self-moderation prohibition
seller-service SHALL отклонять с кодом 403 и телом `{message, time}`
(message `Moderator can't review own application.`) approve и reject заявки,
у которой keycloakUserId совпадает с claim `sub` модератора. Заявка, Seller,
роль в Keycloak и outbox SHALL NOT меняться. Проверка SHALL выполняться после
поиска заявки (иначе 404) и до проверки статуса PENDING (иначе 409). Просмотр
своей заявки модератором SHALL NOT ограничиваться.

#### Scenario: Одобрение своей заявки
- **WHEN** модератор с `sub` "m-1" одобряет PENDING-заявку пользователя "m-1"
- **THEN** seller-service отвечает 403 с message `Moderator can't review own application.`
- **AND** заявка остается PENDING, Seller не создается, gRPC-вызов не выполняется, событие не записывается

#### Scenario: Отклонение своей заявки
- **WHEN** модератор с `sub` "m-1" отклоняет заявку пользователя "m-1" с причиной
- **THEN** seller-service отвечает 403, заявка не меняется и событие не записывается

#### Scenario: Своя уже рассмотренная заявка
- **WHEN** модератор одобряет свою заявку в статусе REJECTED
- **THEN** seller-service отвечает 403

#### Scenario: Просмотр своей заявки
- **WHEN** модератор запрашивает `GET /moderator/seller-applications/{id}` своей заявки
- **THEN** seller-service отвечает 200 с заявкой

#### Scenario: Чужая заявка
- **WHEN** модератор с `sub` "m-1" одобряет PENDING-заявку пользователя "u-2"
- **THEN** одобрение выполняется как обычно

## MODIFIED Requirements

### Requirement: Application rejection
seller-service SHALL принимать `POST /moderator/seller-applications/{id}/reject`
с телом JSON `{moderatorComment}`, где moderatorComment (причина отказа)
обязателен, не пуст и не длиннее 500 символов, иначе 400 без изменения
заявки. Поле status в теле SHALL NOT входить в контракт: если оно передано,
оно игнорируется. seller-service SHALL перевести заявку в REJECTED, сохранить
moderatorComment и записать в outbox SELLER_REJECTED. Успешный ответ SHALL
иметь код 200 без тела.

#### Scenario: Отклонение с комментарием
- **WHEN** модератор отклоняет PENDING-заявку с moderatorComment
- **THEN** заявка REJECTED, комментарий сохранен, в outbox записано SELLER_REJECTED с этим moderatorComment

#### Scenario: Причина не указана
- **WHEN** тело запроса не содержит moderatorComment или он пуст или состоит из пробелов
- **THEN** seller-service отвечает 400 с message, содержащим `Rejection reason is required.`
- **AND** заявка остается PENDING и событие не записывается

#### Scenario: Поле status не передано
- **WHEN** тело запроса содержит только moderatorComment
- **THEN** заявка становится REJECTED, ответ 200

#### Scenario: Передан status APPROVED
- **WHEN** тело содержит status = APPROVED (или любое другое значение) и moderatorComment
- **THEN** поле status игнорируется, заявка становится REJECTED, ответ 200

**Observation**: OBS-16

### Requirement: Seller service error response format
Ответ seller-service об ошибке на любом эндпоинте SHALL иметь тело JSON
`{message, time}`, где `time` имеет формат `yyyy-MM-dd HH:mm:ss`. Ошибки
валидации, нечитаемое тело и неверный тип параметра SHALL давать 400,
`ResourceNotFoundException` — 404, `ResourceConflictException` — 409 с текстом
исключения, прочие исключения — 500 с message `Internal server error` без
деталей. Ответы 401 и 403 Spring Security SHALL NOT меняться.

#### Scenario: Ошибка валидации
- **WHEN** пользователь подает заявку на регистрацию с inn из 11 цифр
- **THEN** seller-service отвечает 400 с телом `{message, time}`, где message начинается с `Validation failed: ` и содержит `INN must contain 10 or 12 digits.`

#### Scenario: Ошибка валидации формы
- **WHEN** продавец отправляет `PUT /seller-applications/update` с paymentAccount из 19 цифр
- **THEN** seller-service отвечает 400 с телом `{message, time}`, где message содержит `Payment account must contain exactly 20 digits.`

#### Scenario: Неизвестное значение enum
- **WHEN** тело запроса не читается: некорректный JSON или неизвестное значение поля-перечисления (в теле reject таких полей после удаления status нет, поэтому пример — обрезанный JSON `{"moderatorComment": `)
- **THEN** seller-service отвечает 400 с телом `{message, time}`

#### Scenario: Ресурс не найден
- **WHEN** модератор запрашивает `GET /moderator/seller-applications/999999`, а заявки нет
- **THEN** seller-service отвечает 404 с message `Application not found with id: [999999].`

#### Scenario: Конфликт
- **WHEN** модератор одобряет уже одобренную заявку 5
- **THEN** seller-service отвечает 409 с message `Application with id: [5] already reviewed.`

#### Scenario: Внутренняя ошибка
- **WHEN** при обработке запроса возникает ошибка БД или gRPC, не переведенная в 4xx
- **THEN** seller-service отвечает 500 с message `Internal server error`, без текста исключения и stack trace
- **AND** исключение записывается в лог

#### Scenario: Нет роли
- **WHEN** пользователь только с ролью USER вызывает `GET /sellers/me`
- **THEN** seller-service отвечает 403, как и раньше
