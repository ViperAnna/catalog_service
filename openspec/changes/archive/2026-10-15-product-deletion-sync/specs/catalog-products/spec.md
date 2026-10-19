## MODIFIED Requirements

### Requirement: Product deletion
catalog-service SHALL по `DELETE /products/{id}` возвращать 404 для
несуществующего товара, затем SHALL проверять владение магазином товара (иначе
404) и SHALL удалять товар с ответом 204. Изображения товара SHALL NOT
удаляться из хранилища. После удаления SHALL публиковаться событие
PRODUCT_DELETED по контракту `integration-events`
`product-events-topic message contract`.

#### Scenario: Удаление своего товара
- **WHEN** продавец удаляет товар своего магазина
- **THEN** товар удален, ответ 204

#### Scenario: Событие удаления
- **WHEN** продавец удаляет товар `p-1` магазина 3
- **THEN** в `product-events-topic` публикуется PRODUCT_DELETED с ключом `p-1`

#### Scenario: Товар не найден
- **WHEN** удаляется несуществующий товар
- **THEN** ответ 404, событие не публикуется

**Observation**: OBS-10, OBS-15

### Requirement: Purging products of deleted store
На событие STORE_PURGED catalog-service SHALL удалить неиспользуемые
изображения товаров магазина по правилам `catalog-media`, затем физически
удалить документы всех товаров с этим storeId и SHALL публиковать
PRODUCT_DELETED для каждого удаленного товара. Повторная обработка SHALL
доводить удаление до конца без ошибки; ошибка SHALL обрабатываться
повторами и DLT `store-events-topic`. События блокировки SHALL NOT удалять
товары.

#### Scenario: Удаление товаров магазина
- **WHEN** получено STORE_PURGED для магазина 3 с товарами в статусах ACTIVE и DRAFT
- **THEN** документов товаров магазина 3 в MongoDB нет, товары других магазинов не меняются

#### Scenario: Повтор после частичного сбоя
- **WHEN** первая обработка STORE_PURGED удалила часть изображений и упала
- **THEN** повторная обработка удаляет оставшиеся неиспользуемые изображения и все документы товаров магазина

#### Scenario: Блокировка не удаляет товары
- **WHEN** получено STORE_BLOCKED или SELLER_BLOCKED
- **THEN** документы товаров не удаляются

#### Scenario: События удаления товаров магазина
- **WHEN** STORE_PURGED удалил товары `p-1` и `p-2` магазина 3
- **THEN** в `product-events-topic` публикуются PRODUCT_DELETED для `p-1` и `p-2`

#### Scenario: Архивация не публикует событие
- **WHEN** получено устаревшее STORE_DELETED, и товары магазина переведены в ARCHIVED
- **THEN** PRODUCT_DELETED не публикуется
