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
realm-роль USER или ADMIN. Владельцем вишлиста SHALL считаться пользователь с
keycloakUserId = claim `sub`. Операции над вишлистом другого пользователя или
несуществующим вишлистом SHALL давать 404. Наличие профиля пользователя SHALL
NOT требоваться.

#### Scenario: Чужой вишлист
- **WHEN** пользователь запрашивает `GET /wishlists/{id}` вишлиста другого пользователя
- **THEN** user-service отвечает 404 с message "The wishlist with id [<id>] not found."

#### Scenario: Без профиля
- **WHEN** пользователь, ни разу не вызывавший `GET /users/me`, создает вишлист
- **THEN** вишлист создается

#### Scenario: Нет нужной роли
- **WHEN** запрос приходит с JWT только с ролью SELLER
- **THEN** user-service отвечает 403

### Requirement: Wishlist creation
user-service SHALL принимать `POST /wishlists` с телом JSON `{name}`, где name
не пуст и не длиннее 100 символов, и SHALL создавать пустой вишлист текущего
пользователя. Если у пользователя уже есть вишлист с точно таким же именем
(с учетом регистра), SHALL возвращаться 409. Ответ SHALL иметь код 201 и тело
`{message, time}` без данных вишлиста.

#### Scenario: Успешное создание
- **WHEN** пользователь создает вишлист "Gifts"
- **THEN** ответ 201 с message "The wishlist was successfully created"

#### Scenario: Дубликат имени
- **WHEN** у пользователя уже есть вишлист "Gifts" и он создает "Gifts"
- **THEN** ответ 409 с message "The wishlist with name [Gifts] already exist"

#### Scenario: Имя в другом регистре
- **WHEN** у пользователя есть "Gifts" и он создает "gifts"
- **THEN** вишлист создается

**Observation**: OBS-01, OBS-02

### Requirement: Wishlist listing
user-service SHALL возвращать по `GET /wishlists` все вишлисты текущего
пользователя в виде `{id, name, productCount, createdAt, updatedAt}`, где
productCount — число идентификаторов товаров, сохраненных в вишлисте. Для
списка SHALL NOT вызываться catalog-service, порядок списка SHALL NOT быть
определен.

#### Scenario: Пустой список
- **WHEN** у пользователя нет вишлистов
- **THEN** ответ 200 с пустым списком

#### Scenario: Удаленный из каталога товар
- **WHEN** в вишлисте сохранен id товара, которого больше нет в каталоге
- **THEN** этот id учитывается в productCount

**Observation**: OBS-05

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

### Requirement: Wishlist product representation
Каждый товар в products SHALL содержать только поля id, name, price,
articleNumber и imagesUrl из ответа catalog-service.

#### Scenario: Поля товара
- **WHEN** вишлист содержит товар
- **THEN** у товара в ответе есть id, name, price, articleNumber, imagesUrl и нет status, categories, description

### Requirement: Wishlist rename
user-service SHALL принимать `PUT /wishlists/{id}` с телом `{name}` с теми же
ограничениями, что при создании. Сначала SHALL проверяться, что у пользователя
нет другого вишлиста с таким именем (иначе 409), затем существование и
владение (иначе 404). Ответ SHALL иметь код 200 и тело `{message, time}`.

#### Scenario: Переименование
- **WHEN** пользователь меняет имя на свободное
- **THEN** ответ 200 с message "The wishlist was successfully updated."

#### Scenario: То же имя
- **WHEN** пользователь отправляет текущее имя вишлиста
- **THEN** ответ 200

#### Scenario: Конфликт для несуществующего вишлиста
- **WHEN** id не существует, а имя совпадает с другим вишлистом пользователя
- **THEN** ответ 409, а не 404

**Observation**: OBS-06

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

### Requirement: Removing product from wishlist
`DELETE /wishlists/{id}/products/{productId}` SHALL проверить владение (иначе
404) и удалить id из вишлиста без обращения к `GET /products/{id}`. Если id нет
в вишлисте, SHALL возвращаться 409 с message
"Product with id [<productId>] not found.". Ответ SHALL содержать вишлист с
products.

#### Scenario: Удаление товара
- **WHEN** пользователь удаляет товар из вишлиста
- **THEN** ответ 200 содержит вишлист без этого товара

#### Scenario: Товара нет в вишлисте
- **WHEN** id товара не сохранен в вишлисте
- **THEN** ответ 409

**Observation**: OBS-04

### Requirement: Wishlists independent of user profile
Вишлисты SHALL храниться по keycloakUserId независимо от профиля
`user-profile`. Удаление профиля SHALL NOT удалять вишлисты, и после повторного
создания профиля они SHALL оставаться доступны тому же пользователю.

#### Scenario: Удаление профиля
- **WHEN** пользователь удаляет профиль и снова вызывает `GET /wishlists`
- **THEN** ответ содержит прежние вишлисты

### Requirement: Wishlist error responses
Ошибки валидации (400), отсутствия вишлиста (404) и конфликтов (409) SHALL
возвращаться с телом `{message, time}`. Ошибки «товар не найден в каталоге»
(404) и «catalog-service недоступен» (503) SHALL возвращаться со стандартным
телом ошибки Spring, без `{message, time}`.

#### Scenario: Каталог недоступен
- **WHEN** catalog-service отвечает 5xx при добавлении товара
- **THEN** ответ 503 со стандартным телом Spring

**Observation**: OBS-03
**Assumption**: ASM-03

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. Ответ на создание не содержит вишлист.** Чтобы узнать id нового
  вишлиста, клиенту нужно запросить список.
- **OBS-02. Уникальность имени только в приложении.** Имя уникально с учетом
  регистра и без обрезки пробелов. Ограничения в БД нет.
- **OBS-03. Ошибки каталога без тела.** «Товар не найден» и «каталог
  недоступен» не обрабатываются `GlobalExceptionHandler`, поэтому возвращаются
  без тела `{message, time}`. Кроме того, сообщение `PRODUCT_NOT_FOUND`
  создается без аргумента и содержит литерал `[%s]`.
- **OBS-04. 409 вместо 404.** Отсутствие товара в вишлисте при удалении дает
  409 с текстом «not found». Сообщение `WISHLIST_ITEM_NOT_FOUND` объявлено, но
  не используется.
- **OBS-05. productCount и products расходятся.** productCount в списке
  считает и id товаров, удаленных из каталога, а products в карточке их не
  показывает. Сохраненные id таких товаров не очищаются.
- **OBS-06. Порядок проверок при переименовании.** Проверка имени выполняется
  до проверки существования и владения.
- **OBS-07. Статус товара не проверяется.** В вишлист можно добавить товар в
  любом статусе.
- **OBS-08. Нет ограничений в БД.** Число вишлистов и товаров в них не
  ограничено. У `wishlist_products` нет первичного и уникального ключа,
  отсутствие дублей обеспечивается только кодом.
- **OBS-09. Неиспользуемые методы репозитория.** `findByName` и
  `findByKeycloakUserIdAndNameContainingIgnoreCase` не используются.

## Assumptions (unverified)
- **ASM-01. JWT отклонен каталогом.** catalog-service проверяет issuer и
  срок токена даже на публичных эндпоинтах, если передан Bearer. Если токен
  отклонен, это 401 (4xx), и user-service ответит 404 «товар не найден».
- **ASM-02. Нет экземпляров catalog-service.** При отсутствии экземпляров
  catalog-service в Eureka балансировщик, вероятно, возвращает 503, и
  user-service отвечает 503. Сетевые ошибки и таймауты, вероятно, дают 500:
  таймауты у WebClient не настроены.
- **ASM-03. Тело ошибок.** Тело стандартной ошибки Spring, вероятно, не
  включает текст сообщения (`server.error.include-message` не задан).
- **ASM-04. Сбой каталога при чтении вишлиста.** Ошибка catalog-service при
  `GET /wishlists/{id}` и после добавления или удаления товара дает ошибку
  всего запроса, хотя изменение состава вишлиста к этому моменту уже
  выполнено в транзакции. Будет ли оно откачено, зависит от типа исключения.

## Open Questions
- **OQ-01. OBS-01–OBS-09.** Какие из них дефекты?
- **OQ-02. Товары, удаленные из каталога.** Очищать ли их id в вишлистах?
- **OQ-03. Регистр в имени.** Должна ли уникальность имени вишлиста не
  зависеть от регистра?
- **OQ-04. Недоступные товары.** Нужно ли запрещать добавление товаров не в
  статусе ACTIVE?
