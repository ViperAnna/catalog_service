# access-control Specification

## Purpose
Сводная baseline (as-is) модель безопасности системы: единый realm Keycloak и
его клиенты, realm-роли, преобразование ролей в authorities Spring Security,
проверка JWT в каждом сервисе, сводная матрица защищенных путей, проверки
владения ресурсами, передача токена между сервисами и компоненты, работающие
без аутентификации. Спецификация описывает фактическое текущее состояние и
сводит его из сервисных specs, не вводя новых требований.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Код безопасности в
  рабочей копии не изменен.
- Метод: статический анализ `SpringSecurityConfig` и `KCRoleConverter` в
  catalog-service, seller-service и user-service, аннотаций `@PreAuthorize`,
  `application.yaml` и env-файлов (значения секретов не читались, сравнивались
  только имена и совпадение учетных записей), `keycloak/realm-export.json`,
  кода keycloak-js во frontend и compose-файлов.
- Нормативные детали доступа к конкретным эндпоинтам описаны в сервисных
  specs: `catalog-categories`, `catalog-products`, `seller-onboarding`,
  `seller-stores`, `user-profile`, `wishlists`. Эта spec сводит их на уровне
  общей модели и ссылается на них. Маршрутизация описана в `api-routing`,
  контракты событий — в `integration-events`.
- Секция Requirements нормативна. Секции Access Matrix, Observations,
  Assumptions и Open Questions нормативными не являются. Изменять
  зафиксированное поведение нужно отдельным change.

## Requirements

### Requirement: Single Keycloak realm
Все сервисы и frontend SHALL использовать один realm Keycloak
`user-service-realm` (Keycloak 26.6.3, порт 8181). В realm SHALL быть открыта
самостоятельная регистрация с подтверждением email, вход по email, сброс
пароля, вход через внешние IdP `google` и `yandex`. Access token SHALL жить
300 секунд.

#### Scenario: Регистрация нового пользователя
- **WHEN** посетитель регистрируется на странице Keycloak
- **THEN** создается учетная запись realm `user-service-realm` с ролью USER по умолчанию

**Observation**: OBS-11

### Requirement: Realm roles
Модель доступа SHALL строиться на realm-ролях USER, SELLER, MODERATOR и ADMIN.
USER SHALL выдаваться каждому зарегистрировавшемуся пользователю через
составную роль `default-roles-user-service-realm`. Аккаунт продавца SHALL
создаваться user-service при одобрении регистрации и SHALL иметь только
realm-роль SELLER, без USER. Существующие продавцы на аккаунте покупателя SHALL
сохранять USER и SELLER; роль SELLER у них снимается только при удалении
продавца. MODERATOR и ADMIN SHALL назначаться вне приложения. Клиентские роли
SHALL NOT использоваться для доступа к API.

#### Scenario: Роль SELLER
- **WHEN** модератор одобряет заявку на регистрацию продавца
- **THEN** создается отдельный аккаунт продавца с realm-ролью SELLER через user-service, аккаунт заявителя сохраняет только свои роли

### Requirement: Role to authority conversion
catalog-service, seller-service и user-service SHALL преобразовывать каждую роль
из claim `realm_access.roles` в authority `ROLE_<РОЛЬ В ВЕРХНЕМ РЕГИСТРЕ>`
идентичными копиями конвертера `KCRoleConverter`. Роли из `resource_access`
SHALL игнорироваться. При отсутствии или пустом `realm_access.roles` authorities
SHALL быть пустыми.

#### Scenario: Роль в нижнем регистре
- **WHEN** в JWT `realm_access.roles` = ["seller"]
- **THEN** у запроса есть authority `ROLE_SELLER`

#### Scenario: Нет claim realm_access
- **WHEN** в JWT нет `realm_access`
- **THEN** у запроса нет ролей, и защищенные пути отвечают 403

**Observation**: OBS-05

### Requirement: JWT validation per service
catalog-service и seller-service SHALL проверять подпись JWT по JWK set
`JWT_JWK_SET_URI` и SHALL требовать, чтобы claim `iss` совпадал с
`JWT_ISSUER_URI` (в dev `http://localhost:8181/realms/user-service-realm`).
user-service SHALL проверять подпись по `JWK_SET_URI` без проверки `iss`. Срок
действия SHALL проверяться, audience SHALL NOT проверяться.

#### Scenario: Токен с другим issuer
- **WHEN** в catalog-service приходит подписанный realm-ключом JWT с `iss` = `http://keycloak:8181/realms/user-service-realm`
- **THEN** catalog-service отвечает 401

#### Scenario: Тот же токен в user-service
- **WHEN** этот же JWT приходит в user-service
- **THEN** токен принимается

**Observation**: OBS-01, OBS-02
**Assumption**: ASM-01, ASM-03

### Requirement: Stateless resource servers
Защищенные сервисы SHALL работать как stateless OAuth2 resource servers: без
HTTP-сессий, с отключенным CSRF и с аутентификацией только по заголовку
`Authorization: Bearer <JWT>`. Запрос без токена к защищенному пути SHALL
получать 401, а запрос с токеном без нужной роли — 403.

#### Scenario: Нет нужной роли
- **WHEN** пользователь с ролью USER вызывает `POST /categories`
- **THEN** catalog-service отвечает 403

### Requirement: Default access policy per service
Для путей, не перечисленных явно, catalog-service и seller-service SHALL
отклонять все запросы (deny all), а user-service SHALL требовать только
аутентификацию. user-service SHALL пропускать без аутентификации любые
`OPTIONS` и `/auth/**`. catalog-service и seller-service SHALL пропускать
`/actuator/health` и ERROR-dispatch.

#### Scenario: Неописанный путь в catalog-service
- **WHEN** аутентифицированный пользователь вызывает `PATCH /products/1`
- **THEN** catalog-service отвечает 403

#### Scenario: Неописанный путь в user-service
- **WHEN** аутентифицированный пользователь вызывает несуществующий путь user-service
- **THEN** запрос проходит фильтр безопасности, и ответ определяется отсутствием обработчика

**Observation**: OBS-06, OBS-07
**Assumption**: ASM-02

### Requirement: Identity and ownership by subject
Идентичностью пользователя во всех сервисах SHALL служить claim `sub`
(keycloakUserId). Владение SHALL проверяться по нему: заявки, Seller и магазины в
seller-service, вишлисты и профиль в user-service. В catalog-service владение
товаром SHALL проверяться через seller-service (`seller-stores`).
Модераторские и административные эндпоинты SHALL NOT проверять владение.

#### Scenario: Чужой магазин
- **WHEN** продавец обращается к магазину другого продавца
- **THEN** seller-service отвечает 404

**Observation**: OBS-12

### Requirement: Token propagation between services
При синхронных вызовах catalog-service -> seller-service (`GET /stores/{id}`) и
user-service -> catalog-service (`GET /products/{id}`, `POST /products/by-ids`)
вызывающий сервис SHALL передавать JWT исходного пользователя. Сервисных
токенов (client credentials) для этих вызовов SHALL NOT быть. gRPC-вызов
seller-service -> user-service SHALL выполняться без учетных данных.

#### Scenario: Проверка владения при создании товара
- **WHEN** продавец создает товар
- **THEN** catalog-service вызывает seller-service с JWT этого продавца

**Observation**: OBS-08

### Requirement: Keycloak clients in use
frontend SHALL входить через публичный клиент `frontend-client` по
Authorization Code с PKCE S256 (keycloak-js, `check-sso`) и SHALL обновлять
токен, если до истечения меньше 30 секунд, перед каждым запросом к API.
user-service SHALL вызывать Keycloak Admin API через конфиденциальный клиент
`user-service-admin` (client credentials, роли `realm-management`) — в том числе
для создания и удаления аккаунтов продавцов и отправки писем с действиями.

#### Scenario: Запрос frontend к API
- **WHEN** frontend выполняет запрос к API с действующей сессией
- **THEN** он подставляет актуальный access token клиента `frontend-client` в `Authorization`

#### Scenario: Создание аккаунта продавца
- **WHEN** user-service создает аккаунт продавца
- **THEN** вызов Admin API выполняется от имени `user-service-admin`

### Requirement: Keycloak clients not used by code
Клиенты `user-manage-client` (direct access grants и service account с
realm-ролями ADMIN и USER), `user-service-client` и `seller-service-client`
SHALL существовать в realm, но SHALL NOT использоваться кодом сервисов.

#### Scenario: Сервисный аккаунт user-manage-client
- **WHEN** запрашивается токен client credentials клиента `user-manage-client`
- **THEN** токен содержит realm-роли ADMIN и USER и принимается всеми сервисами

**Observation**: OBS-03, OBS-04

### Requirement: Components without authentication
api-gateway, discovery-service и notification-service SHALL NOT аутентифицировать
запросы. Kafka SHALL работать через PLAINTEXT без аутентификации и ACL,
gRPC-сервер user-service — без TLS и аутентификации, бакеты MinIO — с
анонимным чтением.

#### Scenario: Публикация в Kafka
- **WHEN** любой клиент сети docker публикует сообщение в `seller-events-topic`
- **THEN** брокер принимает его без аутентификации

**Observation**: OBS-08

### Requirement: Shared infrastructure credentials
user-service, seller-service, notification-service и Keycloak SHALL подключаться
к PostgreSQL под одной и той же учетной записью `POSTGRES_USER`.
catalog-service SHALL подключаться к MongoDB под root-пользователем и к MinIO —
с root-учетными данными (`MINIO_ROOT_USER`).

#### Scenario: Учетная запись БД
- **WHEN** сравниваются учетные данные PostgreSQL сервисов
- **THEN** user-service, seller-service, notification-service и Keycloak используют одну учетную запись

**Observation**: OBS-09

### Requirement: Role change propagation
Назначение и снятие роли SELLER SHALL отражаться в правах доступа только после
выпуска нового access token. Отзыв ролей и сессий при изменении ролей и
проверка статуса продавца при авторизации SHALL NOT выполняться.

#### Scenario: Снятие роли SELLER
- **WHEN** у пользователя снята роль SELLER, а его access token еще действует
- **THEN** запросы к `/stores/**` проходят до истечения токена

**Observation**: OBS-10

### Requirement: Frontend role checks
frontend SHALL показывать ссылки администратора и открывать маршрут
`/admin/users` только пользователям с realm-ролью ADMIN по
`keycloak.hasRealmRole`. Проверки frontend SHALL NOT заменять проверки в
сервисах. Если аутентификация выключена (mock-режим), ссылки администратора
SHALL показываться.

#### Scenario: Пользователь без ADMIN
- **WHEN** пользователь без роли ADMIN открывает `/admin/users`
- **THEN** frontend показывает сообщение, что страница доступна только роли ADMIN

## Access Matrix (informative)
Сводка из сервисных specs. Нормативные детали и коды ответов приведены в
указанных specs.

| Сервис | Путь и метод | Доступ | Источник |
|---|---|---|---|
| catalog | `GET /categories`, `GET /categories/{id}` | все | catalog-categories |
| catalog | `POST`, `PUT`, `DELETE /categories/**` | ADMIN | catalog-categories |
| catalog | `GET /products`, `/products/{id}`, `/products/productByName/{n}`, `POST /products/by-ids` | все | catalog-products |
| catalog | `POST /products`, `PUT`, `DELETE /products/{id}`, `GET /products/stores/{id}/products` | SELLER + владение магазином | catalog-products, seller-stores |
| catalog | прочее | запрещено | — |
| seller | `POST /seller-applications/registration` | USER | seller-onboarding |
| seller | `GET /seller-applications/me`, `/history` | USER или SELLER | seller-onboarding |
| seller | `PUT /update`, `DELETE /deletion`, `POST /unblock` (`/seller-applications`) | SELLER | seller-onboarding |
| seller | `/moderator/seller-applications/**` | MODERATOR | seller-onboarding |
| seller | `/sellers/**` | SELLER | seller-onboarding |
| seller | `/stores/**` | SELLER + владение | seller-stores |
| seller | прочее | запрещено | — |
| user | `/users/me` | USER | user-profile |
| user | `/admin/**` | ADMIN | user-profile |
| user | `/wishlists/**` | USER или ADMIN без SELLER + владение | wishlists |
| user | `OPTIONS /**`, `/auth/**` | все | — |
| user | прочее | любой аутентифицированный | — |
| user | gRPC `user.UserService/*` (:9090) | без аутентификации | seller-onboarding |
| notification, gateway, discovery | все | без аутентификации | notifications, api-routing |

## Observations (non-normative)
Подтверждено кодом и конфигурацией. Изменять это нужно отдельным change после
решения.

- **OBS-01. Проверка issuer различается по сервисам.** user-service `iss` не
  проверяет. catalog-service и seller-service жестко привязаны к issuer из env
  (в dev — адрес `localhost:8181`, через который получает токен браузер).
- **OBS-02. Audience не проверяется ни одним сервисом.** Любой токен realm,
  выданный любому клиенту, принимается всеми сервисами.
- **OBS-03. user-manage-client дает ADMIN по секрету.** Service account клиента
  `user-manage-client` имеет realm-роли ADMIN и USER, а у клиента включен
  direct access grants. Любой, кто знает секрет клиента (он есть в
  `.env.user`), может получить токен ADMIN. Код этот клиент не использует.
- **OBS-04. Неиспользуемые клиенты и конфигурация.** `user-service-client`
  (redirect `https://localhost:8181/redirect`) и `seller-service-client` (все
  flow выключены) не используются. Их секреты лежат в `.env.keycloak`. В
  user-service настроен неиспользуемый OAuth2 client (`user-profile` OBS-10). В
  seller-service `CurrentUserService.getRoles()` читает несуществующий claim
  `roles`.
- **OBS-05. Конвертер скопирован трижды.** `KCRoleConverter` одинаковый в трех
  сервисах. Клиентские роли из `resource_access` игнорируются.
- **OBS-06. Политика по умолчанию различается.** catalog-service и
  seller-service запрещают неописанные пути, а user-service пускает любой
  аутентифицированный запрос и открывает `/auth/**`, хотя эндпоинтов там нет.
- **OBS-07. Проверки ролей сделаны по-разному.** В seller-service и user-service
  роли проверяются и в фильтре, и через `@PreAuthorize`. В catalog-service —
  только в фильтре. В user-service нет правила для ERROR-dispatch.
- **OBS-08. Межсервисные каналы без аутентификации.**
  - gRPC позволяет любому участнику docker-сети назначить роль SELLER
    (`seller-onboarding` OQ-06).
  - Kafka без аутентификации позволяет публиковать поддельные события,
    например `SELLER_APPROVED`, на которые уходят письма.
  - Межсервисные HTTP-вызовы опираются только на токен пользователя.
  - Связанные проблемы описаны в `catalog-media` OBS-08 (анонимный листинг
    MinIO) и `api-routing` OBS-07 (Eureka без защиты).
- **OBS-09. Общие учетные записи инфраструктуры.** Сервисы используют общие
  или root-учетные записи PostgreSQL, MongoDB и MinIO. Изоляции между
  сервисами на уровне хранилищ нет.
- **OBS-10. Отзыв ролей не мгновенный.** Отзыв роли SELLER вступает в силу до
  300 секунд спустя. Статус продавца DELETED при авторизации не учитывается
  (`seller-stores` OBS-08).
- **OBS-11. Настройки realm.** Защита от перебора паролей
  (`bruteForceProtected`) выключена, `sslRequired` = `external`. IdP `google`
  доверяет email (`trustEmail: true`), `yandex` — нет. Профиль при входе через
  IdP может не создаться (`user-profile` OBS-02).
- **OBS-12. Разделения обязанностей нет.** Модератор может одобрить свою
  заявку (`seller-onboarding` OBS-10), администратор может изменить или удалить
  любой профиль, включая свой (`user-profile` OBS-14).

## Assumptions (unverified)
- **ASM-01. Issuer в prod.** В prod `JWT_ISSUER_URI` у catalog-service и
  seller-service, вероятно, задан как публичный адрес Keycloak за nginx
  (`/auth/`). Иначе эти сервисы отклоняли бы токены браузера.
- **ASM-02. Анонимные ошибки в user-service.** Без правила для ERROR-dispatch
  анонимный запрос к user-service, завершившийся ошибкой, вероятно, получает
  401 вместо исходного кода.
- **ASM-03. Issuer зависит от адреса Keycloak.** Keycloak работает с
  `KC_HOSTNAME_STRICT=false` без `KC_HOSTNAME`, поэтому `iss` в токене,
  вероятно, зависит от адреса, по которому токен получен. Токен, полученный
  через `keycloak:8181` внутри сети, будет отклонен catalog-service и
  seller-service.

## Open Questions
- **OQ-01. OBS-01–OBS-12.** Какие из них дефекты, а какие — осознанные решения?
- **OQ-02. Единая проверка JWT.** Нужна ли одинаковая проверка `iss` и
  `aud` во всех сервисах?
- **OQ-03. Неиспользуемые клиенты Keycloak.** Удалить `user-manage-client`,
  `user-service-client`, `seller-service-client` или забрать у
  `user-manage-client` роль ADMIN?
- **OQ-04. Межсервисная аутентификация.** Нужна ли аутентификация для gRPC и
  Kafka (mTLS, SASL, сервисные токены)?
- **OQ-05. Учетные записи хранилищ.** Нужны ли отдельные учетные записи БД и
  хранилищ для каждого сервиса?
