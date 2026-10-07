# api-routing Specification

## Purpose
Baseline (as-is) поведения входной маршрутизации и обнаружения сервисов:
проксирование nginx фронтенда, статические маршруты api-gateway к сервисам
через Eureka (`lb://`), CORS на gateway, отключенный discovery locator, сервер
discovery-service и регистрация сервисов в Eureka. Спецификация описывает
фактическое текущее поведение кода и конфигурации, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Для dev учитывается
  рабочая копия `docker-compose.yml`. Код api-gateway и discovery-service в
  рабочей копии не изменен.
- Метод: статический анализ `api-gateway/src/main/resources/application.yaml`,
  `discovery-service/src/main/resources/application.yaml`, `.env.gateway`,
  compose-файлов и nginx-конфигураций. На dev-стенде проверены реестр Eureka,
  ответы gateway для маршрутизируемых и немаршрутизируемых путей, CORS preflight
  и проксирование nginx `/api/`. Тестов нет.
- Границы spec: правила аутентификации и авторизации на сервисах описываются в
  `access-control`, контракты Kafka — в `integration-events`. Прямой gRPC-вызов
  seller-service -> user-service идет мимо Eureka и описан в
  `seller-onboarding`.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код и конфигурация
  сейчас. Изменять его нужно отдельным change.

## Requirements

### Requirement: Edge proxy via frontend nginx
nginx фронтенда SHALL проксировать `/api/<path>` на `http://api-gateway:8080/<path>`
с отбрасыванием префикса `/api` и передачей заголовков Host, X-Real-IP,
X-Forwarded-For и X-Forwarded-Proto, а `/minio/<path>` — на `http://minio:9000/<path>`.
Остальные пути SHALL отдаваться как SPA с fallback на `index.html`. В prod nginx
SHALL дополнительно проксировать `/auth/` на Keycloak и обслуживать HTTPS.

#### Scenario: Запрос через nginx
- **WHEN** браузер вызывает `GET http://localhost/api/categories`
- **THEN** запрос уходит в api-gateway как `GET /categories`, ответ 200

#### Scenario: Маршрут SPA
- **WHEN** браузер открывает `/products/123`
- **THEN** nginx отдает `index.html`

**Observation**: OBS-08

### Requirement: Static service routes
api-gateway SHALL маршрутизировать запросы по статическим маршрутам на
логические имена сервисов: `/users/**`, `/admin/users/**`, `/wishlists/**` на
`lb://USER-SERVICE`; `/categories/**`, `/products/**` на `lb://CATALOG-SERVICE`;
`/sellers/**`, `/seller-applications/**`, `/moderator/seller-applications/**`,
`/stores/**` на `lb://SELLER-SERVICE`; `/notifications/**` на
`lb://NOTIFICATION-SERVICE`.

#### Scenario: Маршрут каталога
- **WHEN** клиент вызывает `GET /categories` на api-gateway
- **THEN** запрос передается экземпляру CATALOG-SERVICE

#### Scenario: Маршрут продавца
- **WHEN** клиент вызывает `GET /stores` на api-gateway
- **THEN** запрос передается экземпляру SELLER-SERVICE

### Requirement: Request forwarding without transformation
api-gateway SHALL передавать запрос в сервис с тем же путем, query-строкой,
методом, телом и заголовками, включая `Authorization`. Фильтры переписывания
пути, аутентификации, ограничения частоты, повторов и таймаутов на gateway
SHALL NOT быть настроены.

#### Scenario: Передача токена
- **WHEN** клиент вызывает `GET /users/me` с заголовком `Authorization: Bearer <jwt>`
- **THEN** user-service получает тот же путь `/users/me` и тот же заголовок

#### Scenario: Запрос без токена
- **WHEN** клиент без токена вызывает `GET /stores`
- **THEN** gateway передает запрос, и 401 возвращает seller-service

**Observation**: OBS-03

### Requirement: Unmatched paths
Запрос к пути, не подходящему ни под один маршрут, api-gateway SHALL отклонять
с кодом 404 без обращения к сервисам. Служебные пути сервисов
(`/actuator/**`, `/v3/api-docs`, `/swagger-ui/**`) SHALL NOT быть доступны через
gateway.

#### Scenario: Неизвестный путь
- **WHEN** клиент вызывает `GET /unknown/path` на api-gateway
- **THEN** ответ 404

#### Scenario: Документация API
- **WHEN** клиент вызывает `GET /v3/api-docs` на api-gateway
- **THEN** ответ 404

**Observation**: OBS-06

### Requirement: Discovery locator disabled
Автоматические маршруты по идентификаторам сервисов из Eureka SHALL быть
выключены (`discovery.locator.enabled` = `GATEWAY_DISCOVERY_LOCATOR_ENABLED`,
в dev `false`). Параметр `lower-case-service-id: true` SHALL быть задан, но при
выключенном locator SHALL NOT влиять на маршрутизацию.

#### Scenario: Путь с именем сервиса
- **WHEN** клиент вызывает `GET /user-service/users/me` или `GET /USER-SERVICE/users/me`
- **THEN** ответ 404

**Observation**: OBS-05

### Requirement: Load-balanced resolution through Eureka
Для маршрута `lb://<NAME>` api-gateway SHALL выбирать экземпляр сервиса из
реестра Eureka по имени NAME. Если в реестре нет экземпляров с этим именем,
gateway SHALL отвечать 503.

#### Scenario: Сервис зарегистрирован
- **WHEN** CATALOG-SERVICE зарегистрирован в Eureka со статусом UP
- **THEN** запросы `/products/**` доставляются его экземпляру

#### Scenario: Сервис не зарегистрирован
- **WHEN** в Eureka нет экземпляров NOTIFICATION-SERVICE
- **THEN** запрос `/notifications/x` через gateway получает 503

### Requirement: Notifications route
Маршрут `/notifications/**` на `lb://NOTIFICATION-SERVICE` SHALL оставаться в
конфигурации gateway, хотя у notification-service нет HTTP-обработчиков
(`notifications`). Запрос по этому маршруту SHALL NOT выполнять никакой
бизнес-логики: при отсутствии экземпляра в Eureka gateway отвечает 503.

#### Scenario: Текущее состояние dev-стенда
- **WHEN** notification-service не зарегистрирован в Eureka и клиент вызывает `GET /notifications/x`
- **THEN** gateway отвечает 503

**Observation**: OBS-01
**Assumption**: ASM-01

### Requirement: CORS at gateway
api-gateway SHALL применять глобальную CORS-политику ко всем путям `/**` с
allowedOrigins, allowedMethods, allowedHeaders, allowCredentials и maxAge из
`CORS_ALLOWED_ORIGINS`, `CORS_ALLOWED_METHODS`, `CORS_ALLOWED_HEADERS`,
`CORS_ALLOW_CREDENTIALS` и `CORS_MAX_AGE`. Preflight с разрешенным origin SHALL
получать 200 с заголовками CORS, с неразрешенным — 403. Сервисы SHALL NOT
настраивать CORS сами.

#### Scenario: Preflight с разрешенного origin в dev
- **WHEN** приходит `OPTIONS /categories` с `Origin: http://localhost:3000` и методом PUT
- **THEN** ответ 200 с `Access-Control-Allow-Origin: http://localhost:3000`, `Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS`, `Access-Control-Allow-Credentials: true`, `Access-Control-Max-Age: 3600`

#### Scenario: Неразрешенный origin
- **WHEN** приходит preflight с `Origin: http://evil.example`
- **THEN** ответ 403

**Observation**: OBS-04, OBS-08

### Requirement: Discovery server
discovery-service SHALL работать как одиночный Eureka-сервер на порту 8761, не
регистрируясь сам и не загружая реестр (`register-with-eureka: false`,
`fetch-registry: false`). Сервисы с Eureka-клиентом SHALL стартовать после
того, как discovery-service проходит healthcheck (`GET http://localhost:8761`).

#### Scenario: Порядок старта
- **WHEN** выполняется `docker compose up`
- **THEN** api-gateway, catalog-service, user-service, seller-service и notification-service ждут healthy discovery-service

**Observation**: OBS-07

### Requirement: Service registration in Eureka
Сервисы SHALL регистрироваться в `http://discovery-service:8761/eureka/` под
именами и портами: API-GATEWAY:8080, CATALOG-SERVICE:8080, USER-SERVICE:8080,
SELLER-SERVICE:8085, NOTIFICATION-SERVICE:8082. Имя SHALL браться из
`spring.application.name` (для catalog, user, seller — из
`SPRING_APPLICATION_NAME`) и приводиться Eureka к верхнему регистру.

#### Scenario: Реестр dev-стенда
- **WHEN** запрашивается реестр `GET /eureka/apps`
- **THEN** он содержит API-GATEWAY, CATALOG-SERVICE, USER-SERVICE и SELLER-SERVICE со статусом UP и указанными портами

#### Scenario: Имя каталога без переменной окружения
- **WHEN** catalog-service запущен без `SPRING_APPLICATION_NAME`
- **THEN** он регистрируется как CATALOG_SERVICE, а маршрут `lb://CATALOG-SERVICE` не находит экземпляров

**Observation**: OBS-09, OBS-10

## Observations (non-normative)
Поведение подтверждено кодом, конфигурацией или наблюдением на стенде и может
быть дефектом или архитектурным расхождением. Изменять его нужно отдельным
change после решения.

- **OBS-01. Маршрут `/notifications/**` мертвый.** У notification-service нет
  HTTP-обработчиков (`notifications` OBS-01). На dev-стенде он сейчас еще и
  не зарегистрирован в Eureka (`notifications` OBS-08), поэтому маршрут
  отвечает 503.
- **OBS-02. Расхождение версий Spring.** api-gateway собран на Spring Boot
  4.1.0 и Spring Cloud 2025.1.2 (`spring-cloud-starter-gateway-server-webflux`,
  ключи `spring.cloud.gateway.server.webflux.*`), а discovery-service,
  catalog-service, user-service, seller-service и notification-service — на
  Spring Boot 3.3.4 и Spring Cloud 2023.0.3. Это архитектурное расхождение
  стека, а не часть поведения маршрутизации.
- **OBS-03. Gateway — чистый прокси.** На нем нет аутентификации, ограничения
  частоты, повторов, circuit breaker и явных таймаутов. Вся защита выполняется
  в сервисах (`access-control`).
- **OBS-04. Неиспользуемые CORS-переменные.** CORS настроен только на gateway.
  `.env.catalog` содержит переменные `CORS_*`, но catalog-service их не
  использует: CORS-конфигурации в коде сервисов нет. В dev разрешен только
  origin `http://localhost:3000`.
- **OBS-05. lower-case-service-id без эффекта.** Параметр задан, но не влияет
  на маршрутизацию, пока discovery locator выключен.
- **OBS-06. Документация API недоступна через gateway.** Сервисы подключают
  springdoc, но документация и actuator через gateway недоступны.
- **OBS-07. Eureka без защиты.** Eureka одноузловая, без аутентификации
  дашборда и API. В dev порт 8761 опубликован на всех интерфейсах хоста, в prod
  — только на 127.0.0.1.
- **OBS-08. Разные пути фронтенда в dev и prod.** В dev-режиме `java`
  фронтенд обращается к gateway напрямую (`VITE_API_URL=http://localhost:8080`)
  и поэтому зависит от CORS. Через nginx (`/api/`) запросы идут с того же
  origin. Локальная nginx-конфигурация не содержит `/auth/`, в отличие от prod.
- **OBS-09. Имя catalog-service зависит от env.** В `application.yaml`
  catalog-service указано `spring.application.name: catalog_service`. Без
  переопределения из `.env.catalog` маршруты на `lb://CATALOG-SERVICE` и вызовы
  из user-service не работали бы.
- **OBS-10. Eureka URL из файла с именем kafka.** Адрес Eureka для части
  сервисов (включая api-gateway) берется из `.env.kafka`
  (`EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`), а не из файла с соответствующим
  назначением. Для notification-service см. `notifications` OBS-09.

## Assumptions (unverified)
- **ASM-01. Ответ при зарегистрированном notification-service.** Когда
  notification-service зарегистрирован и работает, запрос `/notifications/**`,
  вероятно, получает 404 от самого сервиса. Это состояние на стенде не
  наблюдалось.
- **ASM-02. CORS в prod.** Значения `CORS_*` в prod неизвестны: env-файлы
  передаются через Jenkins Credentials.
- **ASM-03. Нестабильные экземпляры.** Режим self-preservation Eureka включен по
  умолчанию, поэтому упавшие экземпляры могут оставаться в реестре, и
  gateway будет пытаться их вызывать.

## Open Questions
- **OQ-01. OBS-01–OBS-10.** Какие из них дефекты, а какие — осознанные решения?
- **OQ-02. Маршрут `/notifications/**`.** Удалить его или он зарезервирован под
  будущий API?
- **OQ-03. Выравнивание версий Spring.** Нужно ли выравнивать версии Spring
  Boot и Spring Cloud между gateway и остальными сервисами, и в какую сторону?
- **OQ-04. Сквозные функции на gateway.** Нужны ли на gateway проверка JWT,
  ограничение частоты или таймауты, или все остается на сервисах?
