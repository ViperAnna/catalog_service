## ADDED Requirements

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

## MODIFIED Requirements

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
- **WHEN** продавец меняет phoneNumber магазина через `PUT /stores/{id}`
- **THEN** ответ содержит новое значение phoneNumber

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
