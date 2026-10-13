# Design

## Context

Мотивация и решения пользователя — в proposal.md.

Текущее состояние (проверено по коду):
- `StoreUpdateRequest {storeName (обязательно), description, email,
  phoneNumber}`; `StoreServiceImpl.updateStore`: магазин владельца (иначе
  404), проверка статусов (BLOCKED → 409), `trim` имени, проверка
  уникальности без учета самого магазина, `updateFromDTO` (null не меняет
  поле), нормализация телефона, `saveAndFlush` с переводом нарушения
  `uq_stores_seller_store_name` в 409. Ответ 200 с магазином.
- Заявки продавца (`SellerApplication`, `ModeratorController`,
  `ModeratorServiceImpl`) задают образец модерации: PENDING → APPROVED /
  REJECTED, 409 для рассмотренной заявки, 403 для своей заявки,
  обязательная причина отказа (`ReviewSellerApplicationRequest`).
- `Seller.stores` — `cascade = ALL, orphanRemoval = true`: удаление продавца
  удаляет магазины каскадом ORM.
- Kafka и notification-service для заявок продавца используют
  `seller-events-topic`; новый тип события там без доработки
  notification-service ушел бы в DLT. В этом change событий нет.
- Внешние клиенты `PUT /stores/{id}`: не найдены (frontend `/stores` не
  вызывает, catalog-service — только `GET /stores/{id}`).

## Goals / Non-Goals

**Goals:** ни одно поле магазина не меняется без одобрения модератора;
поведение модерации совпадает с заявками продавца; без Kafka и новых сервисов.

**Non-Goals:** письма, отмена заявки, очистка полей null, модерация создания.

## Decisions

### D1. Сущность `StoreChangeRequest`

Таблица `store_change_requests` (создается `ddl-auto`):

| Поле | Назначение |
|---|---|
| `id` | IDENTITY |
| `store` | `@ManyToOne` на `Store`, NOT NULL |
| `keycloakUserId` | автор (для запрета самомодерации) |
| `storeName`, `description`, `email`, `phoneNumber` | предлагаемые значения; null — не менять |
| `status` | `StoreChangeRequestStatus {PENDING, APPROVED, REJECTED}` |
| `moderatorComment` | причина отказа |
| `createdAt`, `updatedAt` | даты (`@CreatedDate`, `@LastModifiedDate`) |

- В `Store` — `@OneToMany(mappedBy = "store", cascade = REMOVE)`: заявки
  удаляются вместе с магазином при `DELETE /stores/{id}` и при каскадном
  удалении магазинов продавца.
- Частичный уникальный индекс `uq_store_change_requests_pending ON
  store_change_requests (store_id) WHERE status = 'PENDING'` в `schema.sql`
  (`CREATE UNIQUE INDEX IF NOT EXISTS`): последняя защита от двух
  PENDING-заявок при одновременной подаче. **Применение на существующих БД:**
  `schema.sql` выполняется при каждом старте после Hibernate
  (`spring.sql.init.mode: always`, `defer-datasource-initialization: true`);
  при `ddl-auto: update` (стенд, `.env.seller`) первый старт новой версии
  создает таблицу, затем индекс; повторный старт пропускает команду. Так же
  применяются `uq_stores_seller_store_name` и `uq_sellers_seller_name`. Если
  индекс создать нельзя, сервис не стартует (`continue-on-error: false`), то
  есть БД не остается без ограничения молча. Условие — `ddl-auto: update`,
  как для остальных команд `schema.sql`. Нарушение
  (`23505` и имя индекса, `UniqueViolations`) переводится в 409.
- Длины колонок совпадают с ограничениями API (имя 100, описание 255,
  комментарий модератора 500 — явный `length`, чтобы не повторить
  `seller-onboarding` OBS-16).

### D2. Подача через `PUT /stores/{id}`

- Тело и валидация прежние (`StoreUpdateRequest`).
- Порядок: магазин владельца (404) → статусы продавца и магазина (409,
  `Blocked store restrictions`, `Current seller resolution`) → нет PENDING-
  заявки этого магазина (409 `Store with id: [<id>] already has a pending
  change request.`) → `trim` имени и уникальность среди других магазинов
  продавца (409) → нормализация телефона → сохранение заявки PENDING.
- Ответ 202 с заявкой: `{id, storeId, storeName, description, email,
  phoneNumber, status, moderatorComment, createdAt, updatedAt}`. Магазин не
  меняется.

### D3. Модерация

- `ModeratorStoreChangeController` (`/moderator/store-change-requests`, роль
  MODERATOR): `GET /pending` (по возрастанию createdAt), `GET /{id}`,
  `POST /{id}/approve` (204), `POST /{id}/reject` с телом
  `{moderatorComment}` (обязательно, ≤ 500; переиспользуется
  `ReviewSellerApplicationRequest`), ответ 200.
- Общие проверки: заявка не найдена → 404; своя заявка (keycloakUserId =
  `sub` модератора) → 403; не PENDING → 409.
- Одобрение в одной транзакции. Магазин читается с блокировкой строки
  (`@Lock(PESSIMISTIC_WRITE)`, `SELECT ... FOR UPDATE`): одновременное
  удаление магазина ждет одобрения или выполняется до него — тогда магазина
  нет, ответ 404, ничего не меняется (без блокировки обновление удаленной строки
  дало бы 500). Далее: текущие статусы продавца и магазина ACTIVE
  (иначе 409, заявка PENDING); имя заявки снова проверяется на уникальность
  (за время рассмотрения его мог занять другой магазин продавца) — иначе 409;
  непустые поля переносятся в магазин; `saveAndFlush` с переводом нарушения
  индекса в 409 (как в `StoreServiceImpl`); заявка → APPROVED.
- Отклонение не проверяет статусы магазина и продавца (модератор может
  отклонить заявку заблокированного продавца).

### D3b. Синхронизация операций с магазином

Проверено по коду: `deleteStore` читает магазин без блокировки, загружает
коллекцию заявок и удаляет ее элементы по одному, затем магазин. Блокировка
только при одобрении не закрывает гонку «подача и удаление»: заявка,
вставленная после загрузки коллекции, не попадет в каскад, и `DELETE` магазина
упадет на внешнем ключе (23503, 500). То же при окончательном удалении
продавца.

Поэтому все операции, меняющие магазин или его заявки, читают магазин через
`@Lock(PESSIMISTIC_WRITE)` (`SELECT ... FOR UPDATE`):
- подача (`PUT /stores/{id}`) — `findByIdAndSellerIdForUpdate`;
- одобрение — `findByIdForUpdate`;
- удаление владельцем (`DELETE /stores/{id}`) — `findByIdAndSellerIdForUpdate`;
- окончательное удаление продавца — `findAllBySellerIdForUpdate` в
  `writeStorePurgedEvents` до каскадного удаления.

Операции с одним магазином выполняются по очереди; операция, начавшая ждать
блокировку до удаления, после него не находит магазин и отвечает 404 без
изменений. Отклонение заявки магазин не меняет и не блокирует; если магазин
удален, заявки уже нет (404).

### D3a. Контракт `PUT /stores/{id}` и OpenAPI

- Метод контроллера помечается `@ResponseStatus(HttpStatus.ACCEPTED)` и
  возвращает DTO заявки. Статического описания OpenAPI в проекте нет;
  springdoc (`springdoc-openapi-starter-webmvc-ui`) строит описание по
  аннотациям, поэтому код 202 и схема ответа попадут в него автоматически.
  Эндпоинты `/v3/api-docs` и `/swagger-ui` сейчас закрыты
  `SpringSecurityConfig` (`anyRequest().denyAll()`) — это не меняется.
- Вызовы `PUT /stores/{id}` в проекте (поиск по коду, тестам, `*.http`,
  frontend): только `StoreController`, `StoreService`, `StoreServiceImpl` и
  тесты `StoreServiceImplTest` (5 тестов `updateStore_*`) и
  `StoreRequestValidationTest` (валидация `StoreUpdateRequest`, не меняется).
  frontend, catalog-service (`SellerClient` — только `GET`), user-service и
  файлы `*.http` его не вызывают. Тесты `updateStore_*` переписываются под
  создание заявки.

### D4. Просмотр владельцем

`GET /stores/{id}/change-requests` — заявки магазина владельца по убыванию
createdAt (404 для чужого магазина). Доступен и при блокировке (просмотр).

### D5. Тесты

Unit (Mockito, `StoreMapperImpl`): подача (202, PENDING, магазин не меняется,
нормализация, trim, уникальность, одна PENDING, блокировки), модерация
(одобрение применяет только непустые поля, повторная проверка имени и
статусов, 403/404/409, отклонение с причиной), просмотр, контроллеры
standalone MockMvc (коды и тело ошибок).

## Risks / Trade-offs

- [BREAKING ответа `PUT /stores/{id}`] → клиентов нет; отмечено в proposal.
- [Одна PENDING-заявка на магазин] → новое изменение до решения модератора
  невозможно (отмена заявки — вне change).
- [Блокировка строки магазина при одобрении] → короткое ожидание для
  параллельных операций с тем же магазином; только при одобрении.
- [Без писем модератор узнает о заявках только из очереди] → решение
  пользователя.

## Migration Plan

Деплой seller-service (`ddl-auto` создаст таблицу) и api-gateway. Существующие
магазины не меняются. Откат: старый код таблицу игнорирует.

## Открытые вопросы

| # | Вопрос | По умолчанию |
|---|---|---|
| Q1 | Принимать ли заявку, если значения совпадают с текущими (ничего не меняется) | принимать (подтверждено) |
| Q2 | Показывать ли в ответе магазина признак «есть заявка на рассмотрении» | нет (подтверждено) |
