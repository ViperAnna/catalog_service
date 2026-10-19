## MODIFIED Requirements

### Requirement: Adding product to wishlist
`POST /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе 404),
затем запросить `GET /products/{productId}` у catalog-service. Ответ 4xx SHALL
давать 404 с message "Product with id [<productId>] not found.", ответ 5xx,
отсутствие ответа за 5 секунд, отказ соединения (дольше 2 секунд или
отклонено) и другая сетевая ошибка — 503 с message "Catalog service is
unavailable."; оба — с телом `{message, time}`. Если товар уже есть в
вишлисте, SHALL возвращаться 409. Иначе id SHALL добавляться, и ответ SHALL
содержать вишлист с products.

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

#### Scenario: Каталог не отвечает
- **WHEN** catalog-service не отвечает на `GET /products/{productId}` дольше 5 секунд
- **THEN** user-service отвечает 503 с телом `{message, time}`, товар не добавляется

**Observation**: OBS-03
**Assumption**: ASM-01

### Requirement: Wishlist error responses
Ошибки валидации (400), отсутствия вишлиста или товара (404), конфликтов (409)
и недоступности catalog-service (503) SHALL возвращаться с телом
`{message, time}`. Недоступностью catalog-service SHALL считаться ответ 5xx,
отсутствие ответа за 5 секунд, неудачное подключение за 2 секунды и другая
сетевая ошибка. Сообщения SHALL NOT содержать неподставленных шаблонов
вида `[%s]`.

#### Scenario: Каталог недоступен
- **WHEN** catalog-service отвечает 5xx при добавлении товара
- **THEN** ответ 503 с телом `{message, time}` и message "Catalog service is unavailable."

#### Scenario: Ошибка каталога при чтении вишлиста
- **WHEN** catalog-service отвечает 5xx на `POST /products/by-ids` при `GET /wishlists/{id}`
- **THEN** ответ 503 с телом `{message, time}`

#### Scenario: Отказ соединения при чтении вишлиста
- **WHEN** соединение с catalog-service при `GET /wishlists/{id}` с товарами отклонено
- **THEN** ответ 503 с телом `{message, time}` и message "Catalog service is unavailable."

**Observation**: OBS-03
**Assumption**: ASM-03
