# seller-stores Specification

## Purpose
Baseline (as-is) поведения seller-service по управлению магазинами продавца:
создание, просмотр, изменение и удаление магазинов, принадлежащих текущему
продавцу, а также контракт `GET /stores/{id}`, которым catalog-service проверяет
владение магазином. Спецификация описывает фактическое текущее поведение кода,
а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Код магазинов в рабочей
  копии не изменен.
- Метод: статический анализ кода, сгенерированного MapStruct-кода
  (`StoreMapperImpl`) и фактической схемы таблицы `stores` в dev-БД. Тестов
  для магазинов нет, runtime-поведение не проверялось.
- Затронутые компоненты: `seller-service` (REST, JPA/PostgreSQL), потребитель
  `catalog-service`.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код сейчас. Изменять его
  нужно отдельным change.
- Пути указаны относительно seller-service. Через api-gateway они доступны без
  изменений, через nginx — с префиксом `/api`.
- Жизненный цикл Seller и роли SELLER описан в `seller-onboarding`.

## Requirements

### Requirement: Role-based access to store API
seller-service SHALL разрешать запросы к `/stores/**` только с JWT, содержащим
realm-роль SELLER (claim `realm_access.roles`). Владельцем запроса SHALL
считаться Seller, у которого keycloakUserId совпадает с claim `sub`.

#### Scenario: Запрос без токена
- **WHEN** клиент вызывает `GET /stores` без Bearer-токена
- **THEN** seller-service отвечает 401

#### Scenario: Нет роли SELLER
- **WHEN** клиент с JWT, содержащим только роль USER, вызывает `GET /stores`
- **THEN** seller-service отвечает 403

### Requirement: Current seller resolution
Каждая операция с магазинами SHALL определять текущего Seller по claim `sub`.
Если Seller не найден, seller-service SHALL отвечать 404, даже если в JWT есть
роль SELLER. Статус Seller (ACTIVE, BLOCKED, DELETED) SHALL NOT проверяться.

#### Scenario: Роль SELLER без записи Seller
- **WHEN** пользователь с ролью SELLER, для которого нет Seller, вызывает `GET /stores`
- **THEN** seller-service отвечает 404

#### Scenario: Seller в статусе DELETED
- **WHEN** запрос приходит с JWT, в котором еще есть роль SELLER, от пользователя, чей Seller имеет статус DELETED
- **THEN** операция выполняется так же, как для ACTIVE-продавца

**Observation**: OBS-08

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

### Requirement: Store field validation
seller-service SHALL отклонять с кодом 400 создание магазина, если storeName
пуст или длиннее 100 символов, email отсутствует или не является email,
phoneNumber отсутствует или не соответствует `^(\+7|8)\d{10}$`, или
description длиннее 255 символов. При изменении SHALL действовать те же
ограничения, но email, phoneNumber и description SHALL быть необязательными.
description SHALL быть необязательным и при создании.

#### Scenario: Пустое имя
- **WHEN** storeName пуст
- **THEN** seller-service отвечает 400

#### Scenario: Некорректный телефон
- **WHEN** phoneNumber равен "abc"
- **THEN** seller-service отвечает 400

#### Scenario: Длинное описание
- **WHEN** description содержит от 256 до 500 символов
- **THEN** seller-service отвечает 400 до обращения к БД

#### Scenario: Описание на границе
- **WHEN** description содержит ровно 255 символов
- **THEN** валидация проходит

### Requirement: Store phone normalization
seller-service SHALL принимать phoneNumber магазина только в формах
`+7XXXXXXXXXX` и `8XXXXXXXXXX`, где X — цифра, и SHALL сохранять его в
формате `+7XXXXXXXXXX`: у формы `8XXXXXXXXXX` первая цифра заменяется на
`+7`. Форма `7XXXXXXXXXX` без плюса, пробелы, скобки, дефисы и номера других
стран SHALL отклоняться с кодом 400. Правило SHALL действовать при создании и
при изменении магазина.

#### Scenario: Форма с восьмеркой
- **WHEN** продавец создает магазин с phoneNumber "89991234567"
- **THEN** магазин сохраняется с phoneNumber "+79991234567", и ответ содержит "+79991234567"

#### Scenario: Каноническая форма
- **WHEN** продавец передает phoneNumber "+79991234567"
- **THEN** сохраняется "+79991234567"

#### Scenario: Форма без плюса
- **WHEN** продавец передает phoneNumber "79991234567"
- **THEN** seller-service отвечает 400

#### Scenario: Номер с разделителями
- **WHEN** продавец передает phoneNumber "+7 999 123-45-67"
- **THEN** seller-service отвечает 400

#### Scenario: Изменение телефона в форме с восьмеркой
- **WHEN** продавец меняет phoneNumber магазина через `PUT /stores/{id}` на "89990001122"
- **THEN** сохраняется "+79990001122"

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

### Requirement: Own stores listing
seller-service SHALL возвращать по `GET /stores` все магазины текущего Seller
по убыванию createdAt. Если магазинов нет, SHALL возвращаться пустой список.
Магазины других продавцов SHALL NOT попадать в ответ.

#### Scenario: Список магазинов
- **WHEN** у продавца два магазина
- **THEN** ответ содержит оба, первым идет созданный позже

#### Scenario: Нет магазинов
- **WHEN** у продавца нет магазинов
- **THEN** ответ 200 с пустым списком

### Requirement: Store retrieval by id
seller-service SHALL возвращать по `GET /stores/{id}` магазин, только если он
принадлежит текущему Seller. Для магазина другого продавца и для
несуществующего id SHALL возвращаться 404.

#### Scenario: Свой магазин
- **WHEN** продавец запрашивает свой магазин
- **THEN** ответ 200 с данными магазина

#### Scenario: Чужой магазин
- **WHEN** продавец запрашивает магазин другого продавца
- **THEN** seller-service отвечает 404

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

### Requirement: Store deletion
seller-service SHALL удалять по `DELETE /stores/{id}` магазин текущего Seller
физически и SHALL отвечать 204. Для чужого или несуществующего магазина SHALL
возвращаться 404. Товары магазина seller-service SHALL NOT изменять сам: их
асинхронно архивирует catalog-service по событию `STORE_DELETED`
(`catalog-products`, `Archiving products of deleted store`).

#### Scenario: Удаление своего магазина
- **WHEN** продавец удаляет свой магазин
- **THEN** запись магазина удалена, ответ 204

#### Scenario: Товары после удаления
- **WHEN** удаляется магазин, у которого есть товары в каталоге
- **THEN** после обработки события STORE_DELETED товары остаются в каталоге со ссылкой на удаленный storeId и статусом ARCHIVED

### Requirement: Store response format
Ответы API магазинов SHALL содержать объект с полями id, sellerId, storeName,
description, email, phoneNumber, createdAt, updatedAt. Поле sellerId SHALL
содержать id продавца-владельца магазина, а phoneNumber — телефон магазина.
Поле phone SHALL NOT возвращаться.

#### Scenario: Ответ на создание
- **WHEN** создается магазин с phoneNumber "+79990000000" продавцом с id 7
- **THEN** в ответе phoneNumber = "+79990000000" и sellerId = 7
- **AND** поля phone в ответе нет

#### Scenario: Список магазинов
- **WHEN** продавец с id 7 запрашивает `GET /stores`
- **THEN** у каждого магазина в ответе sellerId = 7 и phoneNumber равен сохраненному телефону магазина

#### Scenario: Изменение телефона
- **WHEN** продавец меняет phoneNumber магазина через `PUT /stores/{id}`
- **THEN** ответ содержит новое значение phoneNumber

### Requirement: Store ownership lookup for catalog-service
catalog-service SHALL проверять владение магазином вызовом `GET /stores/{id}` в
seller-service (через service discovery) с JWT текущего продавца. Ответ 200
SHALL означать, что магазин принадлежит продавцу. Ответы 403 и 404 SHALL
трактоваться как «магазин не найден или нет доступа», ответ 5xx — как
недоступность seller-service.

#### Scenario: Создание товара в своем магазине
- **WHEN** продавец создает товар со storeId своего магазина
- **THEN** seller-service отвечает 200 и catalog-service продолжает создание

#### Scenario: Товар в чужом магазине
- **WHEN** продавец указывает storeId чужого магазина
- **THEN** seller-service отвечает 404 и catalog-service отклоняет операцию с 404

#### Scenario: seller-service недоступен
- **WHEN** seller-service отвечает 5xx
- **THEN** catalog-service отвечает 503

**Assumption**: ASM-02

### Requirement: Stores independent of seller lifecycle
Изменение Seller в рамках `seller-onboarding`, включая одобрение UPDATE и
DELETION, SHALL NOT изменять и удалять магазины продавца. Магазин SHALL NOT
иметь собственного статуса.

#### Scenario: Удаление продавца
- **WHEN** одобряется заявка DELETION продавца с магазинами
- **THEN** магазины остаются в БД без изменений

**Observation**: OBS-08, OBS-09

### Requirement: Store deleted event
При удалении магазина seller-service SHALL в той же транзакции записывать в
outbox событие с aggregateType `STORE`, aggregateId = id магазина, eventType
`STORE_DELETED`, processed = false и payload JSON `{storeId, sellerId}`.
Событие SHALL публиковаться в `store-events-topic`. Если удаление откатилось,
событие SHALL NOT сохраняться.

#### Scenario: Событие при удалении магазина
- **WHEN** продавец с id 7 удаляет свой магазин 3
- **THEN** в outbox записывается STORE_DELETED с aggregateId "3" и payload `{storeId: 3, sellerId: 7}`

#### Scenario: Удаление чужого магазина
- **WHEN** продавец пытается удалить магазин другого продавца
- **THEN** ответ 404, событие не записывается

### Requirement: Store API error responses
Ответы `/stores/**` об ошибках SHALL иметь формат из требования
`Seller service error response format` capability `seller-onboarding`: тело
JSON `{message, time}`. Коды ответов SHALL NOT меняться. 409 из-за занятого
имени магазина SHALL содержать message `Store with name [<storeName>] already exists.`,
а 404 из-за отсутствия магазина — `Store not found with id: [<id>].`

#### Scenario: Дубликат имени магазина
- **WHEN** у продавца есть магазин "Shop" и он создает магазин "shop"
- **THEN** seller-service отвечает 409 с телом `{message, time}` и message `Store with name [shop] already exists.`

#### Scenario: Чужой или несуществующий магазин
- **WHEN** продавец запрашивает `GET /stores/999999`, а такого магазина у него нет
- **THEN** seller-service отвечает 404 с message `Store not found with id: [999999].`

#### Scenario: Ошибка валидации магазина
- **WHEN** продавец создает магазин без phoneNumber
- **THEN** seller-service отвечает 400 с телом `{message, time}`, где message начинается с `Validation failed: `

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. sellerId в ответе всегда null.** _Устранено change
  `fix-store-response-fields` (архив `2026-10-06-fix-store-response-fields`):
  `StoreMapper` маппит `seller.id` в `sellerId`._ Было: `StoreResponse.sellerId`
  не заполнялся, потому что в `Store` есть только связь `seller`, а маппинга
  `seller.id` не было.
- **OBS-02. phone в ответе всегда null.** _Устранено change
  `fix-store-response-fields`: поле ответа переименовано в `phoneNumber` и
  заполняется телефоном магазина._ Было: в `StoreResponse` поле называлось
  `phone`, а в `Store` — `phoneNumber`, явного `@Mapping` не было, поэтому
  телефон в ответ не попадал.
- **OBS-03. Необязательные email и phoneNumber.** _Устранено change
  `fix-store-create-validation` (архив `2026-10-06-fix-store-create-validation`):
  при создании email и phoneNumber обязательны, их отсутствие дает 400._ Было:
  на уровне API поля были необязательны, а в БД объявлены `NOT NULL`, поэтому
  создание без них завершалось ошибкой БД (5xx).
- **OBS-04. Лимит description.** _Устранено change
  `fix-store-create-validation`: лимит API — 255 символов, как у колонки,
  превышение дает 400._ Было: API допускал до 500 символов при колонке
  `varchar(255)`, и описание длиной 256–500 символов давало 5xx.
- **OBS-05. Проверка имени при изменении.** _Устранено change
  `store-name-unique-per-seller` (архив `2026-10-06-store-name-unique-per-seller`):
  проверка исключает сам изменяемый магазин._ Было: PUT с текущим именем (в
  любом регистре) давал 409, и изменить description, email или телефон без
  переименования магазина было нельзя. storeName в PUT по-прежнему обязателен.
- **OBS-06. Частичный PUT.** Поле нельзя очистить, передав null.
- **OBS-07. Товары после удаления магазина.** _Устранено change
  `archive-products-of-deleted-store` (архив
  `2026-10-06-archive-products-of-deleted-store`): удаление магазина публикует
  `STORE_DELETED`, и catalog-service переводит товары магазина в `ARCHIVED`._
  Было: удаление не учитывало товары, они оставались с прежним статусом.
  По-прежнему: проверка владения для них дает 404, а архивные товары видны в
  выдаче (`catalog-products` OBS-04).
- **OBS-08. Статус Seller не учитывается.** Seller в статусе DELETED (или
  BLOCKED) может управлять магазинами, пока в его JWT есть роль SELLER.
  Магазины удаленного продавца остаются.
- **OBS-09. StoreStatus не используется.** Enum `StoreStatus`
  (ACTIVE, BLOCKED, DELETED) объявлен, но у `Store` нет поля статуса.
- **OBS-10. Уникальность имени без индекса.** _Устранено change
  `store-name-unique-per-seller`: уникальный индекс
  `uq_stores_seller_store_name ON stores (seller_id, lower(btrim(store_name)))`
  создает `schema.sql` при старте seller-service._ Было: уникальность
  обеспечивалась только проверкой в приложении.
- **OBS-11. Обработчика ошибок нет.** _Устранено change
  `seller-service-error-handling`: ответы `/stores/**` об ошибках имеют тело
  `{message, time}`._ Было: `@ControllerAdvice` не было, ошибки возвращались
  со стандартным телом Spring Boot.

## Assumptions (unverified)
- **ASM-01. Гонки.** _Неактуально после change `store-name-unique-per-seller`:
  второй из конкурентных запросов отклоняется уникальным индексом и получает
  409._ Было: параллельное создание двух магазинов с одним именем у одного
  продавца могло пройти проверку (OBS-10).
- **ASM-02. Прочие ответы для catalog-service.** Ответы seller-service, отличные
  от 200, 403, 404 и 5xx (например, 401 при истекшем токене), catalog-service,
  вероятно, не обрабатывает специально, и они приводят к 500. Это поведение
  catalog-service, его нужно подтвердить в будущем spec `catalog-products`.
- **ASM-03. Окно устаревшей роли.** После снятия роли SELLER при удалении
  продавца доступ к `/stores/**` сохраняется до истечения access-токена
  (до 300 с), см. `seller-onboarding` ASM-01.
- **ASM-04. Нечисловой id.** Нечисловой `{id}` в пути, вероятно, дает 400
  (стандартная обработка Spring).

## Open Questions
- **OQ-01. OBS-06, OBS-08, OBS-09.** Какие из них дефекты? (OBS-01–OBS-05,
  OBS-07, OBS-10 и OBS-11 устранены.)
- **OQ-02. Удаление магазина с товарами.** Должно ли оно запрещаться,
  каскадно скрывать или удалять товары, или передавать их?
- **OQ-03. Статусы магазина.** Нужен ли жизненный цикл магазина
  (ACTIVE, BLOCKED, DELETED) и кто его меняет?
- **OQ-04. Магазины удаленного продавца.** Что должно происходить с магазинами
  при удалении или блокировке продавца?
- **OQ-05. Публичная информация о магазине.** Нужен ли публичный просмотр
  магазина для покупателей? Сейчас у товара есть только storeId, а
  `GET /stores/{id}` доступен лишь владельцу.
