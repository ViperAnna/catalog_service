# Spec Delta

## Purpose

Заказы покупателя в order-service: оформление из корзины по ценам каталога на
момент оформления, неизменяемые снимки позиций, история и просмотр заказов,
жизненный цикл статусов с единственным владельцем и события заказа.

## ADDED Requirements

### Requirement: Order access and ownership
Эндпоинты заказов SHALL требовать JWT с ролью USER (без токена — 401, без
роли — 403). Заказ SHALL принадлежать пользователю с `sub`, оформившему его.
Заказ другого пользователя SHALL быть неотличим от несуществующего: 404 с
телом `{message, time}` для чтения и отмены.

#### Scenario: Чужой заказ
- **WHEN** пользователь B вызывает `GET /orders/{id}` для заказа пользователя A
- **THEN** ответ 404

#### Scenario: Отмена чужого заказа
- **WHEN** пользователь B вызывает `POST /orders/{id}/cancel` для заказа пользователя A в статусе CREATED
- **THEN** ответ 404, статус заказа не меняется

#### Scenario: Некорректный идентификатор
- **WHEN** вызывается `GET /orders/not-a-uuid`
- **THEN** ответ 400

### Requirement: Order checkout from cart
`POST /orders` без тела SHALL создавать заказ из всех позиций корзины текущего
пользователя и возвращать 201 с заголовком `Location: /orders/{orderId}` и
заказом в формате `GET /orders/{orderId}`. Цены, названия, артикулы и магазины
SHALL браться из catalog-service в момент оформления, сумма SHALL
рассчитываться сервером. Тело запроса, если передано, SHALL игнорироваться.

#### Scenario: Успешное оформление
- **WHEN** в корзине доступные товары P (100.00 × 2) и Q (50.00 × 1)
- **THEN** ответ 201, заказ в статусе CREATED с `totalAmount` = 250.00 и двумя позициями

#### Scenario: Корзина очищается
- **WHEN** заказ успешно оформлен
- **THEN** `GET /cart` возвращает пустую корзину

#### Scenario: Пустая корзина
- **WHEN** пользователь без позиций в корзине вызывает `POST /orders`
- **THEN** ответ 409, заказ не создается

#### Scenario: Сумма от клиента
- **WHEN** `POST /orders` вызывается с телом `{"totalAmount": 1}`
- **THEN** тело игнорируется, `totalAmount` заказа рассчитан по ценам каталога

#### Scenario: Цена изменилась после просмотра корзины
- **WHEN** покупатель видел в `GET /cart` цену 100.00, а к оформлению цена в каталоге стала 120.00
- **THEN** заказ создается с `unitPrice` = 120.00

### Requirement: Checkout validation of cart items
Если хотя бы одна позиция корзины недоступна по правилам `shopping-cart`
`Unavailable cart items`, оформление SHALL отклоняться с 409, сообщение SHALL
перечислять `productId` недоступных позиций, а заказ, событие и изменения
корзины SHALL NOT создаваться.

#### Scenario: Товар стал недоступен
- **WHEN** в корзине P и Q, а магазин Q заблокирован
- **THEN** ответ 409 с `productId` Q в message, корзина содержит P и Q, заказов нет

### Requirement: Atomic order creation
Заказ, все его позиции, событие ORDER_CREATED в outbox и очистка корзины
SHALL сохраняться в одной транзакции PostgreSQL. При любой ошибке SHALL NOT
оставаться заказ без позиций, позиции без заказа, событие без заказа или
очищенная корзина без заказа. Вызов catalog-service SHALL NOT считаться частью
этой транзакции.

#### Scenario: Ошибка при сохранении позиции
- **WHEN** сохранение одной из позиций нарушает ограничение БД
- **THEN** заказ, позиции и событие не сохранены, корзина не изменена

### Requirement: Duplicate checkout protection
Параллельные или повторные `POST /orders` одного пользователя SHALL создавать
не более одного заказа и одного события ORDER_CREATED из одного состава
корзины. Запрос, обработанный после успешного оформления, SHALL получать 409
(корзина пуста) и SHALL NOT создавать заказ. Заголовок `Idempotency-Key`
SHALL игнорироваться до реализации change `order-checkout-idempotency`.

#### Scenario: Двойное нажатие
- **WHEN** два `POST /orders` пользователя с непустой корзиной выполняются одновременно
- **THEN** один ответ 201, второй 409, у пользователя один новый заказ и одно событие ORDER_CREATED

#### Scenario: Ключ идемпотентности в MVP
- **WHEN** повторный `POST /orders` после успешного оформления содержит тот же `Idempotency-Key`
- **THEN** ответ 409, новый заказ не создается

### Requirement: Order item snapshots
Каждая позиция заказа SHALL хранить снимок на момент оформления: `productId`,
`storeId`, название (до 255 символов), артикул (может отсутствовать), цену за
единицу, количество и `lineTotal` = цена × количество. `totalAmount` SHALL
равняться сумме `lineTotal`. Описание, бренд, категории, характеристики и
изображения SHALL NOT сохраняться.

#### Scenario: Состав снимка
- **WHEN** оформляется товар P магазина 3 с названием "Camera", артикулом "ART-1" и ценой 100.00 в количестве 2
- **THEN** позиция содержит productId P, storeId 3, productName "Camera", articleNumber "ART-1", unitPrice 100.00, quantity 2, lineTotal 200.00

### Requirement: Order history independent of catalog
Чтение заказа и истории SHALL NOT обращаться к catalog-service и
seller-service. Изменение цены или названия, блокировка, архивация или
физическое удаление товара или магазина после оформления SHALL NOT менять
заказ и его позиции. Текущая доступность товара в заказе SHALL NOT
показываться.

#### Scenario: Изменение цены после оформления
- **WHEN** после оформления цена товара в каталоге изменилась
- **THEN** `GET /orders/{id}` возвращает прежнюю цену и сумму

#### Scenario: Товар удален из каталога
- **WHEN** после оформления магазин удален, и его товары физически удалены
- **THEN** заказ возвращается с прежними названием, ценой и `storeId` позиций

#### Scenario: Каталог недоступен
- **WHEN** catalog-service недоступен и вызывается `GET /orders/{id}`
- **THEN** ответ 200 с заказом

### Requirement: No stock reservation
order-service SHALL NOT проверять, резервировать или уменьшать остатки
товаров и SHALL NOT уведомлять продавцов о заказе. Отмена заказа SHALL NOT
изменять данные catalog-service. Количество в заказе ограничивается только
пределом 1–99 на позицию.

#### Scenario: Количество больше наличия
- **WHEN** покупатель оформляет 99 единиц товара, у продавца которого фактически меньше
- **THEN** заказ создается, данные каталога не меняются

### Requirement: Multi-store orders
Один заказ SHALL допускать позиции товаров разных магазинов. Статус и отмена
SHALL относиться ко всему заказу. Разделения заказа на заказы магазинов SHALL
NOT выполняться.

#### Scenario: Товары двух магазинов
- **WHEN** в корзине товары магазинов 3 и 5
- **THEN** создается один заказ с позициями, у которых `storeId` 3 и 5

### Requirement: Order immutability
Состав, цены, сумма и владелец оформленного заказа SHALL NOT изменяться ни
одним API. Статус SHALL меняться только переходами `Order status lifecycle`.
Эндпоинтов изменения и удаления заказа SHALL NOT быть.

#### Scenario: Попытка изменить заказ
- **WHEN** клиент вызывает `PUT /orders/{id}` или `DELETE /orders/{id}`
- **THEN** ответ 405, заказ не меняется

### Requirement: Order retrieval
`GET /orders/{orderId}` SHALL возвращать 200 и заказ
`{id, status, totalAmount, itemsCount, createdAt, updatedAt, cancelledAt, items}`,
где `items` — снимки позиций `{productId, storeId, productName, articleNumber,
unitPrice, quantity, lineTotal}`. Отдельного эндпоинта состава заказа SHALL NOT
быть.

#### Scenario: Получение своего заказа
- **WHEN** владелец запрашивает свой заказ
- **THEN** ответ 200 с позициями и суммой

### Requirement: Order history listing
`GET /orders` SHALL возвращать заказы текущего пользователя страницей
`{content, page, size, totalElements, totalPages}` без позиций. Параметры:
`page` от 0, `size` от 1 до 50 (по умолчанию 20), `sort` — `createdAt` или
`totalAmount` с `asc|desc` (по умолчанию `createdAt,desc`). Иные значения SHALL
давать 400.

#### Scenario: История по умолчанию
- **WHEN** у пользователя 25 заказов и вызывается `GET /orders`
- **THEN** ответ содержит 20 самых новых заказов, `totalElements` = 25, `totalPages` = 2

#### Scenario: Сортировка по неразрешенному полю
- **WHEN** вызывается `GET /orders?sort=userId,asc`
- **THEN** ответ 400

#### Scenario: Только свои заказы
- **WHEN** у пользователей A и B есть заказы, и A вызывает `GET /orders`
- **THEN** ответ содержит только заказы A

### Requirement: Order status lifecycle
Заказ SHALL создаваться в статусе CREATED. В MVP SHALL существовать только
статусы CREATED и CANCELLED и единственный переход CREATED → CANCELLED по
запросу покупателя-владельца. CANCELLED SHALL быть терминальным. Любой другой
переход, включая повторную отмену, SHALL отклоняться с 409 без изменений и без
события.

#### Scenario: Повторная отмена
- **WHEN** владелец вызывает `POST /orders/{id}/cancel` для заказа в статусе CANCELLED
- **THEN** ответ 409, статус, `cancelledAt` и `updatedAt` не меняются, событие не создается

### Requirement: Order cancellation by customer
`POST /orders/{orderId}/cancel` без тела SHALL переводить заказ владельца из
CREATED в CANCELLED, заполнять `cancelledAt`, записывать событие
ORDER_CANCELLED в outbox в той же транзакции и возвращать 200 с заказом.
Параллельные отмены SHALL давать одну отмену и одно событие.

#### Scenario: Отмена созданного заказа
- **WHEN** владелец отменяет заказ в статусе CREATED
- **THEN** ответ 200, статус CANCELLED, создано одно событие ORDER_CANCELLED

#### Scenario: Параллельная отмена
- **WHEN** два запроса отмены одного заказа выполняются одновременно
- **THEN** один ответ 200, второй 409, событие ORDER_CANCELLED одно

### Requirement: Single owner of order state
order-service SHALL быть единственным компонентом, изменяющим данные заказа.
Другие сервисы SHALL NOT изменять БД `order_service` напрямую; будущие
переходы по результатам оплаты или доставки SHALL выполняться order-service
по их событиям или командам с проверкой допустимости перехода и публикацией
собственного события. В MVP служебных переходов и эндпоинтов SHALL NOT быть.

#### Scenario: Нет служебного API
- **WHEN** пользователь с ролью ADMIN вызывает любой путь order-service, кроме `/cart/**` и `/orders/**`
- **THEN** ответ 403

#### Scenario: Смена статуса только через order-service
- **WHEN** статус заказа меняется
- **THEN** изменение выполнено order-service, и в той же транзакции записано событие `ORDER_<STATUS>` в outbox

### Requirement: Order events publishing
При создании и отмене заказа order-service SHALL записывать в outbox ровно
одно событие ORDER_CREATED или ORDER_CANCELLED с уникальным `eventId` в одной
транзакции с изменением заказа и публиковать его в `order-events-topic` по
контракту `integration-events` `order-events-topic message contract` только
после фиксации транзакции.

#### Scenario: Откат оформления
- **WHEN** транзакция оформления откатывается
- **THEN** событие ORDER_CREATED не публикуется

#### Scenario: Kafka недоступна
- **WHEN** Kafka недоступна при оформлении
- **THEN** заказ создается (201), событие публикуется после восстановления Kafka

### Requirement: Catalog unavailability on checkout
Если catalog-service недоступен, отвечает ошибкой или не ответил за 5 секунд,
`POST /orders` SHALL возвращать 503 с телом `{message, time}` без создания
заказа и без изменения корзины.

#### Scenario: Таймаут каталога
- **WHEN** catalog-service не отвечает дольше 5 секунд при оформлении
- **THEN** ответ 503, корзина не изменена, заказов нет
