## MODIFIED Requirements

### Requirement: Wishlist retrieval with products
user-service SHALL возвращать по `GET /wishlists/{id}` вишлист в виде
`{id, name, products, createdAt, updatedAt}`. products SHALL собираться одним
запросом `POST /products/by-ids` к catalog-service из сохраненных id. Товары,
которых catalog-service не вернул (удаленные или недоступные), SHALL NOT
попадать в products и SHALL оставаться в вишлисте. Порядок products SHALL NOT
быть определен.

#### Scenario: Пустой вишлист
- **WHEN** в вишлисте нет товаров
- **THEN** products = [] и catalog-service не вызывается

#### Scenario: Товар удален из каталога
- **WHEN** один из сохраненных товаров удален в catalog-service, в том числе при окончательном удалении магазина или продавца
- **THEN** products не содержит его, а сохраненный id остается в вишлисте

#### Scenario: Товар недоступен
- **WHEN** магазин сохраненного товара заблокирован
- **THEN** products не содержит товар; после разблокировки он снова отображается

**Observation**: OBS-05

### Requirement: Adding product to wishlist
`POST /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе 404),
затем запросить `GET /products/{productId}` у catalog-service. Ответ 4xx SHALL
давать 404, ответ 5xx — 503. Если товар уже есть в вишлисте, SHALL
возвращаться 409. Иначе id SHALL добавляться, и ответ SHALL содержать вишлист с
products.

#### Scenario: Успешное добавление
- **WHEN** пользователь добавляет существующий доступный товар
- **THEN** ответ 200 содержит вишлист с этим товаром в products

#### Scenario: Товар не найден
- **WHEN** catalog-service отвечает 404
- **THEN** user-service отвечает 404

#### Scenario: Повторное добавление
- **WHEN** товар уже есть в вишлисте
- **THEN** ответ 409 с message "Product with id [<productId>] already exists in wishlist."

#### Scenario: Товар в статусе DRAFT
- **WHEN** покупатель добавляет товар в статусе DRAFT или товар заблокированного магазина или продавца
- **THEN** catalog-service отвечает 404, и user-service отвечает 404, товар не добавляется

**Observation**: OBS-03
**Assumption**: ASM-01, ASM-02
