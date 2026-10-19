## MODIFIED Requirements

### Requirement: User profile API access
user-service SHALL разрешать `/users/me` только с JWT, содержащим realm-роль
USER, а `/admin/users/**` только с JWT, содержащим realm-роль ADMIN. JWT SHALL
проверяться по подписи через JWK set Keycloak без проверки issuer. Роли SHALL
извлекаться из claim `realm_access.roles`. Аккаунт продавца без роли USER
SHALL NOT иметь профиля покупателя.

#### Scenario: Нет роли ADMIN
- **WHEN** пользователь с ролью USER вызывает `GET /admin/users`
- **THEN** user-service отвечает 403

#### Scenario: Запрос без токена
- **WHEN** клиент без токена вызывает `GET /users/me`
- **THEN** user-service отвечает 401

#### Scenario: Аккаунт продавца
- **WHEN** аккаунт только с ролью SELLER вызывает `GET /users/me`
- **THEN** user-service отвечает 403, профиль не создается, USER_CREATED не публикуется

**Observation**: OBS-06
