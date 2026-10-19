## MODIFIED Requirements

### Requirement: Wishlist creation
user-service SHALL принимать `POST /wishlists` с телом JSON `{name}`, где name
не пуст и не длиннее 100 символов, и SHALL создавать пустой вишлист текущего
пользователя с именем без пробелов по краям. Если у пользователя уже есть
вишлист с тем же именем без учета регистра и пробелов по краям, SHALL
возвращаться 409, в том числе при одновременных запросах. Ответ SHALL иметь
код 201 и тело `{message, time, id}`, где id — идентификатор созданного
вишлиста.

#### Scenario: Успешное создание
- **WHEN** пользователь создает вишлист "Gifts"
- **THEN** ответ 201 с message "The wishlist was successfully created" и id созданного вишлиста; `GET /wishlists/{id}` с этим id возвращает вишлист "Gifts"

#### Scenario: Дубликат имени
- **WHEN** у пользователя уже есть вишлист "Gifts" и он создает "Gifts"
- **THEN** ответ 409 с message "The wishlist with name [Gifts] already exist"

#### Scenario: Имя в другом регистре
- **WHEN** у пользователя есть "Gifts" и он создает "gifts"
- **THEN** ответ 409 с message "The wishlist with name [gifts] already exist", вишлист не создается

#### Scenario: Пробелы по краям
- **WHEN** пользователь создает вишлист "  Books  "
- **THEN** вишлист сохраняется с именем "Books"; повторное создание "Books" дает 409

#### Scenario: Одновременное создание
- **WHEN** два запроса пользователя одновременно создают "Gifts" и "GIFTS"
- **THEN** один ответ 201, второй 409, у пользователя один такой вишлист

#### Scenario: Одинаковые имена у разных пользователей
- **WHEN** у пользователя A есть "Gifts", и пользователь B создает "Gifts"
- **THEN** вишлист B создается

### Requirement: Wishlist rename
user-service SHALL принимать `PUT /wishlists/{id}` с телом `{name}` с теми же
ограничениями и той же обрезкой пробелов, что при создании. Сначала SHALL
проверяться существование и владение (иначе 404), затем то, что у
пользователя нет другого вишлиста с таким именем без учета регистра (иначе
409, в том числе при одновременных запросах). Ответ SHALL иметь код 200 и
тело `{message, time}`.

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

#### Scenario: Смена регистра своего имени
- **WHEN** пользователь переименовывает свой "Gifts" в "GIFTS"
- **THEN** ответ 200, имя "GIFTS"

#### Scenario: Имя занято в другом регистре
- **WHEN** у пользователя есть "Books", и он переименовывает другой вишлист в " books "
- **THEN** ответ 409, имя не меняется

### Requirement: Adding product to wishlist
`POST /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе 404),
затем запросить `GET /products/{productId}` у catalog-service. Ответ 4xx SHALL
давать 404 с message "Product with id [<productId>] not found.", ответ 5xx,
отсутствие ответа за 5 секунд, отказ соединения (дольше 2 секунд или
отклонено) и другая сетевая ошибка — 503 с message "Catalog service is
unavailable."; оба — с телом `{message, time}`. Если товар уже есть в
вишлисте, SHALL возвращаться 409, в том числе при одновременных запросах.
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

#### Scenario: Каталог не отвечает
- **WHEN** catalog-service не отвечает на `GET /products/{productId}` дольше 5 секунд
- **THEN** user-service отвечает 503 с телом `{message, time}`, товар не добавляется

#### Scenario: Одновременное добавление одного товара
- **WHEN** два запроса одновременно добавляют товар `p-1` в один вишлист
- **THEN** один ответ 200, второй 409 с message "Product with id [p-1] already exists in wishlist.", в вишлисте один `p-1`

**Observation**: OBS-03
**Assumption**: ASM-01
