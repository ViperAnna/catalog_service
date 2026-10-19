## MODIFIED Requirements

### Requirement: Wishlist rename
user-service SHALL принимать `PUT /wishlists/{id}` с телом `{name}` с теми же
ограничениями, что при создании. Сначала SHALL проверяться существование и
владение (иначе 404), затем то, что у пользователя нет другого вишлиста с таким
именем (иначе 409). Ответ SHALL иметь код 200 и тело `{message, time}`.

#### Scenario: Переименование
- **WHEN** пользователь меняет имя на свободное
- **THEN** ответ 200 с message "The wishlist was successfully updated."

#### Scenario: То же имя
- **WHEN** пользователь отправляет текущее имя вишлиста
- **THEN** ответ 200

#### Scenario: Конфликт для несуществующего вишлиста
- **WHEN** id не существует или принадлежит другому пользователю, а имя совпадает с другим вишлистом текущего пользователя
- **THEN** ответ 404 с message "The wishlist with id [<id>] not found."

#### Scenario: Имя занято другим вишлистом
- **WHEN** пользователь переименовывает свой вишлист в имя другого своего вишлиста
- **THEN** ответ 409, имя не меняется

### Requirement: Adding product to wishlist
`POST /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе 404),
затем запросить `GET /products/{productId}` у catalog-service. Ответ 4xx SHALL
давать 404 с message "Product with id [<productId>] not found.", ответ 5xx —
503 с message "Catalog service is unavailable."; оба — с телом
`{message, time}`. Если товар уже есть в вишлисте, SHALL возвращаться 409.
Иначе id SHALL добавляться, и ответ SHALL содержать вишлист с products.

#### Scenario: Успешное добавление
- **WHEN** пользователь добавляет существующий доступный товар
- **THEN** ответ 200 содержит вишлист с этим товаром в products

#### Scenario: Товар не найден
- **WHEN** catalog-service отвечает 404 на товар `p-1`
- **THEN** user-service отвечает 404 с телом `{message, time}` и message "Product with id [p-1] not found."

#### Scenario: Повторное добавление
- **WHEN** товар уже есть в вишлисте
- **THEN** ответ 409 с message "Product with id [<productId>] already exists in wishlist."

#### Scenario: Товар в статусе DRAFT
- **WHEN** покупатель добавляет товар в статусе DRAFT или товар заблокированного магазина или продавца
- **THEN** catalog-service отвечает 404, и user-service отвечает 404, товар не добавляется

**Observation**: OBS-03
**Assumption**: ASM-01, ASM-02

### Requirement: Removing product from wishlist
`DELETE /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе
404) и удалить id из вишлиста без обращения к `GET /products/{id}`. Если id нет
в вишлисте, SHALL возвращаться 404 с message
"Product with id [<productId>] not found in wishlist.". Ответ SHALL содержать
вишлист с products.

#### Scenario: Удаление товара
- **WHEN** пользователь удаляет товар из вишлиста
- **THEN** ответ 200 содержит вишлист без этого товара

#### Scenario: Товара нет в вишлисте
- **WHEN** id товара `p-1` не сохранен в вишлисте
- **THEN** ответ 404 с message "Product with id [p-1] not found in wishlist.", вишлист не меняется

**Observation**: OBS-04

### Requirement: Wishlist error responses
Ошибки валидации (400), отсутствия вишлиста или товара (404), конфликтов (409)
и недоступности catalog-service (503) SHALL возвращаться с телом
`{message, time}`. Сообщения SHALL NOT содержать неподставленных шаблонов
вида `[%s]`.

#### Scenario: Каталог недоступен
- **WHEN** catalog-service отвечает 5xx при добавлении товара
- **THEN** ответ 503 с телом `{message, time}` и message "Catalog service is unavailable."

#### Scenario: Ошибка каталога при чтении вишлиста
- **WHEN** catalog-service отвечает 5xx на `POST /products/by-ids` при `GET /wishlists/{id}`
- **THEN** ответ 503 с телом `{message, time}`

**Observation**: OBS-03
**Assumption**: ASM-03
