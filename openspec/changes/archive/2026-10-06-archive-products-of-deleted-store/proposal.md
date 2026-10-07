# Proposal

## Why

Когда продавец удаляет магазин, его товары остаются в каталоге с прежним
статусом, а продавец больше не может ими управлять: любая операция проверяет
владение через `GET /stores/{id}` и получает 404 (`seller-stores` OBS-07,
`catalog-products` OBS-15). Магазины хранятся в seller-service (PostgreSQL),
товары — в catalog-service (MongoDB), и сейчас catalog-service об удалении
магазина никак не узнает.

Решение A1 из разбора OBS `seller-stores`: при удалении магазина его товары
переводятся в архив асинхронно, через Kafka.

## What Changes

- **seller-service:** удаление магазина (`DELETE /stores/{id}`) в той же
  транзакции записывает в outbox событие `STORE_DELETED`
  (aggregateType `STORE`, aggregateId = id магазина, payload
  `{storeId, sellerId}`).
- **seller-service:** `OutboxPublisher` маршрутизирует события по
  aggregateType: `SELLER_APPLICATION` — в `seller-events-topic` (без
  изменений), `STORE` — в новый топик `store-events-topic` в том же конверте
  `{aggregateType, eventType, payload}` с ключом id магазина. Топик
  объявляется бином `NewTopic` (3 партиции, replication factor 1).
- **catalog-service:** новый потребитель `store-events-topic` в consumer group
  `catalog-group`. На `STORE_DELETED` все товары магазина, кроме уже
  архивных, переводятся в статус `ARCHIVED`. Повторная доставка ничего не
  меняет. Неизвестные типы событий пропускаются с предупреждением в логе.
  Ошибки обработки повторяются 3 раза с паузой 1 с, затем сообщение уходит в
  `store-events-topic.DLT`.
- **catalog-service:** отсутствие Kafka при старте не должно ронять сервис.
  Потребитель стартует отложенно и повторяет попытки подключения, а REST API
  каталога работает независимо от Kafka.
- Не BREAKING: REST API не меняется, формат `seller-events-topic` не
  меняется, новый топик добавляется.

### Known consequence (вне scope)

- Публичная выдача каталога статус не фильтрует (`catalog-products` OBS-04),
  поэтому архивные товары удаленного магазина **остаются видны** в
  `GET /products`, поиске и `by-ids` (в том числе в вишлистах), но со статусом
  `ARCHIVED`. Скрытие архивных товаров из выдачи — отдельный change.

### Out of scope

- Физическое удаление товаров и удаление картинок из MinIO (`catalog-media`
  OBS-05, OBS-06).
- Каскад «продавец → магазины → товары» при одобрении удаления продавца
  (`seller-stores` OBS-08).
- Фильтрация выдачи по статусу (`catalog-products` OBS-04).
- Изменение формата событий других топиков и общий модуль контрактов
  (`integration-events` OBS-01, OBS-02).

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-stores`: меняется `Store deletion`, добавляется
  `Store deleted event`.
- `seller-onboarding`: меняется `Outbox publishing to seller-events-topic`
  (маршрутизация по aggregateType).
- `catalog-products`: добавляются `Archiving products of deleted store` и
  `Store events consumer error handling`.
- `integration-events`: меняются `Topic declarations`, `Event consumers` и
  `Services without event processing`, добавляется
  `store-events-topic message contract`.

## Impact

- **seller-service:** `StoreServiceImpl.deleteStore` (outbox), новые
  `StoreEventType` и `StoreDeletedEvent`, `OutboxPublisher` (маршрутизация),
  `KafkaEvent.eventType` (тип `String`, формат на проводе не меняется),
  `KafkaConfig` (новый `NewTopic`).
- **catalog-service:** впервые использует Kafka: потребитель, конфигурация
  обработки ошибок, отложенный старт, массовое обновление статуса товаров в
  MongoDB.
- **Kafka:** новый топик `store-events-topic` и (при ошибках)
  `store-events-topic.DLT`.
- **notification-service, user-service, frontend, Keycloak, схема PostgreSQL:**
  без изменений.
- **Тесты:** unit-тесты в seller-service и catalog-service. Сквозная проверка —
  отдельная ручная задача: нужны работающая Kafka на стенде и токен продавца.
