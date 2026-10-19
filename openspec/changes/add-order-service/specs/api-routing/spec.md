## MODIFIED Requirements

### Requirement: Static service routes
api-gateway SHALL маршрутизировать запросы по статическим маршрутам на
логические имена сервисов: `/users/**`, `/admin/users/**`, `/wishlists/**` на
`lb://USER-SERVICE`; `/categories/**`, `/products/**` на `lb://CATALOG-SERVICE`;
`/sellers/**`, `/seller-applications/**`, `/moderator/seller-applications/**`,
`/moderator/sellers/**`, `/moderator/stores/**`,
`/moderator/store-change-requests/**`, `/stores/**` на `lb://SELLER-SERVICE`;
`/cart`, `/cart/**`, `/orders`, `/orders/**` на `lb://ORDER-SERVICE`;
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

#### Scenario: Маршрут корзины
- **WHEN** клиент вызывает `GET /cart` или `PUT /cart/items/abc` на api-gateway
- **THEN** запрос передается экземпляру ORDER-SERVICE с тем же путем

#### Scenario: Маршрут заказов
- **WHEN** клиент вызывает `POST /orders/{orderId}/cancel` на api-gateway
- **THEN** запрос передается экземпляру ORDER-SERVICE

#### Scenario: Префикс версии не поддерживается
- **WHEN** клиент вызывает `GET /api/v1/cart` на api-gateway
- **THEN** ответ 404

### Requirement: Discovery server
discovery-service SHALL работать как одиночный Eureka-сервер на порту 8761, не
регистрируясь сам и не загружая реестр (`register-with-eureka: false`,
`fetch-registry: false`). Сервисы с Eureka-клиентом SHALL стартовать после
того, как discovery-service проходит healthcheck (`GET http://localhost:8761`).

#### Scenario: Порядок старта
- **WHEN** выполняется `docker compose up`
- **THEN** api-gateway, catalog-service, user-service, seller-service, order-service и notification-service ждут healthy discovery-service

**Observation**: OBS-07

### Requirement: Service registration in Eureka
Сервисы SHALL регистрироваться в `http://discovery-service:8761/eureka/` под
именами и портами: API-GATEWAY:8080, CATALOG-SERVICE:8080, USER-SERVICE:8080,
SELLER-SERVICE:8085, ORDER-SERVICE:8086, NOTIFICATION-SERVICE:8082. Имя SHALL
браться из `spring.application.name` (для catalog, user, seller, order — из
`SPRING_APPLICATION_NAME`) и приводиться Eureka к верхнему регистру.

#### Scenario: Реестр dev-стенда
- **WHEN** запрашивается реестр `GET /eureka/apps`
- **THEN** он содержит API-GATEWAY, CATALOG-SERVICE, USER-SERVICE, SELLER-SERVICE и ORDER-SERVICE со статусом UP и указанными портами

#### Scenario: Имя каталога без переменной окружения
- **WHEN** catalog-service запущен без `SPRING_APPLICATION_NAME`
- **THEN** он регистрируется как CATALOG_SERVICE, а маршрут `lb://CATALOG-SERVICE` не находит экземпляров

**Observation**: OBS-09, OBS-10
