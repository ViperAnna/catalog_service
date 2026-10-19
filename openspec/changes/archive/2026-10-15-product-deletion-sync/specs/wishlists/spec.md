## MODIFIED Requirements

### Requirement: Wishlist listing
user-service SHALL возвращать по `GET /wishlists` все вишлисты текущего
пользователя в виде `{id, name, productCount, createdAt, updatedAt}`, где
productCount — число идентификаторов товаров, сохраненных в вишлисте. Для
списка SHALL NOT вызываться catalog-service, порядок списка SHALL NOT быть
определен. productCount SHALL учитывать сохраненные id недоступных товаров и
SHALL NOT учитывать id товаров, удаленных из вишлиста по событию
PRODUCT_DELETED.

#### Scenario: Пустой список
- **WHEN** у пользователя нет вишлистов
- **THEN** ответ 200 с пустым списком

#### Scenario: Удаленный из каталога товар
- **WHEN** товар из вишлиста удален в catalog-service, и user-service обработал PRODUCT_DELETED
- **THEN** этот id не учитывается в productCount

#### Scenario: Недоступный товар
- **WHEN** товар из вишлиста недоступен (DRAFT, архив, блокировка магазина или продавца), но не удален
- **THEN** его id учитывается в productCount

### Requirement: Wishlist retrieval with products
user-service SHALL возвращать по `GET /wishlists/{id}` вишлист в виде
`{id, name, products, createdAt, updatedAt}`. products SHALL собираться одним
запросом `POST /products/by-ids` к catalog-service из сохраненных id и SHALL
содержать только товары, которые вернул catalog-service. Физически удаленный
товар SHALL NOT попадать в products, даже если его id остался в вишлисте.
Id удаленных товаров SHALL удаляться из вишлистов по событию PRODUCT_DELETED,
а не при чтении; id недоступных (архивных, заблокированных) товаров SHALL
оставаться в вишлисте. Порядок products SHALL NOT быть определен.

#### Scenario: Пустой вишлист
- **WHEN** в вишлисте нет товаров
- **THEN** products = [] и catalog-service не вызывается

#### Scenario: Товар удален из каталога
- **WHEN** один из сохраненных товаров удален в catalog-service, в том числе при окончательном удалении магазина или продавца
- **THEN** products не содержит его; после обработки PRODUCT_DELETED его id в вишлисте нет

#### Scenario: Товар недоступен
- **WHEN** магазин сохраненного товара заблокирован
- **THEN** products не содержит товар; после разблокировки он снова отображается

#### Scenario: Архивный товар
- **WHEN** товар из вишлиста переведен в ARCHIVED
- **THEN** products не содержит товар, его id остается в вишлисте

#### Scenario: Id удаленного товара остался в вишлисте
- **WHEN** id физически удаленного товара остался в вишлисте (добавление совпало с удалением или товар удален до появления события)
- **THEN** products не содержит этот товар, остальные товары возвращаются, чтение не удаляет id
