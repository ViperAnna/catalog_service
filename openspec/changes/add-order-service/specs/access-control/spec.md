## MODIFIED Requirements

### Requirement: Role to authority conversion
catalog-service, seller-service, user-service и order-service SHALL
преобразовывать каждую роль из claim `realm_access.roles` в authority
`ROLE_<РОЛЬ В ВЕРХНЕМ РЕГИСТРЕ>` идентичными копиями конвертера
`KCRoleConverter`. Роли из `resource_access` SHALL игнорироваться. При
отсутствии или пустом `realm_access.roles` authorities SHALL быть пустыми.

#### Scenario: Роль в нижнем регистре
- **WHEN** в JWT `realm_access.roles` = ["seller"]
- **THEN** у запроса есть authority `ROLE_SELLER`

#### Scenario: Нет claim realm_access
- **WHEN** в JWT нет `realm_access`
- **THEN** у запроса нет ролей, и защищенные пути отвечают 403

#### Scenario: Роль USER в order-service
- **WHEN** в order-service приходит JWT с `realm_access.roles` = ["user"]
- **THEN** у запроса есть authority `ROLE_USER`, и `GET /cart` доступен

**Observation**: OBS-05

### Requirement: JWT validation per service
catalog-service, seller-service и order-service SHALL проверять подпись JWT по
JWK set `JWT_JWK_SET_URI` и SHALL требовать, чтобы claim `iss` совпадал с
`JWT_ISSUER_URI` (в dev `http://localhost:8181/realms/user-service-realm`).
user-service SHALL проверять подпись по `JWK_SET_URI` без проверки `iss`. Срок
действия SHALL проверяться, audience SHALL NOT проверяться.

#### Scenario: Токен с другим issuer
- **WHEN** в catalog-service приходит подписанный realm-ключом JWT с `iss` = `http://keycloak:8181/realms/user-service-realm`
- **THEN** catalog-service отвечает 401

#### Scenario: Тот же токен в user-service
- **WHEN** этот же JWT приходит в user-service
- **THEN** токен принимается

#### Scenario: Токен с другим issuer в order-service
- **WHEN** этот же JWT приходит в order-service
- **THEN** order-service отвечает 401

**Observation**: OBS-01, OBS-02
**Assumption**: ASM-01, ASM-03

### Requirement: Default access policy per service
Для путей, не перечисленных явно, catalog-service, seller-service и
order-service SHALL отклонять все запросы (deny all), а user-service SHALL
требовать только аутентификацию. user-service SHALL пропускать без
аутентификации любые `OPTIONS` и `/auth/**`. catalog-service, seller-service и
order-service SHALL пропускать `/actuator/health` и ERROR-dispatch.
order-service SHALL требовать роль USER для `/cart`, `/cart/**`, `/orders` и
`/orders/**`.

#### Scenario: Неописанный путь в catalog-service
- **WHEN** аутентифицированный пользователь вызывает `PATCH /products/1`
- **THEN** catalog-service отвечает 403

#### Scenario: Неописанный путь в user-service
- **WHEN** аутентифицированный пользователь вызывает несуществующий путь user-service
- **THEN** запрос проходит фильтр безопасности, и ответ определяется отсутствием обработчика

#### Scenario: Неописанный путь в order-service
- **WHEN** аутентифицированный пользователь вызывает `GET /admin/orders` в order-service
- **THEN** order-service отвечает 403

#### Scenario: Корзина без роли USER
- **WHEN** в order-service приходит JWT без роли USER и вызывается `GET /cart`
- **THEN** order-service отвечает 403

**Observation**: OBS-06, OBS-07
**Assumption**: ASM-02

### Requirement: Identity and ownership by subject
Идентичностью пользователя во всех сервисах SHALL служить claim `sub`
(keycloakUserId). Владение SHALL проверяться по нему: заявки, Seller и магазины в
seller-service, вишлисты и профиль в user-service, корзина и заказы в
order-service. В catalog-service владение товаром SHALL проверяться через
seller-service (`seller-stores`). Модераторские и административные эндпоинты
SHALL NOT проверять владение.

#### Scenario: Чужой магазин
- **WHEN** продавец обращается к магазину другого продавца
- **THEN** seller-service отвечает 404

#### Scenario: Чужой заказ
- **WHEN** покупатель запрашивает заказ другого покупателя по его UUID
- **THEN** order-service отвечает 404

**Observation**: OBS-12

### Requirement: Token propagation between services
При синхронных вызовах catalog-service -> seller-service (`GET /stores/{id}`),
user-service -> catalog-service (`GET /products/{id}`, `POST /products/by-ids`)
и order-service -> catalog-service (`POST /products/by-ids`) вызывающий сервис
SHALL передавать JWT исходного пользователя. Сервисных токенов (client
credentials) для этих вызовов SHALL NOT быть. gRPC-вызов seller-service ->
user-service SHALL выполняться без учетных данных.

#### Scenario: Проверка владения при создании товара
- **WHEN** продавец создает товар
- **THEN** catalog-service вызывает seller-service с JWT этого продавца

#### Scenario: Данные товаров при оформлении заказа
- **WHEN** покупатель оформляет заказ
- **THEN** order-service вызывает `POST /products/by-ids` catalog-service с JWT этого покупателя

**Observation**: OBS-08

### Requirement: Shared infrastructure credentials
user-service, seller-service, order-service, notification-service и Keycloak
SHALL подключаться к PostgreSQL под одной и той же учетной записью
`POSTGRES_USER`. catalog-service SHALL подключаться к MongoDB под
root-пользователем и к MinIO — с root-учетными данными (`MINIO_ROOT_USER`).

#### Scenario: Учетная запись БД
- **WHEN** сравниваются учетные данные PostgreSQL сервисов
- **THEN** user-service, seller-service, order-service, notification-service и Keycloak используют одну учетную запись

**Observation**: OBS-09
