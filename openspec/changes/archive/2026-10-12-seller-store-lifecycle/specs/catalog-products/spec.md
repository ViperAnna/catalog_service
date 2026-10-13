## ADDED Requirements

### Requirement: Product availability
Товар SHALL считаться доступным покупателю, только если его status = ACTIVE
и у него не установлены признаки storeBlocked и sellerBlocked (отсутствие
признака = false). Публичные API каталога SHALL возвращать недоступный товар
только его владельцу (`Product retrieval by id`). Решение SHALL приниматься
без вызова seller-service на каждый запрос. Статусы товаров SHALL NOT
меняться из-за блокировок.

#### Scenario: Черновик
- **WHEN** у доступного магазина есть товар в статусе DRAFT
- **THEN** товар не возвращается анонимному пользователю ни одним публичным API

#### Scenario: Заблокированный магазин
- **WHEN** магазин 3 заблокирован, а его товар ACTIVE
- **THEN** товар не возвращается публичными API, его status остается ACTIVE

#### Scenario: Разблокировка
- **WHEN** магазин 3 и его продавец снова ACTIVE
- **THEN** ACTIVE-товары магазина 3 снова возвращаются публичными API

#### Scenario: Владельческий список
- **WHEN** владелец вызывает `GET /products/stores/{storeId}/products`
- **THEN** ответ содержит товары магазина в любом статусе и с любыми признаками

### Requirement: Product blocking flags
catalog-service SHALL хранить в документе товара признаки storeBlocked и
sellerBlocked и обновлять их для всех товаров storeId по событиям
`store-events-topic`: STORE_BLOCKED и STORE_UNBLOCKED задают storeBlocked,
SELLER_BLOCKED и SELLER_UNBLOCKED — sellerBlocked. Событие SHALL задавать
абсолютное значение, повторная обработка SHALL NOT менять результат, один
признак SHALL NOT менять другой.

#### Scenario: Независимые блокировки
- **WHEN** магазин 3 заблокирован отдельно, затем продавца блокируют и разблокируют
- **THEN** у товаров магазина 3 storeBlocked = true, sellerBlocked = false, товары недоступны

#### Scenario: Повтор события
- **WHEN** STORE_BLOCKED для магазина 3 обрабатывается дважды
- **THEN** storeBlocked = true у товаров магазина 3, ошибки нет

#### Scenario: Существующие товары
- **WHEN** у товара нет полей storeBlocked и sellerBlocked
- **THEN** он считается не заблокированным

### Requirement: Product changes require active store and seller
Создание, изменение и удаление товара SHALL отклоняться с 409, если в ответе
проверки владения (`seller-stores` `Store ownership lookup for
catalog-service`) status магазина или sellerStatus не ACTIVE. Проверка SHALL
выполняться в момент операции. Создание и изменение товара SHALL NOT
записывать признаки storeBlocked и sellerBlocked: их меняют только события.

#### Scenario: Изменение товара заблокированного магазина
- **WHEN** владелец изменяет товар магазина в статусе BLOCKED
- **THEN** catalog-service отвечает 409, товар не меняется

#### Scenario: Создание товара заблокированным продавцом
- **WHEN** заблокированный продавец создает товар
- **THEN** catalog-service отвечает 409, изображения не загружаются

#### Scenario: Блокировка во время изменения
- **WHEN** во время изменения товара обработано STORE_BLOCKED его магазина
- **THEN** после сохранения изменения у товара storeBlocked = true, товар недоступен

### Requirement: Purging products of deleted store
На событие STORE_PURGED catalog-service SHALL удалить неиспользуемые
изображения товаров магазина по правилам `catalog-media`, затем физически
удалить документы всех товаров с этим storeId. Повторная обработка SHALL
доводить удаление до конца без ошибки; ошибка SHALL обрабатываться
повторами и DLT `store-events-topic`. События блокировки SHALL NOT удалять
товары.

#### Scenario: Удаление товаров магазина
- **WHEN** получено STORE_PURGED для магазина 3 с товарами в статусах ACTIVE и DRAFT
- **THEN** документов товаров магазина 3 в MongoDB нет, товары других магазинов не меняются

#### Scenario: Повтор после частичного сбоя
- **WHEN** первая обработка STORE_PURGED удалила часть изображений и упала
- **THEN** повторная обработка удаляет оставшиеся неиспользуемые изображения и все документы товаров магазина

#### Scenario: Блокировка не удаляет товары
- **WHEN** получено STORE_BLOCKED или SELLER_BLOCKED
- **THEN** документы товаров не удаляются

## MODIFIED Requirements

### Requirement: Product listing with pagination
catalog-service SHALL возвращать по `GET /products` страницу доступных
товаров (`Product availability`) с параметрами page, size и sort (по
умолчанию page = 0, size = 5) в виде объекта Spring Data Page с полями
content, pageable, totalElements, totalPages, number, size, numberOfElements,
first, last, empty, sort. totalElements SHALL учитывать только доступные
товары.

#### Scenario: Страница по умолчанию
- **WHEN** клиент вызывает `GET /products` без параметров
- **THEN** ответ содержит не более 5 товаров в content и number = 0

#### Scenario: Товары в статусе DRAFT
- **WHEN** существует товар в статусе DRAFT
- **THEN** он не попадает в выдачу, в том числе для его владельца

**Assumption**: ASM-01

### Requirement: Product listing by category
Если в `GET /products` передан categoryId, catalog-service SHALL проверить, что
категория существует (иначе 404), и SHALL вернуть страницу доступных товаров,
в списке категорий которых есть categoryId, с теми же параметрами пагинации.

#### Scenario: Фильтр по категории
- **WHEN** клиент вызывает `GET /products?categoryId=<id>&size=1`
- **THEN** ответ содержит страницу доступных товаров этой категории

#### Scenario: Несуществующая категория
- **WHEN** categoryId не существует
- **THEN** ответ 404

### Requirement: Product retrieval by id
catalog-service SHALL возвращать по `GET /products/{id}` доступный товар
любому клиенту. Недоступный товар SHALL возвращаться только при JWT с ролью
SELLER и успешной проверке владения его магазином (`seller-stores`
`Store ownership lookup for catalog-service`); иначе, как и для
несуществующего id, SHALL возвращаться 404 с телом `{message, time}`.

#### Scenario: Товар не найден
- **WHEN** запрашивается несуществующий id
- **THEN** ответ 404

#### Scenario: Недоступный товар для покупателя
- **WHEN** анонимный пользователь или чужой продавец запрашивает товар в статусе DRAFT или товар заблокированного магазина
- **THEN** ответ 404

#### Scenario: Черновик для владельца
- **WHEN** владелец магазина с JWT запрашивает свой товар в статусе DRAFT
- **THEN** ответ 200 с товаром

### Requirement: Product search by exact name
catalog-service SHALL возвращать по `GET /products/productByName/{name}` все
доступные товары, имя которых совпадает с name без учета регистра (полное
совпадение, не подстрока). Если таких товаров нет, SHALL возвращаться 404.

#### Scenario: Совпадение без учета регистра
- **WHEN** существует доступный товар "Camera" и запрашивается "camera"
- **THEN** ответ содержит этот товар

#### Scenario: Частичное совпадение
- **WHEN** существует товар "Camera Pro" и запрашивается "camera"
- **THEN** ответ 404

#### Scenario: Только недоступные совпадения
- **WHEN** единственный товар "Camera" находится в статусе INACTIVE
- **THEN** ответ 404

**Observation**: OBS-14

### Requirement: Products batch retrieval by ids
catalog-service SHALL возвращать по `POST /products/by-ids` с телом JSON-массива
идентификаторов список найденных доступных товаров. Несуществующие и
недоступные идентификаторы SHALL пропускаться без ошибки. Порядок результата
SHALL NOT зависеть от порядка во входном массиве.

#### Scenario: Часть идентификаторов не существует
- **WHEN** передаются один существующий доступный и один несуществующий id
- **THEN** ответ 200 содержит один товар

#### Scenario: Недоступный товар
- **WHEN** передается id товара заблокированного магазина
- **THEN** ответ 200 не содержит этот товар

**Observation**: OBS-12

### Requirement: Archiving products of deleted store
catalog-service SHALL потреблять `store-events-topic` в consumer group
`catalog-group` и на событие `STORE_DELETED` (его больше не публикуют, но
сообщения могут оставаться в топике и DLT) SHALL переводить в `ARCHIVED` все
товары с этим storeId, у которых статус еще не `ARCHIVED`, не удаляя их.
Повторная обработка SHALL NOT менять результат. Неизвестный eventType SHALL
пропускаться с предупреждением в логе.

#### Scenario: Архивация товаров магазина
- **WHEN** получено STORE_DELETED для storeId 3, и у магазина товары в статусах ACTIVE и DRAFT
- **THEN** все товары магазина 3 получают статус ARCHIVED, товары других магазинов не меняются

#### Scenario: Повторная доставка
- **WHEN** то же событие STORE_DELETED обрабатывается второй раз
- **THEN** товары остаются в статусе ARCHIVED, ошибки нет

#### Scenario: Магазин без товаров
- **WHEN** получено STORE_DELETED для магазина без товаров
- **THEN** ничего не меняется, сообщение считается обработанным

#### Scenario: Неизвестный тип события
- **WHEN** в `store-events-topic` приходит конверт с неизвестным eventType
- **THEN** сообщение пропускается с предупреждением, товары не меняются

#### Scenario: Архивные товары в выдаче
- **WHEN** товары удаленного магазина заархивированы
- **THEN** они не возвращаются покупателям (`Product availability`)

### Requirement: Product creation preconditions
При создании catalog-service SHALL выполнять проверки в порядке: каждая
категория существует (иначе 404), магазин принадлежит продавцу, и его status
и sellerStatus ACTIVE (иначе 404 или 409, при недоступности seller-service
503). Затем SHALL загружаться изображения и сохраняться товар, после чего
проверка магазина SHALL повторяться; при не-ACTIVE, отсутствии магазина или
недоступности seller-service созданный документ SHALL удаляться с тем же кодом.

#### Scenario: Несуществующая категория
- **WHEN** в categories указан несуществующий id
- **THEN** ответ 404 с message "Category not found with id: [<id>]." и обращения к seller-service нет

#### Scenario: Чужой магазин
- **WHEN** storeId принадлежит другому продавцу
- **THEN** ответ 404, изображения не загружаются

#### Scenario: seller-service недоступен
- **WHEN** seller-service отвечает 5xx
- **THEN** catalog-service отвечает 503

#### Scenario: Блокировка во время создания
- **WHEN** магазин заблокирован после первой проверки, но до повторной
- **THEN** созданный документ удален, ответ 409, товар не становится доступным

#### Scenario: Удаление магазина во время создания
- **WHEN** магазин удален после первой проверки, но до повторной
- **THEN** созданный документ удален, ответ 404

**Assumption**: ASM-02

### Requirement: Product update
catalog-service SHALL принимать `PUT /products/{id}` как `multipart/form-data` с
теми же полями и ограничениями, что при создании, кроме storeId. Если товара
нет, SHALL возвращаться 404. Иначе SHALL проверяться владение и статусы
магазина, затем существование категорий, после чего SHALL заменяться
изображения и редактируемые поля товара частичным обновлением. Магазин товара
и признаки блокировки SHALL NOT меняться. Ответ SHALL иметь код 201.

#### Scenario: Успешное изменение
- **WHEN** продавец отправляет валидный PUT для товара своего магазина
- **THEN** товар обновлен, ответ 201 с message "The product was successfully updated."

#### Scenario: Изменение без изображений
- **WHEN** images не переданы
- **THEN** ответ 400

#### Scenario: Товар чужого магазина
- **WHEN** товар принадлежит магазину другого продавца
- **THEN** ответ 404

#### Scenario: Признаки блокировки сохраняются
- **WHEN** у товара storeBlocked = true, и владелец изменяет его после разблокировки магазина, но до обработки STORE_UNBLOCKED
- **THEN** storeBlocked остается true до обработки события

**Observation**: OBS-02, OBS-03, OBS-09
