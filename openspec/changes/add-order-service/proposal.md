# Proposal

## Why

В маркетплейсе нет покупки: покупатель видит товары и вишлисты, но не может
собрать корзину и оформить заказ. Нужен отдельный сервис `order-service`,
который владеет корзиной и заказами, не доверяет ценам клиента и встраивается в
существующую архитектуру (Eureka, gateway, Keycloak, PostgreSQL, Kafka outbox)
без параллельных механизмов.

Решения пользователя (согласованы в explore-сессии, две итерации):
- U1. Маршруты в стиле проекта, без `/api/v1`: `/cart`, `/cart/items`,
  `/cart/items/{productId}`, `/orders`, `/orders/{orderId}`,
  `/orders/{orderId}/cancel`. Изменение количества — `PUT`, CORS не меняется.
- U2. Один заказ может содержать товары разных магазинов, подзаказов нет.
  Позиция заказа — снимок данных товара на момент оформления. FK на сущности
  других сервисов нет. В MVP заказ отменяется целиком; доставка по магазинам
  в будущем может потребовать подзаказов.
- U3. order-service — единственный владелец состояния заказа и единственная
  точка изменения его статуса. В MVP статусы `CREATED` и `CANCELLED`;
  расширение для оплаты и доставки — через события или команды, без прямого
  изменения БД заказа другими сервисами.
- U4. События `ORDER_CREATED` и `ORDER_CANCELLED` публикуются через
  transactional outbox; у каждого события стабильный `eventId`;
  notification-service обрабатывает их идемпотентно и не теряет молча.
  `email` покупателя в payload разрешен как минимально необходимые
  персональные данные с правилами логирования и хранения.
- U5. MVP сервер сам получает актуальные цены и считает сумму; подтверждения
  суммы клиентом нет (`expectedTotal` удален). Подтверждение изменения цены —
  отдельное бизнес-требование.
- U6. Защита оформления в MVP — блокировка корзины в транзакции. Контракт
  `Idempotency-Key` и ограничения БД согласованы сейчас (design.md D12),
  реализация — change `order-checkout-idempotency`.
- U7. Тестовые классы в этом change не создаются, `skipTests=true` сохраняется,
  настройки тестов существующих сервисов не меняются. Сборка без запуска
  тестов не считается проверкой корректности. Тестирование — change
  `add-order-service-tests`.
- U8. Ручные операции на стендах выполняются только по запросу пользователя.

## What Changes

- Новый Maven-модуль `order-service` (Spring Boot 3.3.4, Spring Cloud 2023.0.3,
  Java 17): корзина, оформление и отмена заказа, история заказов покупателя.
- Собственная БД PostgreSQL `order_service`: `carts`, `cart_items`, `orders`,
  `order_items`, `outbox_events`; схема по принятой стратегии
  `ddl-auto: update` + идемпотентный `schema.sql` (без Flyway/Liquibase).
- Данные товара (цена, название, артикул, доступность, магазин) берутся только
  из catalog-service через `POST /products/by-ids` с JWT пользователя. Цена и
  сумма от клиента не принимаются.
- catalog-service: в ответ товара добавляется `storeId` (аддитивно;
  существующие потребители лишнее поле игнорируют).
- Новый топик `order-events-topic` (`ORDER_CREATED`, `ORDER_CANCELLED`) в
  конверте `{aggregateType, eventType, payload}` с `eventId` в payload.
- notification-service: потребитель `order-events-topic` с классификацией
  событий, идемпотентностью по `eventId` (таблица обработанных событий),
  явным DLT `order-events-topic.DLT`; два письма покупателю.
- api-gateway: маршрут `/cart`, `/cart/**`, `/orders`, `/orders/**` →
  `lb://ORDER-SERVICE`.
- Инфраструктура: корневой `pom.xml`, `postgres/init.sql`, `.env.order`,
  `docker-compose.yml`, `docker-compose.prod.yml`, `Jenkinsfile`.
- Не входят: оплата, возвраты, доставка, остатки и резервирование (в каталоге
  остатков нет), служебные переходы статусов, реализация `Idempotency-Key`,
  тесты, изменения frontend.

## Capabilities

### New Capabilities
- `shopping-cart`: корзина покупателя — состав, количество, серверный расчет
  стоимости по актуальным данным каталога, поведение для недоступных товаров.
- `orders`: оформление заказа из корзины, снимки позиций, история и просмотр
  заказов, жизненный цикл статусов и правило владения статусом, события заказа.

### Modified Capabilities
- `api-routing`: маршрут order-service на gateway, регистрация ORDER-SERVICE,
  порядок старта.
- `access-control`: order-service как resource server (роли, проверка JWT,
  deny-all, владение по `sub`), передача JWT order → catalog, общая учетная
  запись PostgreSQL.
- `catalog-products`: `storeId` в ответе товара.
- `integration-events`: топик `order-events-topic`, его контракт, `eventId`,
  персональные данные, outbox order-service, политика DLT потребителя.
- `notifications`: письма о заказах, классификация событий заказа,
  идемпотентная обработка, логирование без персональных данных.

## Impact

- Код: новый модуль `order-service/`; `catalog-service` (`ProductResponse`,
  маппер); `notification-service` (события заказа, consumer, таблица
  обработанных событий, письма, DLT-топик); `api-gateway` (`application.yaml`).
- Инфраструктура: новая БД `order_service` — на существующих томах
  `pgdata` создается вручную по запросу пользователя (`init.sql` выполняется
  только при пустом томе); новый env-файл и сервис в compose; Jenkins.
- Публичные контракты: новые `/cart/**`, `/orders/**`; аддитивное поле
  `storeId` в ответе товара. Ломающих изменений нет.
- Зависимости: новых библиотек сверх используемых в проекте нет.
- Проверка в этом change — компиляция, сборка и ревью соответствия specs.
  Корректность поведения не считается подтвержденной до выполнения
  `add-order-service-tests`.

## Follow-up changes

- `add-order-service-tests` — создан как отдельный change: тесты
  order-service и notification-service (заказные события), решение по Maven/CI.
- `order-checkout-idempotency` — реализация `Idempotency-Key` по контракту
  design.md D12; change создается при старте этого этапа.
