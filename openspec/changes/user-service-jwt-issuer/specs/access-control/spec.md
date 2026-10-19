## MODIFIED Requirements

### Requirement: JWT validation per service
catalog-service, seller-service и user-service SHALL проверять подпись JWT по
JWK set (`JWT_JWK_SET_URI`, у user-service — `JWK_SET_URI`) и SHALL требовать,
чтобы claim `iss` совпадал с `JWT_ISSUER_URI` (в dev
`http://localhost:8181/realms/user-service-realm`). Срок действия SHALL
проверяться, audience SHALL NOT проверяться.

#### Scenario: Токен с другим issuer
- **WHEN** в catalog-service приходит подписанный realm-ключом JWT с `iss` = `http://keycloak:8181/realms/user-service-realm`
- **THEN** catalog-service отвечает 401

#### Scenario: Тот же токен в user-service
- **WHEN** этот же JWT приходит в user-service
- **THEN** user-service отвечает 401

#### Scenario: Токен с ожидаемым issuer в user-service
- **WHEN** в user-service приходит действующий JWT с `iss` = `JWT_ISSUER_URI` и ролью USER
- **THEN** запрос `GET /users/me` проходит проверку аутентификации

**Observation**: OBS-01, OBS-02
**Assumption**: ASM-01, ASM-03
