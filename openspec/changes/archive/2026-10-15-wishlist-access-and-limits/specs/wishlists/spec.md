## MODIFIED Requirements

### Requirement: Wishlist API access and ownership
user-service SHALL разрешать `/wishlists/**` только с JWT, содержащим
realm-роль USER или ADMIN и не содержащим роль SELLER. JWT с ролью SELLER
SHALL получать 403 независимо от других ролей. Владельцем вишлиста SHALL
считаться пользователь с keycloakUserId = claim `sub`. Операции над вишлистом
другого пользователя или несуществующим вишлистом SHALL давать 404. Наличие
профиля пользователя SHALL NOT требоваться.

#### Scenario: Чужой вишлист
- **WHEN** пользователь запрашивает `GET /wishlists/{id}` вишлиста другого пользователя
- **THEN** user-service отвечает 404 с message "The wishlist with id [<id>] not found."

#### Scenario: Без профиля
- **WHEN** пользователь, ни разу не вызывавший `GET /users/me`, создает вишлист
- **THEN** вишлист создается

#### Scenario: Нет нужной роли
- **WHEN** запрос приходит с JWT только с ролью SELLER
- **THEN** user-service отвечает 403

#### Scenario: Продавец с ролью USER
- **WHEN** запрос `GET /wishlists` или `POST /wishlists` приходит с JWT с ролями USER и SELLER
- **THEN** user-service отвечает 403, вишлист не создается

#### Scenario: Вишлисты после получения роли SELLER
- **WHEN** у пользователя есть вишлисты, и ему назначена роль SELLER
- **THEN** после выпуска нового токена запросы к `/wishlists/**` дают 403, вишлисты сохраняются в БД

### Requirement: Wishlist creation
user-service SHALL принимать `POST /wishlists` с телом JSON `{name}`, где name
не пуст и не длиннее 100 символов, и SHALL создавать пустой вишлист текущего
пользователя с именем без пробелов по краям. Если у пользователя уже есть
вишлист с тем же именем без учета регистра и пробелов по краям, SHALL
возвращаться 409, в том числе при одновременных запросах. Если у пользователя
уже 20 вишлистов, SHALL возвращаться 409, в том числе при одновременных
запросах. Ответ SHALL иметь код 201 и тело `{message, time, id}`, где id —
идентификатор созданного вишлиста.

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

#### Scenario: Лимит вишлистов
- **WHEN** у пользователя 20 вишлистов, и он создает еще один
- **THEN** ответ 409 с message "Wishlist limit reached: at most 20 wishlists per user.", вишлист не создается

#### Scenario: Одновременное создание на границе лимита
- **WHEN** у пользователя 19 вишлистов, и два запроса одновременно создают вишлисты с разными именами
- **THEN** один ответ 201, второй 409, у пользователя 20 вишлистов

### Requirement: Adding product to wishlist
`POST /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе 404),
затем запросить `GET /products/{productId}` у catalog-service. Ответ 4xx SHALL
давать 404 с message "Product with id [<productId>] not found.", ответ 5xx,
отсутствие ответа за 5 секунд, отказ соединения (дольше 2 секунд или
отклонено) и другая сетевая ошибка — 503 с message "Catalog service is
unavailable."; оба — с телом `{message, time}`. Если товар уже есть в
вишлисте, SHALL возвращаться 409, в том числе при одновременных запросах. Если
в вишлисте уже 200 товаров, SHALL возвращаться 409, в том числе при
одновременных запросах. Иначе id SHALL добавляться, и ответ SHALL содержать
вишлист с products.

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

#### Scenario: Лимит товаров
- **WHEN** в вишлисте 200 товаров, и пользователь добавляет еще один
- **THEN** ответ 409 с message "Wishlist product limit reached: at most 200 products per wishlist.", товар не добавляется

#### Scenario: Одновременное добавление на границе лимита
- **WHEN** в вишлисте 199 товаров, и два запроса одновременно добавляют разные товары
- **THEN** один ответ 200, второй 409, в вишлисте 200 товаров

**Observation**: OBS-03
**Assumption**: ASM-01

### Requirement: Removing product from wishlist
`DELETE /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе
404) и удалить id из вишлиста без обращения к `GET /products/{id}`. Если id нет
в вишлисте, SHALL возвращаться 404 с message
"Product with id [<productId>] not found in wishlist.". Ответ SHALL содержать
вишлист с products. Если catalog-service недоступен при сборке ответа,
удаление SHALL сохраняться, ответ SHALL быть 200 с `products = []`, а
остальные id SHALL оставаться в вишлисте.

#### Scenario: Удаление товара
- **WHEN** пользователь удаляет товар из вишлиста
- **THEN** ответ 200 содержит вишлист без этого товара

#### Scenario: Товара нет в вишлисте
- **WHEN** id товара `p-1` не сохранен в вишлисте
- **THEN** ответ 404 с message "Product with id [p-1] not found in wishlist.", вишлист не меняется

#### Scenario: Каталог недоступен при удалении
- **WHEN** в вишлисте `p-1` и `p-2`, catalog-service недоступен, и пользователь удаляет `p-1`
- **THEN** ответ 200 с `products = []`; после восстановления каталога `GET /wishlists/{id}` содержит только `p-2`

**Observation**: OBS-04

### Requirement: Wishlist error responses
Ошибки валидации (400), отсутствия вишлиста или товара (404), конфликтов (409)
и недоступности catalog-service (503) SHALL возвращаться с телом
`{message, time}`. Недоступностью catalog-service SHALL считаться ответ 5xx,
отсутствие ответа за 5 секунд, неудачное подключение за 2 секунды и другая
сетевая ошибка; при удалении товара она SHALL NOT давать ошибку (`Removing
product from wishlist`). Сообщения SHALL NOT содержать неподставленных шаблонов
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
