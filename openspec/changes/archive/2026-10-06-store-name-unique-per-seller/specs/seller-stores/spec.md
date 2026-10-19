## MODIFIED Requirements

### Requirement: Store creation
seller-service SHALL принимать `POST /stores` с телом JSON
`{storeName, description, email, phoneNumber}`, где storeName, email и
phoneNumber обязательны, и SHALL создавать магазин, принадлежащий текущему
Seller, с переданными значениями полей, storeName без пробелов по краям и
нормализованным телефоном. Ответ SHALL иметь код 201 и содержать созданный
магазин.

#### Scenario: Успешное создание
- **WHEN** продавец отправляет валидный запрос с уникальным для него storeName, email и phoneNumber
- **THEN** создается магазин текущего Seller
- **AND** ответ имеет код 201 и содержит id, storeName, description, email, createdAt, updatedAt

#### Scenario: Создание без email или телефона
- **WHEN** продавец не передает email или phoneNumber
- **THEN** seller-service отвечает 400, магазин не создается и обращения к БД на запись нет

#### Scenario: Имя с пробелами по краям
- **WHEN** продавец создает магазин со storeName "  My Shop  "
- **THEN** магазин сохраняется со storeName "My Shop", регистр сохраняется

### Requirement: Store name uniqueness per seller
seller-service SHALL отклонять с кодом 409 создание и изменение магазина, если у
текущего Seller уже есть другой магазин, у которого storeName совпадает после
удаления пробелов по краям и без учета регистра. Изменяемый магазин SHALL NOT
считаться конфликтом с самим собой. Одинаковые имена у магазинов разных
продавцов SHALL допускаться. Уникальность SHALL обеспечиваться и проверкой в
сервисе, и уникальным индексом БД по `(seller_id, lower(btrim(store_name)))`.

#### Scenario: Дубликат имени у того же продавца
- **WHEN** у продавца есть магазин "Shop" и он создает магазин "shop"
- **THEN** seller-service отвечает 409

#### Scenario: Дубликат с пробелами и в другом регистре
- **WHEN** у продавца есть магазин "Shop" и он создает магазин "  SHOP  "
- **THEN** seller-service отвечает 409

#### Scenario: Одинаковое имя у разных продавцов
- **WHEN** у другого продавца есть магазин "Shop"
- **THEN** создание магазина "Shop" текущим продавцом успешно

#### Scenario: Изменение без смены имени
- **WHEN** продавец отправляет PUT магазина "Shop" со storeName "shop" и новым description
- **THEN** ответ 200, description обновлен, конфликта нет

#### Scenario: Переименование в имя другого своего магазина
- **WHEN** у продавца есть магазины "Shop" и "Outlet", и он меняет имя "Outlet" на " shop "
- **THEN** seller-service отвечает 409 и магазин не меняется

#### Scenario: Конкурентное создание
- **WHEN** два запроса одного продавца одновременно создают магазины "Shop" и "shop", и проверка в сервисе проходит в обоих
- **THEN** один запрос успешен, второй отклоняется уникальным индексом (SQLState 23505, `uq_stores_seller_store_name`) и получает 409

### Requirement: Store update
seller-service SHALL принимать `PUT /stores/{id}` с телом JSON
`{storeName, description, email, phoneNumber}`, где обязателен только
storeName, и SHALL изменять только магазин текущего Seller (иначе 404).
Каждое из полей storeName, description, email, phoneNumber SHALL меняться
только при значении, отличном от null; storeName SHALL сохраняться без
пробелов по краям, phoneNumber — нормализованным. Ответ SHALL иметь код 200 и
содержать магазин.

#### Scenario: Изменение имени и описания
- **WHEN** продавец отправляет новое уникальное storeName и новый description
- **THEN** оба поля обновлены, ответ 200

#### Scenario: null не очищает поле
- **WHEN** продавец отправляет description = null
- **THEN** description магазина остается прежним

#### Scenario: Изменение без email и телефона
- **WHEN** продавец отправляет PUT без email и phoneNumber
- **THEN** валидация проходит, email и phoneNumber магазина остаются прежними

#### Scenario: Изменение чужого магазина
- **WHEN** продавец отправляет PUT для магазина другого продавца
- **THEN** seller-service отвечает 404

**Observation**: OBS-06
