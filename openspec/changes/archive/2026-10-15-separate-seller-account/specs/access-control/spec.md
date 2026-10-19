## MODIFIED Requirements

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
