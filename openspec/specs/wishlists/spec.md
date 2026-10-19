# wishlists Specification

## Purpose
Baseline (as-is) поведения user-service по ведению списков желаемых товаров
(вишлистов) пользователя: создание, просмотр, переименование и удаление
вишлистов, добавление и удаление товаров с проверкой существования товара в
catalog-service и сборка состава вишлиста из данных каталога. Спецификация
описывает фактическое текущее поведение кода, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Код user-service в
  рабочей копии не изменен.
- Метод: статический анализ кода, сгенерированного `WishlistMapperImpl` и схемы
  таблиц `wishlists` и `wishlist_products` в dev-БД. Тестов нет.
- Хранение: PostgreSQL, база `user_service`. Вишлист привязан к
  keycloakUserId, а не к записи профиля из `user-profile`.
- Зависимость: catalog-service, эндпоинты `GET /products/{id}` и
  `POST /products/by-ids` (`catalog-products`). Вызываются через service
  discovery (`lb://CATALOG-SERVICE`) с JWT текущего пользователя.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код сейчас. Изменять его
  нужно отдельным change.
- Пути указаны относительно user-service. Через api-gateway они доступны без
  изменений, через nginx — с префиксом `/api`.

## Requirements

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

### Requirement: Wishlist product representation
Каждый товар в products SHALL содержать только поля id, name, price,
articleNumber и imagesUrl из ответа catalog-service.

#### Scenario: Поля товара
- **WHEN** вишлист содержит товар
- **THEN** у товара в ответе есть id, name, price, articleNumber, imagesUrl и нет status, categories, description

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

### Requirement: Wishlist deletion
user-service SHALL удалять по `DELETE /wishlists/{id}` вишлист текущего
пользователя вместе со списком его товаров и SHALL отвечать 200 с телом
`{message, time}`. Для чужого или несуществующего вишлиста SHALL возвращаться
404.

#### Scenario: Удаление
- **WHEN** пользователь удаляет свой вишлист
- **THEN** ответ 200 с message "The wishlist was successfully deleted."

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

### Requirement: Wishlists independent of user profile
Вишлисты SHALL храниться по keycloakUserId независимо от профиля
`user-profile`. Удаление профиля SHALL NOT удалять вишлисты, и после повторного
создания профиля они SHALL оставаться доступны тому же пользователю.

#### Scenario: Удаление профиля
- **WHEN** пользователь удаляет профиль и снова вызывает `GET /wishlists`
- **THEN** ответ содержит прежние вишлисты

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

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. Ответ на создание не содержит вишлист.** _Устранено change
  `wishlist-create-response-id`: ответ `{message, time, id}`._ Было: чтобы
  узнать id нового вишлиста, клиенту нужно было запросить список.
- **OBS-02. Уникальность имени только в приложении.** _Устранено change
  `user-service-db-constraints`: имя обрезается, уникально без учета регистра,
  индекс `uq_wishlists_user_name`._ Было: имя уникально с учетом регистра и
  без обрезки пробелов, ограничения в БД нет.
- **OBS-03. Ошибки каталога без тела.** _Устранено change
  `wishlist-error-responses`: ошибки каталога возвращаются с телом
  `{message, time}`, сообщения без `[%s]`._ Было: «товар не найден» и «каталог
  недоступен» не обрабатывались `GlobalExceptionHandler` и возвращались без
  тела `{message, time}`, сообщение `PRODUCT_NOT_FOUND` содержало литерал `[%s]`.
- **OBS-04. 409 вместо 404.** _Устранено change `wishlist-error-responses`:
  404 с сообщением `WISHLIST_ITEM_NOT_FOUND`._ Было: отсутствие товара в
  вишлисте при удалении давало 409 с текстом «not found».
- **OBS-05. productCount и products расходятся.** _Устранено change
  `product-deletion-sync`: id физически удаленных товаров удаляются из
  вишлистов по событию PRODUCT_DELETED; удаленный товар не показывается в
  products, даже если id остался после редкой гонки (такие id и id, удаленные
  до change, очищаются ручной процедурой сверки с MongoDB)._ Остается:
  productCount учитывает id недоступных (архивных, заблокированных) товаров,
  которые products не показывает.
- **OBS-06. Порядок проверок при переименовании.** _Устранено change
  `wishlist-error-responses`: сначала существование и владение (404), затем
  имя (409)._ Было: проверка имени выполнялась до проверки существования и
  владения.
- **OBS-07. Статус товара не проверяется.** _Устранено: change
  `seller-store-lifecycle` — `GET /products/{id}` возвращает 404 для
  недоступного товара; change `wishlist-access-and-limits` — JWT с ролью
  SELLER не получает доступа к вишлистам, поэтому продавец-владелец не может
  добавить свой недоступный товар._
- **OBS-08. Нет ограничений в БД.** _Устранено: change
  `user-service-db-constraints` — уникальный индекс `wishlist_products
  (wishlist_id, product_id)` и `product_id NOT NULL`; change
  `wishlist-access-and-limits` — лимиты 20 вишлистов на пользователя и 200
  товаров в вишлисте с защитой от одновременных запросов._
- **OBS-09. Неиспользуемые методы репозитория.** _Устранено change
  `wishlist-access-and-limits`: `findByName` и
  `findByKeycloakUserIdAndNameContainingIgnoreCase` удалены._

## Assumptions (unverified)
- **ASM-01. JWT отклонен каталогом.** catalog-service проверяет issuer и
  срок токена даже на публичных эндпоинтах, если передан Bearer. Если токен
  отклонен, это 401 (4xx), и user-service ответит 404 «товар не найден».
- **ASM-02. Нет экземпляров catalog-service.** При отсутствии экземпляров
  catalog-service в Eureka балансировщик, вероятно, возвращает 503, и
  user-service отвечает 503. _Сетевые ошибки и таймауты стали нормативными
  change `wishlist-catalog-timeouts` (503, подключение 2 с, ответ 5 с; см.
  `Wishlist error responses`)._

## Open Questions
- **OQ-01. OBS-01–OBS-09.** Какие из них дефекты?
- **OQ-02. Товары, удаленные из каталога.** _Решено change
  `product-deletion-sync`: id удаляются по событию PRODUCT_DELETED, архивные и
  заблокированные товары остаются; гарантия отсутствия id в БД при любых
  гонках не требуется._
- **OQ-03. Регистр в имени.** _Решено change `user-service-db-constraints`:
  уникальность без учета регистра, пробелы по краям обрезаются._
- **OQ-04. Недоступные товары.** _Решено: недоступные товары не добавляются
  (catalog-service отдает 404), продавцы вишлисты не используют
  (change `wishlist-access-and-limits`)._
