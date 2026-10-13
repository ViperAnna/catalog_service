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
роль SELLER. Создание, изменение и удаление магазина SHALL отклоняться с 409,
если Seller в статусе BLOCKED; просмотр SHALL быть доступен
(`seller-onboarding` `Blocked seller restrictions`).

#### Scenario: Роль SELLER без записи Seller
- **WHEN** пользователь с ролью SELLER, для которого нет Seller, вызывает `GET /stores`
- **THEN** seller-service отвечает 404

#### Scenario: Seller в статусе DELETED
- **WHEN** запрос приходит с JWT, в котором еще есть роль SELLER, от пользователя, чей продавец окончательно удален
- **THEN** Seller не найден, seller-service отвечает 404

#### Scenario: Seller в статусе BLOCKED
- **WHEN** заблокированный продавец вызывает `PUT /stores/{id}` или `DELETE /stores/{id}`
- **THEN** seller-service отвечает 409, магазин не меняется

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
стран SHALL отклоняться с кодом 400. Правило SHALL действовать при создании
магазина и при подаче заявки на изменение.

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
- **WHEN** продавец отправляет `PUT /stores/{id}` с phoneNumber "89990001122"
- **THEN** заявка сохраняется с phoneNumber "+79990001122", после одобрения магазин получает "+79990001122"

### Requirement: Store name uniqueness per seller
seller-service SHALL отклонять с кодом 409 создание магазина, подачу заявки на
изменение и ее одобрение, если у Seller уже есть другой магазин, у которого
storeName совпадает после удаления пробелов по краям и без учета регистра.
Изменяемый магазин SHALL NOT считаться конфликтом с самим собой. Одинаковые
имена у разных продавцов SHALL допускаться. Уникальность SHALL обеспечиваться
проверкой в сервисе и индексом `(seller_id, lower(btrim(store_name)))`.

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
- **THEN** ответ 202, заявка создана, конфликта нет

#### Scenario: Переименование в имя другого своего магазина
- **WHEN** у продавца есть магазины "Shop" и "Outlet", и он отправляет PUT магазина "Outlet" со storeName " shop "
- **THEN** seller-service отвечает 409 и заявка не создается

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
storeName, для магазина текущего Seller (иначе 404) и SHALL создавать заявку
на изменение в статусе PENDING вместо изменения магазина. У магазина SHALL
быть не больше одной PENDING-заявки (иначе 409). Поле null в заявке SHALL NOT
менять значение при одобрении. Ответ SHALL иметь код 202 и содержать заявку.

#### Scenario: Изменение имени и описания
- **WHEN** продавец отправляет новое уникальное storeName и новый description
- **THEN** создается PENDING-заявка с этими значениями, ответ 202, магазин не меняется до одобрения

#### Scenario: null не очищает поле
- **WHEN** продавец отправляет description = null и заявку одобряют
- **THEN** description магазина остается прежним

#### Scenario: Изменение без email и телефона
- **WHEN** продавец отправляет PUT без email и phoneNumber
- **THEN** валидация проходит, в заявке email и phoneNumber равны null

#### Scenario: Изменение чужого магазина
- **WHEN** продавец отправляет PUT для магазина другого продавца
- **THEN** seller-service отвечает 404

#### Scenario: Вторая заявка
- **WHEN** у магазина уже есть PENDING-заявка, и продавец снова отправляет PUT
- **THEN** seller-service отвечает 409, новая заявка не создается

#### Scenario: Одновременная подача
- **WHEN** два PUT одного магазина одновременно проходят проверку отсутствия PENDING-заявки
- **THEN** одна заявка создается, второй запрос отклоняется индексом `uq_store_change_requests_pending` (SQLState 23505) и получает 409

**Observation**: OBS-06

### Requirement: Store deletion
seller-service SHALL удалять по `DELETE /stores/{id}` магазин текущего Seller
физически и SHALL отвечать 204. Для чужого или несуществующего магазина SHALL
возвращаться 404. Товары магазина seller-service SHALL NOT изменять сам: их и
неиспользуемые изображения физически удаляет catalog-service по событию
`STORE_PURGED` (`catalog-products` `Purging products of deleted store`).

#### Scenario: Удаление своего магазина
- **WHEN** ACTIVE-продавец удаляет свой ACTIVE-магазин
- **THEN** запись магазина удалена, ответ 204

#### Scenario: Товары после удаления
- **WHEN** удаляется магазин, у которого есть товары в каталоге
- **THEN** после обработки события STORE_PURGED документов товаров этого магазина в каталоге нет

### Requirement: Store response format
Ответы API магазинов SHALL содержать объект с полями id, sellerId, storeName,
description, email, phoneNumber, status, sellerStatus, createdAt, updatedAt.
Поле sellerId SHALL содержать id продавца-владельца, phoneNumber — телефон
магазина, status — статус магазина, sellerStatus — статус продавца. Поле
phone SHALL NOT возвращаться.

#### Scenario: Ответ на создание
- **WHEN** создается магазин с phoneNumber "+79990000000" продавцом с id 7
- **THEN** в ответе phoneNumber = "+79990000000", sellerId = 7, status = ACTIVE, sellerStatus = ACTIVE
- **AND** поля phone в ответе нет

#### Scenario: Список магазинов
- **WHEN** продавец с id 7 запрашивает `GET /stores`
- **THEN** у каждого магазина в ответе sellerId = 7 и phoneNumber равен сохраненному телефону магазина

#### Scenario: Изменение телефона
- **WHEN** модератор одобрил заявку с новым phoneNumber
- **THEN** `GET /stores/{id}` возвращает новое значение phoneNumber

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
Одобрение UPDATE, блокировка и разблокировка продавца SHALL NOT изменять и
удалять магазины продавца и их статусы. Одобрение DELETION SHALL физически
удалять все магазины продавца с событием STORE_PURGED на каждый. Seller и
магазины SHALL NOT удаляться иначе, чем через одобрение DELETION или
`DELETE /stores/{id}`.

#### Scenario: Удаление продавца
- **WHEN** одобряется заявка DELETION продавца с магазинами
- **THEN** магазины удалены из БД и для каждого записано STORE_PURGED

#### Scenario: Блокировка продавца
- **WHEN** продавца блокируют
- **THEN** магазины и их статусы не меняются

### Requirement: Store deleted event
При удалении магазина (владельцем или при удалении продавца) seller-service
SHALL в той же транзакции записывать в outbox событие с aggregateType `STORE`,
aggregateId = id магазина, eventType `STORE_PURGED`, processed = false и
payload `{storeId, sellerId}`. `STORE_DELETED` SHALL NOT публиковаться.
Если удаление откатилось, событие SHALL NOT сохраняться.

#### Scenario: Событие при удалении магазина
- **WHEN** продавец с id 7 удаляет свой магазин 3
- **THEN** в outbox записывается STORE_PURGED с aggregateId "3" и payload `{storeId: 3, sellerId: 7}`

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

### Requirement: Store status
Магазин SHALL иметь статус ACTIVE или BLOCKED (`stores.status`, NOT NULL).
Созданный магазин SHALL получать ACTIVE. Магазины, существовавшие до
появления статуса, SHALL получить ACTIVE. Состояние DELETED SHALL означать
физическое отсутствие строки и SHALL NOT храниться.

#### Scenario: Новый магазин
- **WHEN** ACTIVE-продавец создает магазин
- **THEN** магазин сохраняется со статусом ACTIVE

#### Scenario: Существующие магазины
- **WHEN** seller-service стартует после появления колонки status
- **THEN** у всех магазинов без статуса status = ACTIVE

### Requirement: Store blocking by moderator
seller-service SHALL принимать `POST /moderator/stores/{storeId}/block` и
`POST /moderator/stores/{storeId}/unblock` с ролью MODERATOR: block переводит
магазин из ACTIVE в BLOCKED, unblock — из BLOCKED в ACTIVE, ответ 204; нет
магазина — 404; другой исходный статус — 409. Блокировка SHALL NOT менять
статусы товаров и SHALL NOT удалять данные. Разблокировка магазина SHALL NOT
снимать блокировку продавца.

#### Scenario: Блокировка магазина
- **WHEN** модератор блокирует ACTIVE-магазин 3
- **THEN** магазин получает статус BLOCKED, ответ 204, записывается STORE_BLOCKED, товары не меняются и не удаляются

#### Scenario: Разблокировка магазина заблокированного продавца
- **WHEN** модератор разблокирует магазин 3, а его продавец BLOCKED
- **THEN** магазин получает статус ACTIVE, записывается STORE_UNBLOCKED, товары магазина остаются недоступными из-за блокировки продавца

#### Scenario: Недопустимый переход
- **WHEN** модератор разблокирует ACTIVE-магазин
- **THEN** seller-service отвечает 409, событий нет

### Requirement: Store availability events
seller-service SHALL записывать в outbox в той же транзакции, что и смену
статуса, события aggregateType STORE с ключом storeId и payload
`{storeId, sellerId}`: STORE_BLOCKED и STORE_UNBLOCKED при блокировке и
разблокировке магазина; SELLER_BLOCKED и SELLER_UNBLOCKED для каждого магазина
продавца при его блокировке и разблокировке. События блокировки SHALL NOT
записываться при удалении.

#### Scenario: Блокировка продавца с двумя магазинами
- **WHEN** блокируется продавец с магазинами 3 и 4
- **THEN** записываются SELLER_BLOCKED с ключами 3 и 4

### Requirement: Product changes in blocked store
Ответ `GET /stores/{id}`, по которому catalog-service проверяет владение,
SHALL содержать status магазина и sellerStatus продавца, чтобы
catalog-service отклонял изменение товаров при не-ACTIVE (`catalog-products`
`Product changes require active store and seller`). Владелец заблокированного
магазина SHALL сохранять просмотр магазина.

#### Scenario: Проверка владения заблокированного магазина
- **WHEN** владелец ACTIVE-продавец запрашивает `GET /stores/3`, а магазин 3 BLOCKED
- **THEN** ответ 200 с status = BLOCKED и sellerStatus = ACTIVE

### Requirement: Blocked store restrictions
Для магазина в статусе BLOCKED `PUT /stores/{id}` и `DELETE /stores/{id}`
SHALL отклоняться с 409, изменения товаров — по `catalog-products`
`Product changes require active store and seller`. Просмотр магазина и его
товаров владельцем SHALL сохраняться. Разблокировка магазина SHALL
выполняться только модератором; отдельной заявки для магазина SHALL NOT быть.

#### Scenario: Удаление заблокированного магазина
- **WHEN** владелец вызывает `DELETE /stores/3`, а магазин 3 BLOCKED
- **THEN** seller-service отвечает 409, магазин и товары не удаляются, STORE_PURGED не записывается

#### Scenario: Удаление после разблокировки
- **WHEN** модератор разблокировал магазин 3, и владелец ACTIVE вызывает `DELETE /stores/3`
- **THEN** магазин удаляется обычным образом с STORE_PURGED

### Requirement: Store change request moderation
seller-service SHALL принимать с ролью MODERATOR `GET
/moderator/store-change-requests/pending`, `GET .../{id}`, `POST .../{id}/approve`
(204) и `POST .../{id}/reject` с обязательной причиной (200). Нет заявки — 404,
своя заявка — 403, заявка не PENDING — 409. Одобрение SHALL переносить
непустые поля в магазин и переводить заявку в APPROVED; отклонение — в
REJECTED с причиной без изменения магазина.

#### Scenario: Одобрение заявки
- **WHEN** модератор одобряет PENDING-заявку магазина 3 с storeName "New" и description = null
- **THEN** у магазина 3 storeName = "New", description прежний, заявка APPROVED, ответ 204

#### Scenario: Отклонение заявки
- **WHEN** модератор отклоняет PENDING-заявку с причиной "Invalid phone"
- **THEN** заявка REJECTED с moderatorComment "Invalid phone", магазин не меняется

#### Scenario: Отклонение без причины
- **WHEN** модератор отклоняет заявку с пустой причиной
- **THEN** seller-service отвечает 400, заявка остается PENDING

#### Scenario: Своя заявка
- **WHEN** модератор рассматривает заявку, которую подал сам
- **THEN** seller-service отвечает 403

#### Scenario: Повторное рассмотрение
- **WHEN** модератор одобряет заявку в статусе REJECTED
- **THEN** seller-service отвечает 409

### Requirement: Store change approval checks
При одобрении seller-service SHALL проверять текущие статусы продавца и
магазина (не ACTIVE — 409) и уникальность имени из заявки среди других
магазинов продавца (занято — 409, в том числе при нарушении индекса
`uq_stores_seller_store_name`). При отказе по этим причинам заявка SHALL
оставаться PENDING, магазин SHALL NOT меняться.

#### Scenario: Имя занято за время рассмотрения
- **WHEN** после подачи заявки с именем "Outlet" продавец создал другой магазин "outlet", и модератор одобряет заявку
- **THEN** seller-service отвечает 409, заявка PENDING, магазин не меняется

#### Scenario: Магазин заблокирован после подачи
- **WHEN** модератор одобряет заявку магазина, который заблокировали после подачи
- **THEN** seller-service отвечает 409, заявка PENDING

#### Scenario: Отклонение заявки заблокированного магазина
- **WHEN** модератор отклоняет заявку магазина в статусе BLOCKED
- **THEN** заявка REJECTED

### Requirement: Store change requests retrieval
seller-service SHALL возвращать по `GET /stores/{id}/change-requests` заявки
магазина текущего Seller по убыванию даты подачи с полями id, storeId,
storeName, description, email, phoneNumber, status, moderatorComment,
createdAt, updatedAt. Для чужого или несуществующего магазина SHALL
возвращаться 404. Просмотр SHALL быть доступен при блокировке.

#### Scenario: История заявок
- **WHEN** у магазина есть REJECTED-заявка и более поздняя PENDING-заявка
- **THEN** ответ содержит обе, первой — PENDING

#### Scenario: Чужой магазин
- **WHEN** продавец запрашивает заявки магазина другого продавца
- **THEN** seller-service отвечает 404

### Requirement: Store change requests removal
Заявки на изменение магазина SHALL физически удаляться вместе с магазином:
при `DELETE /stores/{id}` и при окончательном удалении продавца.

#### Scenario: Удаление магазина с заявкой
- **WHEN** владелец удаляет магазин с PENDING-заявкой
- **THEN** магазин и все его заявки удалены, заявка исчезает из очереди модератора

#### Scenario: Подача одновременно с удалением
- **WHEN** владелец отправляет PUT магазина, а в это время магазин удаляется
- **THEN** операции выполняются по очереди: либо заявка создана и затем удалена вместе с магазином, либо магазин удален и PUT отвечает 404; ответа 500 нет

#### Scenario: Одобрение одновременно с удалением
- **WHEN** модератор одобряет заявку, а владелец в это время удаляет магазин
- **THEN** одна операция ждет другую: либо заявка одобрена и затем магазин удален вместе с заявками, либо магазин удален и одобрение отвечает 404 без изменений

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
- **OBS-06. Частичный PUT.** Поле нельзя очистить, передав null. После change
  `store-change-moderation` (архив `2026-10-12-store-change-moderation`)
  поведение сохраняется: null в заявке на изменение означает «не менять поле».
- **OBS-07. Товары после удаления магазина.** _Устранено change
  `archive-products-of-deleted-store` (архив
  `2026-10-06-archive-products-of-deleted-store`): удаление магазина публикует
  `STORE_DELETED`, и catalog-service переводит товары магазина в `ARCHIVED`._
  Было: удаление не учитывало товары, они оставались с прежним статусом.
  По-прежнему: проверка владения для них дает 404, а архивные товары видны в
  выдаче (`catalog-products` OBS-04).
- **OBS-08. Статус Seller не учитывается.** _Устранено change `seller-store-lifecycle`:
  заблокированный продавец только просматривает магазины (изменения — 409),
  удаленный продавец физически отсутствует вместе с магазинами._ Было:
  Seller в статусе DELETED или BLOCKED управлял магазинами, пока в его JWT
  была роль SELLER, магазины удаленного продавца оставались.
- **OBS-09. StoreStatus не используется.** _Устранено change `seller-store-lifecycle`:
  `stores.status` хранит ACTIVE или BLOCKED, DELETED означает физическое
  отсутствие строки._ Было: enum объявлен, но у `Store` не было поля статуса.
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
- **OQ-01. OBS-06.** Дефект ли это? (OBS-01–OBS-05, OBS-07–OBS-11
  устранены.)
- **OQ-02. Удаление магазина с товарами.** _Решено change `seller-store-lifecycle`:
  товары и неиспользуемые изображения удаляются физически (`STORE_PURGED`);
  заблокированный магазин удалить нельзя._
- **OQ-03. Статусы магазина.** _Решено change `seller-store-lifecycle`: ACTIVE и BLOCKED,
  блокирует и разблокирует модератор, DELETED — физическое удаление._
- **OQ-04. Магазины удаленного продавца.** _Решено change `seller-store-lifecycle`: при
  удалении продавца магазины удаляются, при блокировке не меняются, а их
  товары скрываются от покупателей._
- **OQ-05. Публичная информация о магазине.** Нужен ли публичный просмотр
  магазина для покупателей? Сейчас у товара есть только storeId, а
  `GET /stores/{id}` доступен лишь владельцу.
- **OQ-06. Модерация изменений магазина.** _Решено change `store-change-moderation`:
  `PUT /stores/{id}` создает заявку PENDING (202), магазин меняется только после
  одобрения модератором; заявки хранятся отдельно от заявок продавца, модерируются
  все поля, писем о результате нет._
