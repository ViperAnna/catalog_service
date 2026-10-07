# user-profile Specification

## Purpose
Baseline (as-is) поведения user-service по ведению локального профиля
пользователя: создание профиля из JWT при первом обращении, просмотр, изменение
и удаление собственного профиля, администрирование профилей и публикация
события `USER_CREATED` через outbox в Kafka. Спецификация описывает фактическое
текущее поведение кода, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Код user-service в
  рабочей копии не изменен.
- Метод: статический анализ кода, сгенерированного `UserMapperImpl` и схемы
  таблицы `users` в dev-БД. Все тесты user-service закомментированы,
  runtime-поведение не проверялось.
- Хранение: PostgreSQL, база `user_service`, таблицы `users` и `outbox_event`.
- Учетные записи, пароли и роли хранятся в Keycloak (realm
  `user-service-realm`). user-service их не создает и не изменяет, за
  исключением роли SELLER (см. `seller-onboarding`).
- Обработка `USER_CREATED` в notification-service описывается отдельным spec.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код сейчас. Изменять его
  нужно отдельным change.
- Пути указаны относительно user-service. Через api-gateway они доступны без
  изменений, через nginx — с префиксом `/api`.

## Requirements

### Requirement: User profile API access
user-service SHALL разрешать `/users/me` только с JWT, содержащим realm-роль
USER, а `/admin/users/**` только с JWT, содержащим realm-роль ADMIN. JWT SHALL
проверяться по подписи через JWK set Keycloak без проверки issuer. Роли SHALL
извлекаться из claim `realm_access.roles`.

#### Scenario: Нет роли ADMIN
- **WHEN** пользователь с ролью USER вызывает `GET /admin/users`
- **THEN** user-service отвечает 403

#### Scenario: Запрос без токена
- **WHEN** клиент без токена вызывает `GET /users/me`
- **THEN** user-service отвечает 401

**Observation**: OBS-06

### Requirement: Profile provisioning on first read
При `GET /users/me`, если профиля с keycloakUserId = claim `sub` нет,
user-service SHALL создать профиль с firstName = `given_name`, lastName =
`family_name` и email = `email` из JWT и в той же транзакции записать в outbox
событие USER_CREATED. Другие эндпоинты SHALL NOT создавать профиль.

#### Scenario: Первое обращение
- **WHEN** пользователь впервые вызывает `GET /users/me`
- **THEN** создается профиль из claims JWT и в outbox записывается USER_CREATED

#### Scenario: Повторное обращение
- **WHEN** профиль уже существует
- **THEN** новый профиль и событие не создаются

#### Scenario: В JWT нет family_name
- **WHEN** в JWT нет claim `family_name`
- **THEN** сохранение профиля нарушает ограничение NOT NULL и запрос завершается 5xx

**Observation**: OBS-01, OBS-02
**Assumption**: ASM-01

### Requirement: Own profile retrieval
`GET /users/me` SHALL возвращать профиль текущего пользователя, дополненный
полем roles — списком realm-ролей из JWT без `offline_access`,
`uma_authorization` и `default-roles-user-service-realm`.

#### Scenario: Роли в профиле
- **WHEN** в JWT роли USER, SELLER, offline_access
- **THEN** roles = ["USER", "SELLER"]

**Observation**: OBS-07

### Requirement: Own profile update
user-service SHALL принимать `PUT /users/me` с параметрами формы firstName,
lastName, phone, nicknameTelegram, email и вложенным address и SHALL менять в
профиле текущего пользователя только переданные (не null) поля, включая поля
адреса по отдельности. Если профиля нет, SHALL возвращаться 404. Ответ SHALL
иметь код 200 и тело `{message, time}`.

#### Scenario: Изменение телефона
- **WHEN** пользователь передает только phone
- **THEN** меняется только phone, ответ 200 с message "The user was successfully updated."

#### Scenario: Изменение до первого чтения
- **WHEN** пользователь без профиля вызывает `PUT /users/me`
- **THEN** user-service отвечает 404

#### Scenario: Изменение email
- **WHEN** пользователь меняет email
- **THEN** email меняется только в профиле user-service, учетная запись Keycloak не меняется

**Observation**: OBS-01, OBS-03, OBS-08

### Requirement: Profile field validation
user-service SHALL отклонять с кодом 400 изменение профиля, если firstName,
lastName или nicknameTelegram длиннее 20 символов, phone не соответствует
`\+?[0-9]{7,15}`, email не является email или длиннее 100 символов, или любое
поле адреса длиннее 20 символов. Все поля SHALL быть необязательными.

#### Scenario: Некорректный телефон
- **WHEN** phone = "12"
- **THEN** ответ 400 с message, начинающимся с "Validation failed:"

### Requirement: Own profile deletion
`DELETE /users/me` SHALL удалять только запись профиля текущего пользователя в
user-service и SHALL отвечать 204. Если профиля нет, SHALL возвращаться 404.
Учетная запись Keycloak, вишлисты и данные других сервисов SHALL NOT
изменяться.

#### Scenario: Удаление профиля
- **WHEN** пользователь удаляет свой профиль
- **THEN** запись профиля удалена, ответ 204, учетная запись Keycloak сохранена

#### Scenario: Чтение после удаления
- **WHEN** после удаления пользователь вызывает `GET /users/me` с действующим JWT
- **THEN** профиль создается заново с новым id и в outbox записывается новое USER_CREATED

**Observation**: OBS-04, OBS-05

### Requirement: Admin user listing and retrieval
user-service SHALL возвращать по `GET /admin/users` все профили одним списком
без пагинации, а по `GET /admin/users/{keycloakId}` профиль с указанным
keycloakUserId (иначе 404). Поле roles в этих ответах SHALL быть null.

#### Scenario: Профиль не найден
- **WHEN** администратор запрашивает несуществующий keycloakId
- **THEN** ответ 404 с message "User not found with id: [<keycloakId>]."

**Observation**: OBS-01, OBS-07

### Requirement: Admin user update and deletion
`PUT /admin/users/{keycloakId}` SHALL изменять профиль по тем же правилам и с
той же валидацией, что `PUT /users/me`, а `DELETE /admin/users/{keycloakId}`
SHALL удалять профиль по тем же правилам, что `DELETE /users/me`. Для
несуществующего keycloakId SHALL возвращаться 404.

#### Scenario: Удаление администратором
- **WHEN** администратор удаляет профиль пользователя
- **THEN** удаляется только запись в user-service, ответ 204

**Observation**: OBS-04, OBS-14

### Requirement: User response format
Профиль в ответах SHALL содержать поля id (внутренний UUID), keycloakUserId,
firstName, lastName, phone, nicknameTelegram, email, address
(`{country, city, street, numberOfHouse, numberOfApartment, postalCode}` или
null) и roles. Даты создания и изменения в ответ SHALL NOT входить.

#### Scenario: Профиль без адреса
- **WHEN** у профиля нет адреса
- **THEN** address = null

### Requirement: USER_CREATED outbox event
При создании профиля user-service SHALL записывать в `outbox_event` событие с
aggregateType `USER`, aggregateId = id профиля, eventType `USER_CREATED`,
processed = false и payload JSON
`{userId, keycloakUserId, email, username}`, где userId — внутренний UUID
профиля, а username — claim `preferred_username`.

#### Scenario: Payload события
- **WHEN** создается профиль пользователя "jdoe"
- **THEN** payload содержит userId профиля, его keycloakUserId, email и username = "jdoe"

**Observation**: OBS-15

### Requirement: USER_CREATED publishing
user-service SHALL через 3 секунды после окончания предыдущего цикла выбирать
до 100 событий с processed = false по возрастанию createdAt и публиковать
каждое в топик `user-created-events-topic` с ключом aggregateId и значением,
равным payload, ожидая подтверждения не более 10 секунд. Событие SHALL
помечаться processed после подтверждения, при ошибке цикл SHALL прекращаться.

#### Scenario: Публикация
- **WHEN** в outbox есть необработанное USER_CREATED
- **THEN** в `user-created-events-topic` публикуется сообщение без конверта, и событие помечается processed

#### Scenario: Kafka недоступна
- **WHEN** отправка завершилась ошибкой
- **THEN** событие остается processed = false до следующего цикла

**Observation**: OBS-13
**Assumption**: ASM-04

### Requirement: User service error responses
Для ошибок валидации (400), отсутствия ресурса (404) и конфликтов (409)
user-service SHALL возвращать тело `{message, time}` с time в формате
`yyyy-MM-dd HH:mm:ss`. Для 400 по валидации message SHALL начинаться с
"Validation failed: ". Прочие ошибки SHALL обрабатываться стандартно Spring.

#### Scenario: Ошибка БД
- **WHEN** изменение email нарушает уникальное ограничение
- **THEN** ответ 500 без тела `{message, time}`

**Observation**: OBS-03
**Assumption**: ASM-03

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. Профиль создается только при `GET /users/me`.** До этого
  `PUT /users/me` дает 404, а администратор не видит пользователя, который не
  открывал профиль. Отдельного шага регистрации нет: `AuthController`
  закомментирован, события Keycloak не используются.
- **OBS-02. Неполные claims ломают создание профиля.** Если в JWT нет
  `given_name`, `family_name` или `email` (возможно при входе через google
  или yandex), создание профиля падает на ограничении NOT NULL (5xx).
- **OBS-03. Email не синхронизируется.** Уникальность email обеспечивается
  только ограничением БД, поэтому смена на занятый email дает 500. Смена email
  не передается в Keycloak: email в профиле и в JWT расходятся.
- **OBS-04. Удаление только локальное.** Удаление профиля не трогает учетную
  запись Keycloak и вишлисты. Вишлисты привязаны к keycloakUserId и снова
  становятся доступны после повторного создания профиля.
- **OBS-05. Повторное создание дает новые события.** После удаления следующий
  `GET /users/me` создает профиль с новым внутренним id и новое USER_CREATED.
  notification-service отправит повторное приветственное письмо, а привязка
  Telegram к старому userId теряется.
- **OBS-06. Issuer не проверяется.** В отличие от catalog-service и
  seller-service, у которых задан `issuer-uri`.
- **OBS-07. roles только в /users/me.** Поле roles заполняется только там и
  берется из JWT, а не из Keycloak. В ответах администратора roles = null.
- **OBS-08. Изменение через параметры формы.** Изменение принимается не как
  JSON (`@ModelAttribute`), и поле нельзя очистить, передав null.
- **OBS-09. Телефон не уникален.** Уникальность телефона не проверяется, хотя
  сообщение `USER_PHONE_ALREADY_EXIST` объявлено.
- **OBS-10. Мертвый код и конфигурация.** Не используются `UserRequest` (с
  password), `AddressUpdateRequest`, `MessageProducer`, правило
  `/auth/**` permitAll без эндпоинтов и настройки
  `spring.security.oauth2.client` (стартера OAuth2 client в зависимостях нет).
- **OBS-11. 204 без тела.** DELETE отвечает 204, поэтому объявленное тело
  `{message, time}` клиенту не передается.
- **OBS-12. Тесты отключены.** Все тесты user-service закомментированы.
- **OBS-13. Формат событий отличается от seller-events-topic.** Сообщение в
  `user-created-events-topic` не обернуто в конверт, в отличие от
  `seller-events-topic`. Обработанные записи outbox не удаляются.
- **OBS-14. Администратор не ограничен.** Администратор может изменить или
  удалить любой профиль, включая свой, без синхронизации с Keycloak.
- **OBS-15. Ключ события — внутренний id.** Ключ и userId в событии — это
  внутренний UUID профиля, а не keycloakUserId. notification-service использует
  его как идентификатор для привязки Telegram.

## Assumptions (unverified)
- **ASM-01. Гонка при создании профиля.** Два параллельных первых
  `GET /users/me` одного пользователя, вероятно, приводят к нарушению
  уникальности `keycloak_user_id`, и один из запросов дает 500.
- **ASM-02. Даты адреса.** Поля `createdAt` и `updatedAt` во встраиваемом
  `Address` (`@CreatedDate`, `@LastModifiedDate`), вероятно, не заполняются
  аудитом.
- **ASM-03. Ошибки БД.** Они дают 500 со стандартным телом Spring Boot.
- **ASM-04. At-least-once.** Доставка at-least-once: при сбое после
  подтверждения Kafka возможна повторная публикация.

## Open Questions
- **OQ-01. OBS-01–OBS-15.** Какие из них дефекты?
- **OQ-02. Когда создавать профиль.** При регистрации (событие Keycloak), при
  первом входе или при первом `GET /users/me`?
- **OQ-03. Глубина удаления.** Должно ли удаление профиля удалять или
  деактивировать учетную запись Keycloak, вишлисты и данные продавца?
- **OQ-04. Источник правды для профиля.** Должны ли имя и email
  синхронизироваться с Keycloak? Какой источник считать главным?
