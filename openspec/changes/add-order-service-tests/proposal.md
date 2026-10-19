# Proposal

## Why

Change `add-order-service` реализует order-service и обработку событий заказа
в notification-service без тестов (решение пользователя U7). Сборка с
`skipTests=true` подтверждает только компиляцию. Этот change подтверждает
корректность поведения по specs `shopping-cart`, `orders`,
`integration-events`, `notifications` и `access-control` тестами и определяет,
как запускать их в Maven и CI, не ломая текущую конфигурацию.

Зависит от `add-order-service`: начинается после его реализации. До старта
нужно подтвердить у пользователя решение по Maven/CI (design.md, T1–T3).

## What Changes

- Unit-тесты чистых классов order-service (расчет, доступность позиции,
  переходы статусов, построение и обезличивание payload) и классификатора
  событий notification-service.
- Тесты сервисного слоя order-service с заглушками каталога и репозиториев.
- Интеграционные тесты PostgreSQL (Testcontainers): ограничения и индексы
  `schema.sql`, блокировка корзины и параллельное оформление, optimistic lock
  отмены, outbox в одной транзакции с заказом, обезличивание.
- REST/security-тесты order-service: 401/403, deny-all, чужие данные → 404,
  валидация, формат ошибок, пагинация, 405.
- Тесты интеграции с catalog-service через HTTP-заглушку: недоступные товары,
  некорректная цена, 5xx и таймаут → 503.
- Тесты notification-service для `order-events-topic`: идемпотентность по
  `eventId`, классы событий, DLT-политика, отсутствие email, отсутствие PII в
  логах.
- Решение и реализация способа запуска тестов order-service в Maven и
  Jenkins без изменения `skipTests` и сборки других модулей.

## Capabilities

### New Capabilities
<!-- нет: change не меняет поведение системы -->

### Modified Capabilities
<!-- нет: requirements не меняются; .openspec.yaml содержит skip_specs: true -->

## Impact

- Код: `order-service/src/test/**`, `notification-service/src/test/**`
  (только новые тесты событий заказа), test-зависимости и, при выборе
  варианта T1, профиль Maven в `order-service/pom.xml`; при выборе варианта
  T2 — шаг Jenkins.
- Не меняются: `skipTests` и surefire существующих модулей, существующие
  тесты, production-код (кроме исправлений дефектов, найденных тестами, —
  они фиксируются отдельными задачами с согласованием).
- Окружение: интеграционным тестам нужен Docker (Testcontainers).
