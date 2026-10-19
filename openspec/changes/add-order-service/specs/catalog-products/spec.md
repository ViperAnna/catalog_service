## MODIFIED Requirements

### Requirement: Product response format
Ответ чтения товара SHALL содержать поля id, storeId, name, description,
brand, price, articleNumber, imagesUrl (публичные URL изображений в порядке
хранения), categories (список `{id, name}`), characteristic (`{attributes}`),
tag и status (имя значения). Даты создания и изменения в ответ SHALL NOT
входить. storeId SHALL возвращаться всеми эндпоинтами чтения товара, включая
`POST /products/by-ids`.

#### Scenario: Формат товара
- **WHEN** запрашивается товар с одной категорией
- **THEN** categories содержит объект с id и именем этой категории, а id товара заполнен

#### Scenario: Магазин товара
- **WHEN** запрашивается по `POST /products/by-ids` доступный товар магазина 3
- **THEN** у товара в ответе storeId = 3

#### Scenario: Совместимость вишлистов
- **WHEN** user-service получает товары с полем storeId
- **THEN** ответ вишлиста по-прежнему содержит только id, name, price, articleNumber и imagesUrl

**Observation**: OBS-05, OBS-07
