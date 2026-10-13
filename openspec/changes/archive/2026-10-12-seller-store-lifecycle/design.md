# Design

## Context

Мотивация и решения пользователя (Q1–Q9, R1–R5) — в proposal.md.

### Текущие механизмы (проверено по коду)

- **Удаление магазина владельцем:** `StoreServiceImpl.deleteStore` удаляет
  строку и пишет outbox `STORE/STORE_DELETED {storeId, sellerId}` →
  `store-events-topic` (ключ storeId) → catalog `StoreEventConsumer` →
  `StoreProductsArchiver` (товары → ARCHIVED). Неизвестный eventType
  пропускается. Ошибки: 3 повтора, `store-events-topic.DLT`; после DLT
  обработка следующих сообщений того же ключа продолжается.
- **Outbox:** `OutboxPublisher` (`@Transactional`, раз в 3 с) публикует до 100
  записей `processed = false` по `createdAt`, ждет подтверждения Kafka
  (`send().get(10 s)`), затем ставит `processed = true`; на ошибке отправки
  останавливается (порядок сохраняется). Неизвестный тип помечается processed
  без публикации. Обработанные записи хранятся.
- **Удаление продавца:** `approveDeletion` ставит DELETED, gRPC
  `RemoveSellerRole` внутри транзакции; магазины, заявки, outbox остаются.
- **Проверки статуса:** `validateSellerActive` — только при подаче UPDATE и
  DELETION (409). `approveUpdate` статус не проверяет. Операции с магазинами
  статус не проверяют. catalog проверяет только владение (`GET /stores/{id}`).
- **Создание товара:** категории и владение → загрузка изображений в MinIO
  (секунды) → `productRepo.save` (insert).
- **Изменение товара:** `findById` → владение → загрузка новых изображений →
  `productMapper.updateProductFromDTO` → `productRepo.save` — **полная
  перезапись документа**, загруженного до загрузки изображений.
- **Изображения:** бакеты `products` и `categories` раздельны, ключ
  `<sha256>_<имя>`, дедупликация внутри бакета (`catalog-media` OBS-05).
- **frontend** отправляет `Authorization: Bearer` во всех запросах при входе
  и использует публичные `GET /products`, поиск и `GET /products/{id}`.
- **Избранное:** `by-ids` для карточки, `GET /products/{id}` при добавлении;
  Kafka-consumer в user-service нет.
- **Персональные данные продавца:** `sellers`, `stores`, `seller_applications`;
  payload событий `SELLER_APPLICATION` (`keycloakUserId`, `sellerName`,
  `email`, у SELLER_REJECTED — `moderatorComment`); сообщения
  `seller-events-topic` и DLT; лог notification-service
  `Unable to send email to {}` с адресом получателя. notification-service
  использует из payload только `sellerName`, `email`, `moderatorComment` и
  `applicationType` (для письма) и `applicationId` (в логах); `keycloakUserId`
  не используется. События `STORE` содержат только id.

## Goals / Non-Goals

**Goals:** бизнес-правила без новых коллекций; идемпотентное и повторяемое
окончательное удаление; минимальное хранение персональных данных без потери
надежности доставки.

**Non-Goals:** покупка и заказы, автоматическая очистка старых данных,
уведомления о блокировке, удаление сообщений из Kafka.

## Decisions

### D1. Состояния и доступность

```
 Seller:  ACTIVE <-- block (moderator) / unblock (moderator, or approved UNBLOCK) --> BLOCKED
             |                                                                        |
             +--------------- approved DELETION (from ACTIVE or BLOCKED) -------------+--> row deleted

 Store:   ACTIVE <-- block / unblock (moderator only) --> BLOCKED
             |
             +--- DELETE /stores/{id} (owner; seller and store ACTIVE) ---------------------> row deleted
             +--- approved seller DELETION (store in any status) ----------------------------> row deleted

 Product available <=> product.status = ACTIVE AND NOT storeBlocked AND NOT sellerBlocked
```

`StoreStatus` и `SellerStatus` — `{ACTIVE, BLOCKED, DELETED}`, в БД хранятся
только ACTIVE и BLOCKED. Колонка `stores.status`: `ddl-auto` добавит
nullable, `schema.sql` заполнит `ACTIVE`, задаст `DEFAULT` и `NOT NULL`
(идемпотентно).

### D2. Признаки блокировки в товарах

- `Product.storeBlocked`, `Product.sellerBlocked` (Boolean, отсутствие =
  false). События задают абсолютное значение:
  `updateMulti({storeId}, set <признак>)`. Повтор безопасен, уровни
  независимы, порядок по магазину сохраняется (ключ storeId, одна партиция,
  outbox по порядку).
- **Только события меняют признаки.** Создание и изменение товара признаки не
  пишут (создание — без полей, изменение — без затрагивания полей).

### D3. Гонки создания и изменения товара (R1)

Обозначения: B — фиксация блокировки в seller-service, P — обработка ее
события в catalog (P > B), C1 — проверка статуса до загрузки изображений,
S — запись товара, C2 — повторная проверка после записи.

**Создание.** Порядок: C1 → загрузка → S (insert) → C2.
- C2 сообщает не-ACTIVE (магазин или продавец) или магазин не найден →
  catalog удаляет только что созданный документ и отвечает 409 (404 для
  удаленного магазина). Товар не существует, поэтому не может стать доступным.
- seller-service недоступен на C2 → документ удаляется, ответ 503.

| Порядок | Результат |
|---|---|
| B < C1 | C1 → 409, ничего не создано |
| C1 < B < C2, P < S (событие обработано до записи) | C2 видит BLOCKED → документ удален |
| C1 < B < C2, P > S | C2 видит BLOCKED → документ удален (событие затем ничего не найдет) |
| C2 < B | P > B > C2 > S → событие обновит признаки уже записанного товара |
| удаление магазина (STORE_PURGED) в окне | C2 → 404 → документ удален; иначе STORE_PURGED удалит его |

Одной повторной проверки достаточно, если товар не пишет признаки сам:
единственный случай, когда событие «проходит мимо» товара, — P < S, и тогда
B < P < S < C2, т.е. C2 обязательно видит блокировку. Если блокировку сняли
до C2, товар остается доступным — это корректно. Изображения, загруженные
для удаленного документа, остаются в бакете (как сейчас, `catalog-media`
OBS-06).

**Изменение — дополнительная защита.** Полная перезапись документа (`save`)
документом, прочитанным до обработки события, сбросила бы `storeBlocked`
(lost update). Минимальная мера: изменение товара записывается частичным
обновлением (`$set` только редактируемых полей: name, description, brand,
price, images, categories, characteristic, tag, status) — признаки
блокировки и storeId не перезаписываются. Повторная проверка после изменения
не нужна: признаки управляются только событиями и после частичного
обновления остаются корректными; изменение, начатое до блокировки, может
завершиться после нее, но товар уже скрыт признаком.

**Удаление товара** (`DELETE /products/{id}`) удаляет документ — гонок с
признаками нет.

### D4. Безопасная повторная отправка из DLT (R1)

После попадания сообщения в `store-events-topic.DLT` следующие события того
же магазина продолжают обрабатываться, поэтому переотправка старого
сообщения может откатить более новое состояние. Процедура для оператора:
1. Найти исходную запись в `outbox_events` seller-service (по storeId и
   eventType, `processed = true`) и все записи того же aggregateId с более
   поздним `created_at` (id записи — UUID и порядка не задает; publisher
   отправляет записи по `created_at`).
2. Определить текущее состояние в seller-service: статус магазина и
   продавца; существует ли магазин.
3. Решение:
   - `STORE_PURGED` — переотправлять всегда (идемпотентно, окончательно);
   - событие блокировки или разблокировки — переотправлять, только если для
     того же признака (store или seller) после него нет обработанных записей
     и оно совпадает с текущим статусом. Если есть более новое событие или
     статус не совпадает — исходное сообщение не переотправлять; вместо этого
     переотправить последнее по `created_at` событие этого признака, если оно
     совпадает с текущим статусом;
   - если магазин удален — переотправлять только `STORE_PURGED`.
4. Переотправка: установить `processed = false` у выбранной записи outbox
   (payload событий `STORE` не обезличивается, D9) — publisher отправит ее
   снова; либо отправить сообщение из DLT в `store-events-topic` с тем же
   ключом.
5. Проверка: признаки товаров магазина в MongoDB совпадают с текущими
   статусами в seller-service.

### D5. Публичный и владельческий доступ

- Публичные `GET /products`, по категории, поиск, `by-ids` — только
  доступные товары.
- `GET /products/{id}`: доступный — всем; недоступный — только при JWT с
  ролью SELLER и успешной проверке владения (`GET /stores/{id}`), иначе 404.
  frontend не меняется. Черновики владельца пропадают из общих списков, но
  открываются по ссылке.

### D6. Ограничения по статусу (Q5, R3, R5)

| Операция | Продавец BLOCKED | Магазин BLOCKED |
|---|---|---|
| просмотр своих данных, магазинов, товаров, заявок | можно | можно |
| `POST /stores` | 409 | — |
| `PUT /stores/{id}`, `DELETE /stores/{id}` | 409 | 409 |
| `POST`/`PUT`/`DELETE /products` | 409 | 409 |
| подача UPDATE | 409 | можно |
| одобрение UPDATE модератором | 409, заявка остается PENDING | можно |
| отклонение UPDATE модератором | можно | можно |
| подача и одобрение DELETION | можно | можно |
| подача UNBLOCK | можно (только BLOCKED) | — |

- Каждая изменяющая операция проверяет текущий статус в момент выполнения,
  а не статус на момент подачи заявки.
- **Окончательное удаление заблокированного магазина** (R3): новых действий не
  добавляется. Доступны существующие пути: модератор разблокирует магазин,
  после чего владелец удаляет его обычным `DELETE`; либо продавец подает
  DELETION, и одобрение удаляет все его магазины, включая заблокированные.
  Отдельного «удаления модератором» нет (решение N2).
- Роль SELLER не снимается; код 409 — по аналогии с `SELLER_IS_NOT_ACTIVE`.
- catalog получает `status` и `sellerStatus` в ответе `GET /stores/{id}`.

### D7. Заявка UNBLOCK (R4)

- Только для продавца: `POST /seller-applications/unblock` с `{reason}`,
  только BLOCKED, без PENDING-заявки. Одобрение: ACTIVE, `SELLER_UNBLOCKED`
  для магазинов, SELLER_APPROVED `applicationType = UNBLOCK`. Блокировки
  магазинов не снимаются; разблокировка магазина — только действием
  модератора. Одобрение переиспользует `SellerModerationService.unblockSeller`.
- **Схема (найдено при реализации):** Hibernate при создании таблицы
  `seller_applications` поставил CHECK
  `seller_applications_application_type_check` по значениям enum
  (`REGISTRATION`, `UPDATE`, `DELETION`); `ddl-auto: update` его не меняет, и
  вставка `UNBLOCK` дала бы ошибку БД. `schema.sql` пересоздает ограничение с
  `UNBLOCK` (`DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT`, идемпотентно).
- Письмо (notification-service), тема `Seller account unblocked`:
  ```
  Hello, %s.

  Your request to unblock the seller account has been approved.

  You can manage your stores and products again. Stores blocked separately
  remain blocked until a moderator unblocks them.

  ------------------

  Thank you for using our service!
  ```
  `%s` — sellerName, как в остальных письмах; email, ИНН и иные данные не
  включаются.

### D8. Окончательное удаление

```
 seller-service, one PG transaction:
   DELETE /stores/{id} (owner, seller and store ACTIVE)   approve DELETION (seller ACTIVE or BLOCKED)
     outbox STORE_PURGED{store}                            collect ids of seller's applications
     delete store row                                      outbox STORE_PURGED for each store
                                                           outbox SELLER_APPROVED{DELETION}
                                                           redact payload of seller's published
                                                             SELLER_APPLICATION outbox records (D9)
                                                           delete stores, seller, applications
                                                           gRPC RemoveSellerRole (failure -> rollback)
                 |
                 v  store-events-topic, key storeId
 catalog, STORE_PURGED(storeId), idempotent:
   1. delete objects of `products` bucket of store's products unless a product
      of another store has an image with the same hash
   2. delete product documents of store
```

- `STORE_DELETED` больше не публикуется, catalog архивирует по нему только
  старые сообщения.
- Повтор: транзакция seller-service атомарна; outbox повторяет публикацию;
  catalog — 3 повтора, DLT, переотправка по D4 (для `STORE_PURGED` безопасна
  всегда).

### D9. Персональные данные в outbox и логах (R2)

- **Что нужно для обработки:** notification-service использует `sellerName`,
  `email`, `moderatorComment`, `applicationType`, `applicationId`;
  `keycloakUserId` не используется, но остается в контракте (решение N1).
- **Обезличивание после подтвержденной публикации:** в той же транзакции
  publisher, после `send().get()` и перед `processed = true`, payload записи
  `SELLER_APPLICATION` заменяется на `{"applicationId": <id>, "redacted": true}`.
  Если commit не прошел, запись остается `processed = false` с исходным
  payload и будет отправлена снова — гарантия at-least-once не меняется.
  Записи, помеченные processed без публикации (неизвестный тип), тоже
  обезличиваются.
- **Что теряется:** повторная публикация уже отправленного события
  `SELLER_APPLICATION` из outbox (`processed = false`) становится невозможной
  — такая операция не входит в действующие гарантии; письма при сбое
  доставки остаются в `seller-events-topic.DLT`. События `STORE` не
  обезличиваются: персональных данных в них нет, а их повтор нужен для D4.
- **Удаление продавца:** опубликованные записи его заявок обезличиваются в
  транзакции удаления; ожидающие (в том числе SELLER_APPROVED{DELETION},
  нужное для прощального письма) сохраняют payload до публикации и
  обезличиваются сразу после нее. Если Kafka недоступна, данные остаются в
  ожидающих записях до публикации — без этого письмо нельзя отправить.
- **Записи до этого change:** уже обработанные записи с персональными данными
  остаются; их однократная обезличка — ручная операция (tasks, 8.3).
- **Kafka:** опубликованные сообщения и DLT хранят данные до истечения
  retention топиков; изменение outbox их не удаляет.
- **Логи:** notification-service при ошибке отправки письма продавцу
  логирует applicationId и тип события вместо адреса получателя. Лог
  приветственного письма пользователю (`notifications`) — вне этого change.

### D10. Существующие записи Seller в статусе DELETED

Ручная процедура (не при старте): 1) проверка — DELETED-продавцы, их магазины,
товары в MongoDB, заявки; 2) резервная копия — `pg_dump` `sellers`, `stores`,
`seller_applications`, `outbox_events`, `mongodump` `products`, список
объектов MinIO; 3) очистка в транзакции на продавца — `STORE_PURGED` в outbox
для магазинов, обезличка его записей outbox, удаление магазинов, заявок,
Seller; повторный запуск ничего не находит; 4) проверка результата.

**Проверка на стенде (2026-10-12, только чтение):** `sellers` — 1 запись,
ACTIVE (id 2), 1 магазин (id 1); продавцов в статусе DELETED нет;
`seller_applications` — REGISTRATION APPROVED 1, REJECTED 1; `outbox_events` —
4 записи `SELLER_APPLICATION`, все processed (с персональными данными, см.
задачу 7.2); MongoDB `catalog_db.products` — 1 товар ACTIVE с `store_id = 1`
(поля документа в snake_case через `@Field`: `store_id`, `article_number`),
одно изображение, ключ объекта MinIO — последний сегмент `url`; полей
признаков блокировки нет. Процедура D10 на стенде не нужна; на production
проверку повторить.

### D11. Повторная регистрация

После удаления записей Seller, магазинов и заявок ограничения
`Registration application constraints` и уникальные ограничения БД не
находят прежних данных; новая REGISTRATION проходит обычную модерацию.

## Согласованность сервисов

| Связь | Обеспечение |
|---|---|
| seller-service → catalog: блокировки | `STORE_*` и `SELLER_*` в `store-events-topic`, признаки в товарах |
| seller-service → catalog: проверка изменений | `status`, `sellerStatus` в `GET /stores/{id}`; повторная проверка после создания |
| seller-service → catalog: удаление | `STORE_PURGED`, идемпотентный обработчик |
| seller-service → notification | `SELLER_APPROVED` с `UNBLOCK`; обезличка после публикации не затрагивает доставку |
| api-gateway | маршруты `/moderator/sellers/**`, `/moderator/stores/**` |
| user-service | без изменений: 404 от каталога при добавлении, `by-ids` без недоступных |

## Risks / Trade-offs

- [Гонки создания и изменения товара] → D3; остаточный риск — изображения
  удаленного при повторной проверке документа остаются в бакете.
- [DLT вне порядка] → процедура D4, автоматической защиты нет.
- [Общие изображения и параллельная загрузка] → объект может быть удален сразу
  после проверки ссылок; низкая вероятность.
- [Блокировка продавца с PENDING UPDATE] → продавец не может подать UNBLOCK или
  DELETION, пока модератор не отклонит UPDATE или не разблокирует продавца
  (решение N3, правило одной PENDING-заявки не меняется).
- [Персональные данные в Kafka и старых записях outbox] → D9.

## Migration Plan

1. Проверка D10, шаг 1.
2. Деплой catalog-service, notification-service, api-gateway.
3. Деплой seller-service.
4. Ручные операции D10 и обезличка старых записей outbox — после резервной
   копии, по решению.

## Решения по оставшимся вопросам

| # | Вопрос | Решение |
|---|---|---|
| N1 | Удалять ли `keycloakUserId` из payload `SELLER_APPLICATION` | нет, контракт событий не меняется; поле обезличивается в outbox вместе с остальным payload после публикации (D9), в Kafka и DLT остается до истечения retention |
| N2 | Отдельная операция модератора для окончательного удаления заблокированного магазина | не нужна; пути — разблокировка модератором и обычный `DELETE` или удаление продавца (D6) |
| N3 | Заблокированный продавец с PENDING UPDATE и правило одной PENDING-заявки | оставить: UNBLOCK и DELETION подаются после того, как модератор отклонит UPDATE или разблокирует продавца |

Открытых вопросов нет.
