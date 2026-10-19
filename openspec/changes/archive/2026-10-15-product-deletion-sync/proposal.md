# Proposal

## Why

Товары, физически удаленные из каталога, навсегда остаются в вишлистах:
`productCount` в списке их учитывает, а карточка вишлиста не показывает
(spec `wishlists` OBS-05). Решения пользователя (2026-10-15): клиент не должен видеть физически
удаленные товары в вишлисте; их id удаляются из вишлистов событием, а не
очисткой при чтении; гарантировать отсутствие id в БД при любых гонках не
нужно (таблица удаленных товаров не вводится); архивные и заблокированные
товары не удаляются.

Проверено: подходящего события нет. catalog-service ничего не публикует в
Kafka; товар удаляется владельцем (`DELETE /products/{id}`) и массово при
`STORE_PURGED`, но ни один путь не сообщает id товара. `STORE_PURGED`
(seller-service) содержит только storeId, а вишлист хранит только id товаров.

## What Changes

- catalog-service публикует новое событие `PRODUCT_DELETED` в
  `product-events-topic` (ключ — productId) при каждом физическом удалении
  товара: удаление владельцем и `STORE_PURGED`. Архивация (устаревший
  `STORE_DELETED`), блокировки и смена статуса события не создают.
- Надежность без транзакций MongoDB: outbox-коллекция в catalog, запись до
  удаления, публикация только для фактически удаленного товара.
- user-service потребляет `product-events-topic` и удаляет productId из всех
  вишлистов. Обработка идемпотентна; ошибки — повторы и DLT, как у других
  потребителей.
- Карточка вишлиста показывает только товары, которые вернул каталог:
  оставшийся после редкой гонки id удаленного товара клиенту не виден (уже
  так работает — закрепляется требованием и тестом). Оставшиеся id
  удаляются ручной процедурой сверки с MongoDB по запросу пользователя.
- Не входят: таблица удаленных товаров, очистка при чтении, служебный API
  проверки существования товаров, другие потребители события.

## Capabilities

### New Capabilities
<!-- нет -->

### Modified Capabilities
- `catalog-products`: удаление товара владельцем и при `STORE_PURGED`
  публикует `PRODUCT_DELETED`.
- `integration-events`: новый топик и контракт, catalog-service — producer,
  user-service — consumer.
- `wishlists`: удаленные из каталога товары удаляются из вишлистов, список и
  карточка согласованы.

## Impact

- catalog-service: outbox-коллекция MongoDB, публикатор, `NewTopic`,
  `ProductServiceImpl.deleteProductById`, `StoreProductsPurger`; Kafka
  producer-настройки.
- user-service: Kafka consumer-настройки и listener, запрос удаления id из
  `wishlist_products`, обработчик ошибок с DLT.
- Конфликт архивации: change `add-order-service` тоже меняет в
  `integration-events` requirements `Topic declarations`, `Event consumers`,
  `Services without event processing` — второй архивируемый change сводит
  свои блоки с уже синхронизированными (design.md R3).
