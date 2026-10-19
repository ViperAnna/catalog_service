## MODIFIED Requirements

### Requirement: Static service routes
api-gateway SHALL маршрутизировать запросы по статическим маршрутам на
логические имена сервисов: `/users/**`, `/admin/users/**`, `/wishlists/**` на
`lb://USER-SERVICE`; `/categories/**`, `/products/**` на `lb://CATALOG-SERVICE`;
`/sellers/**`, `/seller-applications/**`, `/moderator/seller-applications/**`,
`/moderator/sellers/**`, `/moderator/stores/**`,
`/moderator/store-change-requests/**`, `/stores/**` на `lb://SELLER-SERVICE`;
`/notifications/**` на `lb://NOTIFICATION-SERVICE`.

#### Scenario: Маршрут каталога
- **WHEN** клиент вызывает `GET /categories` на api-gateway
- **THEN** запрос передается экземпляру CATALOG-SERVICE

#### Scenario: Маршрут продавца
- **WHEN** клиент вызывает `GET /stores` на api-gateway
- **THEN** запрос передается экземпляру SELLER-SERVICE

#### Scenario: Маршрут блокировки
- **WHEN** модератор вызывает `POST /moderator/stores/3/block` на api-gateway
- **THEN** запрос передается экземпляру SELLER-SERVICE

#### Scenario: Маршрут модерации магазинов
- **WHEN** модератор вызывает `GET /moderator/store-change-requests/pending` на api-gateway
- **THEN** запрос передается экземпляру SELLER-SERVICE
