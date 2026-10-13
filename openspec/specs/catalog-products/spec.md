# catalog-products Specification

## Purpose
Baseline (as-is) поведения catalog-service по работе с товарами: публичная
выдача и поиск товаров, получение товаров по списку идентификаторов для других
сервисов, создание, изменение и удаление товаров продавцом с проверкой владения
магазином и формат товара в ответах. Спецификация описывает фактическое
текущее поведение кода, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79` **с учетом
  незакоммиченного изменения рабочей копии** в `ProductMapper.toDTO`: удален
  `@Mapping(target = "id", ignore = true)`, поэтому поле `id` в ответах товара
  заполнено. В коммите `78cec79` без этого изменения `id` в ответах равен null.
- Метод: статический анализ кода и пересобранного из рабочей копии
  `ProductMapperImpl`. На dev-стенде проверены формат ответа `GET /products`
  (содержит `id`), состав документа в MongoDB и индексы коллекции `products`.
  Тестов для товаров нет.
- Хранение: MongoDB, коллекция `products`. Изображения хранятся в MinIO,
  механика хранения описана в `catalog-media`. Проверка владения магазином
  использует контракт из `seller-stores`.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код сейчас. Изменять его
  нужно отдельным change.
- Пути указаны относительно catalog-service. Через api-gateway они доступны без
  изменений, через nginx — с префиксом `/api`.
- Профиль `generate-data` (`ProductInitializer`, `ProductGenerator`) генерирует
  тестовые товары. Это инструмент наполнения данными, он вне scope spec.

## Requirements

### Requirement: Product API access
catalog-service SHALL разрешать без аутентификации `GET /products`,
`GET /products/{id}`, `GET /products/productByName/{name}` и
`POST /products/by-ids`. Запросы `POST /products`, `PUT /products/{id}`,
`DELETE /products/{id}` и `GET /products/stores/{storeId}/products` SHALL
требовать JWT с realm-ролью SELLER. Любые другие запросы к `/products/**` SHALL
отклоняться.

#### Scenario: Анонимная выдача
- **WHEN** клиент без токена вызывает `GET /products`
- **THEN** catalog-service отвечает 200

#### Scenario: Создание без роли SELLER
- **WHEN** клиент с JWT только с ролью USER вызывает `POST /products`
- **THEN** catalog-service отвечает 403

### Requirement: Product creation
catalog-service SHALL принимать `POST /products` как `multipart/form-data` с
полями storeId, name, description, brand, price, images, categories,
characteristic, tag, status и SHALL создавать товар, привязанный к магазину
storeId, с загруженными изображениями в порядке передачи. Ответ SHALL иметь
код 201 и тело `{message, time}` без данных товара.

#### Scenario: Успешное создание
- **WHEN** продавец отправляет валидный запрос со storeId своего магазина и существующими категориями
- **THEN** товар сохраняется, изображения загружаются в хранилище товаров
- **AND** ответ 201 содержит message "The product was successfully created."

**Observation**: OBS-02, OBS-11

### Requirement: Product field validation
catalog-service SHALL отклонять с кодом 400 создание товара, если отсутствует
storeId или price, пусты name, description или brand, description длиннее 500
или brand длиннее 50 символов, пусты images, categories или tag, отсутствует
characteristic, хотя бы одно изображение не JPEG (`catalog-media`) или status не
равен ACTIVE, INACTIVE, ARCHIVED или DRAFT. Нижняя граница price SHALL NOT
проверяться.

#### Scenario: Нет изображений
- **WHEN** images не переданы
- **THEN** ответ 400 с message, начинающимся с "Validation failed:"

#### Scenario: Отрицательная цена
- **WHEN** price = -1, остальные поля валидны
- **THEN** валидация проходит

**Observation**: OBS-08

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

### Requirement: Product article number and default status
При создании catalog-service SHALL присваивать товару articleNumber, равный
новому случайному UUID, игнорируя articleNumber из запроса. Если status не
передан, SHALL устанавливаться статус DRAFT.

#### Scenario: articleNumber из запроса
- **WHEN** продавец передает articleNumber "ART-1"
- **THEN** у созданного товара articleNumber — сгенерированный UUID, а не "ART-1"

#### Scenario: Статус по умолчанию
- **WHEN** status не передан
- **THEN** товар создается со статусом DRAFT

**Observation**: OBS-01

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

### Requirement: Store products listing
catalog-service SHALL возвращать по `GET /products/stores/{storeId}/products`
все товары магазина без пагинации, только если магазин принадлежит текущему
продавцу по контракту `seller-stores`. Иначе SHALL возвращаться 404.

#### Scenario: Свой магазин
- **WHEN** продавец запрашивает товары своего магазина
- **THEN** ответ содержит все товары с этим storeId

#### Scenario: Удаленный магазин
- **WHEN** магазин удален в seller-service, а его товары остались
- **THEN** ответ 404

**Observation**: OBS-15

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

### Requirement: Product images on update
При изменении catalog-service SHALL формировать новый список изображений товара
из переданных файлов в порядке передачи. Если хеш SHA-256 файла совпадает с
хешем текущего изображения товара, SHALL повторно использоваться это
изображение, иначе файл SHALL загружаться. Изображения, отсутствующие в новом
списке, SHALL NOT удаляться из хранилища.

#### Scenario: Повторная передача той же картинки
- **WHEN** продавец передает файл, совпадающий по содержимому с текущим изображением
- **THEN** в товаре остается прежнее изображение без повторной загрузки

#### Scenario: Удаление изображения из списка
- **WHEN** продавец передает только одно из двух текущих изображений
- **THEN** у товара остается одно изображение, второе остается в хранилище

**Observation**: OBS-10

### Requirement: Product deletion
catalog-service SHALL по `DELETE /products/{id}` возвращать 404 для
несуществующего товара, затем SHALL проверять владение магазином товара (иначе
404) и SHALL удалять товар с ответом 204. Изображения товара SHALL NOT
удаляться из хранилища.

#### Scenario: Удаление своего товара
- **WHEN** продавец удаляет товар своего магазина
- **THEN** товар удален, ответ 204

**Observation**: OBS-10, OBS-15

### Requirement: Product response format
Ответ чтения товара SHALL содержать поля id, name, description, brand, price,
articleNumber, imagesUrl (публичные URL изображений в порядке хранения),
categories (список `{id, name}`), characteristic (`{attributes}`), tag и status
(имя значения). storeId, даты создания и изменения в ответ SHALL NOT входить.

#### Scenario: Формат товара
- **WHEN** запрашивается товар с одной категорией
- **THEN** categories содержит объект с id и именем этой категории, а id товара заполнен

**Observation**: OBS-05, OBS-07

### Requirement: Product error responses
Ошибки товарных эндпоинтов SHALL обрабатываться так же, как в
`catalog-categories` (`Category error responses`). Недоступность seller-service
SHALL давать 503 с телом `{message, time}`.

#### Scenario: Ошибка валидации
- **WHEN** запрос создания нарушает ограничение
- **THEN** ответ 400 с телом `{message, time}`

**Assumption**: ASM-02, ASM-03

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

### Requirement: Store events consumer error handling
Ошибка обработки сообщения из `store-events-topic` (например, некорректный
JSON) SHALL повторяться 3 раза с паузой 1 секунда, после чего сообщение SHALL
публиковаться в `store-events-topic.DLT`. Недоступность Kafka при старте
catalog-service SHALL NOT мешать старту сервиса и работе REST API: потребитель
SHALL повторять подключение, пока Kafka не станет доступна.

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения не является JSON-конвертом
- **THEN** после 4 попыток (1 + 3 повтора) сообщение публикуется в `store-events-topic.DLT`

#### Scenario: Старт без Kafka
- **WHEN** catalog-service стартует при недоступной Kafka
- **THEN** сервис стартует и отвечает на `GET /products`, а потребитель начинает работу после появления Kafka

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

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. articleNumber из запроса не используется.** Поле принимается в
  запросе, но всегда заменяется на сгенерированный UUID.
- **OBS-02. Ответы не содержат товар.** Ответы на создание и изменение не
  содержат сам товар и его id.
- **OBS-03. PUT отвечает 201.** Изменение товара возвращает 201 Created
  (`@ResponseStatus(CREATED)` на `updateProduct`).
- **OBS-04. Статус не фильтрует выдачу.** _Устранено change `seller-store-lifecycle`:
  публичные API отдают только доступные товары (ACTIVE, магазин и продавец не
  заблокированы); недоступный товар по id видит только владелец._ Было:
  DRAFT, INACTIVE и ARCHIVED были видны анонимным пользователям.
- **OBS-05. Даты не сохраняются.** У `Product.dateCreate` и `dateUpdate` нет
  аудита, и код их не заполняет. В документах MongoDB этих полей нет (проверено
  на dev-стенде).
- **OBS-06. Индексы не созданы.** Аннотации `@Indexed` (уникальный
  `article_number`, `store_id`) не применяются: автосоздание индексов Spring
  Data MongoDB не включено. У коллекции `products` есть только индекс `_id`
  (проверено на dev-стенде).
- **OBS-07. storeId не возвращается.** По ответу нельзя узнать магазин
  товара.
- **OBS-08. Цена без нижней границы.** Допускаются 0 и отрицательные значения.
- **OBS-09. PUT требует все данные.** Нужно заново передавать все поля и все
  изображения, частичное изменение невозможно.
- **OBS-10. Изображения не удаляются.** Изображения не удаляются из MinIO ни
  при замене, ни при удалении товара.
- **OBS-11. Загрузка до сохранения.** Изображения загружаются в MinIO до
  сохранения товара. Если сохранение не прошло, объекты остаются в хранилище.
- **OBS-12. by-ids без ограничений.** Эндпоинт публичный, размер массива не
  ограничен, отсутствующие id молча пропускаются, порядок не сохраняется.
- **OBS-13. Статус категорий не проверяется.** При создании и изменении
  проверяется только существование категорий.
- **OBS-14. Пустой поиск дает 404.** Поиск по имени при отсутствии совпадений
  возвращает 404, а не пустой список.
- **OBS-15. Товары удаленного магазина.** _Устранено change `seller-store-lifecycle`:
  удаление магазина или продавца физически удаляет его товары
  (`STORE_PURGED`); `STORE_DELETED` больше не публикуется и архивирует
  товары только для старых сообщений._ Было: товары удаленного магазина
  оставались в `ARCHIVED` и были видны в публичной выдаче.

## Assumptions (unverified)
- **ASM-01. Формат страницы.** Он соответствует прямой сериализации `PageImpl`.
  Spring Data помечает этот формат как нестабильный, при обновлении он может
  измениться.
- **ASM-02. Прочие ответы seller-service.** Ответ 401 от seller-service
  (например, истек токен), таймауты (подключение 2 с, ответ 5 с) и сетевые
  ошибки, вероятно, не обрабатываются специально и дают 500.
- **ASM-03. Лимиты multipart.** Превышение лимитов multipart (5MB на файл,
  30MB на запрос), вероятно, дает 413 без тела `{message, time}`.
- **ASM-04. Параметр sort.** Он, вероятно, принимает любое поле товара
  (например, `sort=price,desc`) по стандартной обработке Spring Data.

## Open Questions
- **OQ-01. OBS-01–OBS-15.** Какие из них дефекты?
- **OQ-02. Публичная выдача.** _Решено change `seller-store-lifecycle`: только доступные товары._
- **OQ-03. Контракт by-ids.** Нужно ли сохранять порядок, сообщать об
  отсутствующих id или ограничивать размер запроса?
- **OQ-04. Товары удаленного магазина.** _Решено change `seller-store-lifecycle`: товары
  удаляются физически._
