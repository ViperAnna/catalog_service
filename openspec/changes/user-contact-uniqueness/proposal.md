# Proposal

## Why

В user-service конфликты контактных данных пользователя обрабатываются плохо
(`user-profile` OBS-03, OBS-09, ASM-03):
- смена email на уже занятый отдает 500 с ошибкой БД, потому что уникальность
  проверяется только ограничением `UNIQUE(email)`, причем с учетом регистра;
- телефон не уникален и хранится в произвольном виде, хотя сообщение
  `USER_PHONE_ALREADY_EXIST` объявлено.

Change C3 из разбора OBS `user-profile`: email и телефон становятся
уникальными контактами пользователя с понятным ответом 409.

## What Changes

- **Email приводится к lowercase** перед сохранением: при создании профиля из
  JWT (`GET /users/me`) и при изменении (`PUT /users/me`,
  `PUT /admin/users/{keycloakId}`). `Test@example.com` и `test@example.com`
  считаются одним email. Уникальность в БД обеспечивает существующий
  `UNIQUE(email)`.
- **Телефон становится уникальным и хранится в каноническом формате РФ
  `+7XXXXXXXXXX`.** Принимаются только `+7XXXXXXXXXX`, `7XXXXXXXXXX` и
  `8XXXXXXXXXX`, последние две формы нормализуются. Для `phone` в сущности
  объявляется unique constraint, его создает `ddl-auto: update`.
- **Конфликт дает 409** с телом `{message, time}`:
  `USER_EMAIL_ALREADY_EXIST` для email и `USER_PHONE_ALREADY_EXIST` для
  телефона. Сначала выполняется проверка в сервисе. Нарушение unique
  constraint при гонке (`SQLState 23505`) тоже переводится в соответствующий
  409.
- Собственный email и телефон пользователя (в том числе в другом регистре или
  в другой допустимой форме) при обновлении конфликтом не считаются.
- **Валидация frontend** (`ProfilePage`, `AdminUsersPage/UserEditModal`)
  приводится в соответствие с backend.
- **BREAKING (API):** `phone`, который раньше принимался по шаблону
  `\+?[0-9]{7,15}` (например, номера других стран или 7–9 цифр), теперь
  отклоняется с 400. Смена занятого email теперь дает 409 вместо 500.

### Out of scope

- Миграции и Flyway: схема остается на `ddl-auto: update`.
- Автоматическая нормализация уже сохраненных email и телефонов. Перед
  применением проверяется, что конфликтов с учетом нормализации нет, после
  чего риск по старым записям принимается.
- Конфликт email при создании профиля из JWT. Сейчас он дает 5xx, и это
  остается задачей C2 (`user-profile` OBS-02, ASM-01). В C3 при создании
  только приводится к lowercase.
- Синхронизация email с Keycloak (`user-profile` OBS-03, часть b, D3).
- Изменение контракта Kafka. В `USER_CREATED` email приходит в lowercase,
  формат события не меняется.
- Тестовая инфраструктура user-service: она относится к приостановленному C1.

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `user-profile`: изменяются требования `Profile provisioning on first read`,
  `Own profile update`, `Profile field validation`,
  `Admin user update and deletion`, `User service error responses`;
  добавляются требования `Contact data uniqueness` и `Phone normalization`.

## Impact

- **user-service:** `UserServiceImpl` (нормализация, проверки, перевод
  нарушения constraint в 409), `UserUpdateRequest` (валидация phone),
  `User` (unique для phone), `UserRepository` (запросы проверки конфликтов),
  обработка 409 (`ResourceConflictException` уже обрабатывается
  `GlobalExceptionHandler`).
- **БД `user_service`:** новый unique constraint на `users.phone` через
  `ddl-auto: update`. Ручные проверки до и после применения описаны в tasks.
  При деплое на production их нужно повторить для production БД.
- **frontend:** валидация телефона в двух формах.
- **Kafka, Keycloak:** без изменений.
- **Тесты:** C3 требует тестов на 409, нормализацию и гонку. Эти задачи
  включены в tasks и отложены до возобновления C1.
