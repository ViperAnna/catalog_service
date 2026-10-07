## ADDED Requirements

### Requirement: Contact data uniqueness
Email и телефон SHALL быть уникальными среди профилей. Email SHALL
сравниваться без учета регистра, телефон — в каноническом формате
`+7XXXXXXXXXX`. Попытка установить email или телефон, принадлежащий другому
профилю, SHALL отклоняться с кодом 409. Собственные значения изменяемого
профиля SHALL NOT считаться конфликтом. Уникальность SHALL обеспечиваться и
проверкой в сервисе, и unique constraint в БД на `users.email` и `users.phone`.

#### Scenario: Занятый email в другом регистре
- **WHEN** у другого профиля email "test@example.com", и пользователь меняет свой email на "Test@Example.com"
- **THEN** ответ 409 с message "User with email [test@example.com] already exist"

#### Scenario: Занятый телефон в другой форме
- **WHEN** у другого профиля phone "+79991234567", и пользователь меняет свой phone на "89991234567"
- **THEN** ответ 409 с message "User with phone [+79991234567] already exist"

#### Scenario: Собственные значения
- **WHEN** пользователь отправляет свой текущий email в другом регистре и свой текущий телефон в форме "8XXXXXXXXXX"
- **THEN** конфликта нет, ответ 200

#### Scenario: Конкурентные изменения
- **WHEN** два запроса одновременно устанавливают одинаковый свободный email или телефон, и проверка в сервисе проходит в обоих
- **THEN** один запрос успешен, второй отклоняется нарушением unique constraint (SQLState 23505) и получает 409 с сообщением для конфликтующего поля

#### Scenario: Профили без телефона
- **WHEN** у нескольких профилей phone не задан
- **THEN** это не считается конфликтом

### Requirement: Phone normalization
user-service SHALL принимать phone только в формах `+7XXXXXXXXXX`,
`7XXXXXXXXXX` и `8XXXXXXXXXX`, где X — цифра, и SHALL сохранять его в
каноническом формате `+7XXXXXXXXXX`: форма `7XXXXXXXXXX` дополняется `+`, у
формы `8XXXXXXXXXX` первая цифра заменяется на `+7`. Пробелы, скобки, дефисы
и номера других стран SHALL NOT приниматься.

#### Scenario: Форма с восьмеркой
- **WHEN** пользователь передает phone "89991234567"
- **THEN** сохраняется "+79991234567"

#### Scenario: Форма без плюса
- **WHEN** пользователь передает phone "79991234567"
- **THEN** сохраняется "+79991234567"

#### Scenario: Номер с разделителями
- **WHEN** пользователь передает phone "+7 999 123-45-67"
- **THEN** ответ 400

#### Scenario: Номер другой страны
- **WHEN** пользователь передает phone "+12025550123"
- **THEN** ответ 400

## MODIFIED Requirements

### Requirement: Profile provisioning on first read
При `GET /users/me`, если профиля с keycloakUserId = claim `sub` нет,
user-service SHALL создать профиль с firstName = `given_name`, lastName =
`family_name` и email = `email` из JWT, приведенным к нижнему регистру, и в
той же транзакции записать в outbox событие USER_CREATED. Другие эндпоинты
SHALL NOT создавать профиль.

#### Scenario: Первое обращение
- **WHEN** пользователь впервые вызывает `GET /users/me`
- **THEN** создается профиль из claims JWT и в outbox записывается USER_CREATED

#### Scenario: Email в смешанном регистре
- **WHEN** в JWT claim `email` = "John.Doe@Example.com"
- **THEN** профиль сохраняется с email "john.doe@example.com", и в payload USER_CREATED email = "john.doe@example.com"

#### Scenario: Повторное обращение
- **WHEN** профиль уже существует
- **THEN** новый профиль и событие не создаются

#### Scenario: В JWT нет family_name
- **WHEN** в JWT нет claim `family_name`
- **THEN** сохранение профиля нарушает ограничение NOT NULL и запрос завершается 5xx

**Observation**: OBS-01, OBS-02
**Assumption**: ASM-01

### Requirement: Own profile update
user-service SHALL принимать `PUT /users/me` с параметрами формы firstName,
lastName, phone, nicknameTelegram, email и вложенным address и SHALL менять в
профиле текущего пользователя только переданные (не null) поля, включая поля
адреса по отдельности. email SHALL сохраняться в нижнем регистре, phone — в
каноническом формате. Если профиля нет, SHALL возвращаться 404. Ответ SHALL
иметь код 200 и тело `{message, time}`.

#### Scenario: Изменение телефона
- **WHEN** пользователь передает только phone
- **THEN** меняется только phone, ответ 200 с message "The user was successfully updated."

#### Scenario: Изменение до первого чтения
- **WHEN** пользователь без профиля вызывает `PUT /users/me`
- **THEN** user-service отвечает 404

#### Scenario: Изменение email
- **WHEN** пользователь меняет email на свободный "New@Example.com"
- **THEN** в профиле user-service сохраняется "new@example.com", учетная запись Keycloak не меняется

#### Scenario: Занятый email
- **WHEN** пользователь меняет email на email другого профиля
- **THEN** ответ 409, профиль не меняется

**Observation**: OBS-01, OBS-03, OBS-08

### Requirement: Profile field validation
user-service SHALL отклонять с кодом 400 изменение профиля, если firstName,
lastName или nicknameTelegram длиннее 20 символов, phone не соответствует
`^(\+7|7|8)\d{10}$`, email не является email или длиннее 100 символов, или
любое поле адреса длиннее 20 символов. Все поля SHALL быть необязательными.

#### Scenario: Некорректный телефон
- **WHEN** phone = "12"
- **THEN** ответ 400 с message, начинающимся с "Validation failed:"

#### Scenario: Ранее допустимый номер
- **WHEN** phone = "+4915112345678" (раньше проходил шаблон `\+?[0-9]{7,15}`)
- **THEN** ответ 400

### Requirement: Admin user update and deletion
`PUT /admin/users/{keycloakId}` SHALL изменять профиль по тем же правилам и с
той же валидацией, нормализацией и проверкой уникальности, что
`PUT /users/me`, а `DELETE /admin/users/{keycloakId}` SHALL удалять профиль
по тем же правилам, что `DELETE /users/me`. Для несуществующего keycloakId
SHALL возвращаться 404.

#### Scenario: Удаление администратором
- **WHEN** администратор удаляет профиль пользователя
- **THEN** удаляется только запись в user-service, ответ 204

#### Scenario: Конфликт при изменении администратором
- **WHEN** администратор устанавливает профилю пользователя телефон другого профиля
- **THEN** ответ 409 с message "User with phone [<телефон>] already exist"

**Observation**: OBS-04, OBS-14

### Requirement: User service error responses
Для ошибок валидации (400), отсутствия ресурса (404) и конфликтов (409)
user-service SHALL возвращать тело `{message, time}` с time в формате
`yyyy-MM-dd HH:mm:ss`. Для 400 по валидации message SHALL начинаться с
"Validation failed: ". Нарушение unique constraint email или phone при
изменении профиля SHALL возвращаться как 409. Прочие ошибки SHALL
обрабатываться стандартно Spring.

#### Scenario: Ошибка БД
- **WHEN** изменение email нарушает уникальное ограничение в БД
- **THEN** ответ 409 с телом `{message, time}` и message "User with email [<email>] already exist"

**Observation**: OBS-03
**Assumption**: ASM-03
