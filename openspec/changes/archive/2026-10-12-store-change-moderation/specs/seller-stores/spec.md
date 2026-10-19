## ADDED Requirements

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

## MODIFIED Requirements

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
