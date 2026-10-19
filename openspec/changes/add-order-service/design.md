# Design

## Context

Мотивация и решения пользователя U1–U8 — в proposal.md. Ниже — факты кода,
конфигурации и specs (ветка `feature/SAM1-14`, коммит `f4e49cd`), которые
определяют подход.

### Существующие механизмы (проверено по коду)

- **Сборка.** Корневой `pom.xml` — только reactor (`<modules>`), каждый сервис
  наследует `spring-boot-starter-parent` 3.3.4 и импортирует BOM Spring Cloud
  2023.0.3. Во всех сервисах `<skipTests>true</skipTests>`, Jenkins собирает
  `mvn clean package -DskipTests`. Testcontainers подключен только в
  user-service.
- **Схема БД.** `ddl-auto: update` + идемпотентный `schema.sql`
  (`spring.sql.init.mode: always`, `defer-datasource-initialization: true`) для
  частичных и функциональных индексов и CHECK-ограничений (seller-service).
  Flyway/Liquibase нет.
- **БД.** Один PostgreSQL 16, по базе на сервис, базы создает
  `postgres/init.sql` только при пустом томе `pgdata`. Учетная запись общая
  для всех сервисов (`access-control` `Shared infrastructure credentials`).
- **Безопасность.** Stateless resource servers; роли из `realm_access.roles`
  через копию `KCRoleConverter`; catalog и seller проверяют `iss` и запрещают
  неописанные пути. Идентичность — `sub`. Сервисных токенов нет.
- **Keycloak (realm-export).** `verifyEmail: true`; у `frontend-client` в
  default scopes есть `email` — access token содержит `email` и
  `email_verified`. IdP `google` — `trustEmail: true`, `yandex` —
  `trustEmail: false`. seller-service уже берет email из claim `email`.
- **Каталог.** `POST /products/by-ids` публичный, возвращает только доступные
  товары, молча пропускает отсутствующие, не ограничен по размеру. Нижняя
  граница цены не проверяется. Валюты и остатков нет. `storeId` в ответе нет.
  Товары удаленного магазина и их изображения удаляются физически
  (`STORE_PURGED`).
- **Ошибки.** Тело `{message, time}` (`time` в формате `yyyy-MM-dd HH:mm:ss`).
- **Kafka.** Outbox seller-service: `outbox_events`, публикатор
  `@Scheduled(fixedRate = 3000)` + `@Transactional`, до 100 записей, синхронная
  отправка с ожиданием 10 с, остановка цикла на ошибке, неизвестный тип →
  processed без публикации, конверт `{aggregateType, eventType, payload}`,
  обезличивание после подтвержденной публикации. Блокировки строк нет:
  несколько экземпляров публикуют одно и то же (`integration-events` OBS-07).
  notification-service: `DefaultErrorHandler` (3 повтора по 1 с, затем
  `<topic>.DLT`), ошибки SMTP перехватываются без повторов — уведомление
  теряется (`integration-events` OBS-15); дедупликации нет; конверт
  типизирован `SellerEventType`. Retention топиков по умолчанию 7 дней
  (`integration-events` ASM-07). В compose у каждого сервиса задан
  `container_name`, поэтому `docker compose up --scale` для него невозможен.
- **Gateway.** Статические маршруты без префикса версии; CORS-методы без PATCH.
- **Деплой.** compose dev/prod; prod-образы `viperanna/<service>:<TAG>`,
  env-файлы на сервере; Jenkins хранит перечень сервисов в четырех картах.

## Goals / Non-Goals

**Goals:**
- Сервис по образцу seller-service без новых библиотек.
- Сумма заказа вычисляется только сервером по данным каталога на момент
  оформления; заказ — неизменяемый снимок.
- Два параллельных оформления одной корзины не создают двух заказов.
- События заказа не теряются молча: каждое либо обработано, либо пропущено по
  явной политике с записью в лог, либо находится в DLT.
- Бизнес-правила — в классах без Spring, HTTP и БД (D15).

**Non-Goals:**
- Оплата, возвраты, доставка, остатки и резервирование, служебные переходы,
  реализация `Idempotency-Key`, тестовые классы, админский просмотр заказов,
  уведомления продавцам, frontend.
- Исправление наблюдений других specs, кроме явно перечисленных в delta.

## Decisions

### D1. Модуль и соглашения

`order-service/` в reactor, `groupId ru.klimovich`, пакет
`ru.klimovich.order_service`, порт `8086`, `SPRING_APPLICATION_NAME=order-service`
→ Eureka `ORDER-SERVICE`. Зависимости — подмножество seller-service: web,
validation, actuator, oauth2-resource-server, data-jpa, postgresql,
spring-kafka, eureka-client, webflux (WebClient), mapstruct, springdoc, lombok,
test-стартеры. gRPC и commons-codec не нужны. `<skipTests>true</skipTests>`
как во всех модулях (U7). Dockerfile — копия seller-service.

*Альтернатива:* общий parent POM — отклонена, меняет все модули.

### D2. Идентификаторы пользователя и модель данных

- Пользователь в order-service — только `sub` JWT (`keycloakUserId`),
  `user_id VARCHAR(64)`. Внутренний UUID профиля user-service не
  используется и не запрашивается. Email в таблицах order-service не
  хранится (только в outbox до публикации, D9).
- Товар: `product_id VARCHAR(64)` (ObjectId строкой). Магазин: `store_id
  BIGINT`. Свои сущности: `UUID`.
- Деньги: `NUMERIC(12,2)` для цены, `NUMERIC(14,2)` для `line_total` и
  `total_amount`. Валюта одна и неявная, поля валюты нет, пока его нет в
  каталоге.

```
carts        id PK, user_id UNIQUE NOT NULL, created_at, updated_at
cart_items   id PK, cart_id FK->carts ON DELETE CASCADE, product_id NOT NULL,
             quantity NOT NULL CHECK (quantity BETWEEN 1 AND 99),
             created_at, updated_at, UNIQUE (cart_id, product_id)
orders       id PK, user_id NOT NULL, status NOT NULL CHECK IN ('CREATED','CANCELLED'),
             total_amount NOT NULL CHECK (> 0), items_count NOT NULL CHECK (> 0),
             created_at, updated_at, cancelled_at NULL, version NOT NULL
             INDEX (user_id, created_at DESC)
order_items  см. D7
outbox_events  id UUID PK, aggregate_type, aggregate_id, event_type, payload TEXT,
             created_at, processed; INDEX (processed, created_at)
```

FK только внутри БД сервиса. Корзина и заказы не связаны. API удаления
заказов нет. CHECK и индексы создает `schema.sql` (идемпотентно), таблицы —
Hibernate. В корзине не более 50 позиций (ограничивает и тело `by-ids`).

### D3. Контракт с catalog-service

Единственный вызов — `POST {catalog-service.url}/products/by-ids` с JSON-
массивом id и JWT пользователя; `@LoadBalanced WebClient`, таймауты 2 с / 5 с.

Позиция **доступна**, если товар вернулся, `price` не `null`, `> 0`, не
больше 2 знаков после запятой и помещается в `NUMERIC(12,2)`, `name` не пуст и
`storeId` не `null`. Иначе — недоступна; причину order-service не различает.
5xx, таймаут, сетевая ошибка или 4xx на корректный запрос → 503.

catalog-service добавляет `storeId` в ответ товара. Совместимость: user-service
десериализует в свой DTO из 5 полей (лишние поля уже игнорируются), frontend
поле не использует. Требование `catalog-products` `Product response format`
меняется через MODIFIED.

### D4. Корзина

- Корзина создается лениво (`INSERT ... ON CONFLICT (user_id) DO NOTHING`).
  `GET /cart` без корзины возвращает пустую корзину, не создавая строку.
- Изменяющие операции и оформление берут блокировку строки корзины
  (`SELECT ... FOR UPDATE`, `lock_timeout` 5 с → 409).
- Цена не хранится. `GET /cart` запрашивает каталог; недоступные позиции
  возвращаются с `available = false`, без цены, не входят в `total`.
- `POST /cart/items` → 201 `{productId, quantity}` (недоступен → 404, уже в
  корзине → 409, 51-я позиция → 409); `PUT /cart/items/{productId}` → 200
  `{productId, quantity}` без вызова каталога; `DELETE /cart/items/{productId}`
  → 204 (нет позиции → 404); `DELETE /cart` → 204 идемпотентно.

*Альтернатива:* повторный `POST` суммирует количество — отклонена.

### D5. Оформление заказа

```
POST /orders   (без тела; тело, если передано, игнорируется)
TX (READ COMMITTED)
  SELECT cart FOR UPDATE                 -- сериализует оформления пользователя
  [order-checkout-idempotency: поиск заказа по (user_id, Idempotency-Key)]
  нет корзины или нет позиций -> 409
  by-ids(catalog)  ошибка/таймаут -> 503, ничего не изменено
  есть недоступные позиции -> 409 со списком productId, корзина без изменений
  total = sum(unitPrice * quantity)       -- только серверные данные
  INSERT orders(CREATED) + order_items (снимки D7)
  INSERT outbox_events(ORDER_CREATED)
  DELETE cart_items корзины
COMMIT -> 201, Location: /orders/{id}, тело — заказ
```

Подтверждения суммы клиентом нет (U5): если цена изменилась между просмотром
корзины и оформлением, заказ создается по новой цене, которая видна в ответе.
Повторный параллельный запрос ждет блокировку, затем видит пустую корзину и
получает 409 — это защита от дубля, а не идемпотентность (D12). Вызов каталога
внутри транзакции держит блокировку до 5 с (R2).

*Альтернатива:* оптимистическая версия корзины — отклонена: повтор вызова
каталога при конфликте, гарантия та же.

### D6. Контракт статусов и владение состоянием

order-service — единственный владелец состояния заказа (U3). Статус меняется
только доменным методом перехода `transition(order, target, initiator)`,
который сверяется с таблицей переходов. Прямой записи статуса, суммы или
позиций нет ни в API, ни в сервисном слое; другие сервисы не имеют доступа к
БД `order_service` по контракту (технически учетная запись PostgreSQL общая —
R10).

Таблица переходов MVP:

| Из | В | Инициатор | Результат |
|---|---|---|---|
| — | CREATED | CUSTOMER (`POST /orders`) | 201, ORDER_CREATED |
| CREATED | CANCELLED | CUSTOMER-владелец (`POST /orders/{id}/cancel`) | 200, `cancelledAt`, ORDER_CANCELLED |
| CANCELLED | CANCELLED | CUSTOMER (повторная отмена) | 409, без изменений и событий |
| любой | иной | любой | запрещено (нет API) |

- Повторная отмена — 409, а не 200: клиент различает «отменено сейчас» и
  «уже было отменено»; событие не дублируется.
- Параллельные отмены: optimistic lock (`version`), проигравший получает 409,
  событие одно.
- Каждый переход пишет событие в outbox в той же транзакции.

Расширение (не реализуется, ориентир для будущих change):

```
 [CREATED] --PAYMENT_SUCCEEDED (payment-events-topic)--> [PAID]
 [CREATED] --PAYMENT_FAILED / таймаут оплаты (SYSTEM)--> [CANCELLED]
 [PAID] --SHIPMENT_DISPATCHED (delivery-events-topic)--> [SHIPPED]
 [SHIPPED] --SHIPMENT_DELIVERED--> [DELIVERED]
 [CREATED|PAID] --CUSTOMER cancel--> [CANCELLED] (+ refund — отдельный процесс)
```

- Внешний сервис публикует факт своей операции (событие) или вызывает
  команду order-service; order-service потребляет его, проверяет переход,
  меняет статус и публикует собственное событие `ORDER_<STATUS>`.
- Входящие события будут обрабатываться идемпотентно по их `eventId`
  (таблица обработанных входящих событий в `order_service`); событие,
  требующее недопустимого перехода, не меняет заказ и логируется как
  конфликт с orderId и eventId.
- Новые статусы добавляются в enum, CHECK в `schema.sql`
  (`DROP CONSTRAINT IF EXISTS` + `ADD`, как в seller-service) и в контракт
  `integration-events` до первой публикации.

Последствия одного заказа на несколько магазинов (U2): статус общий, отмена
только целиком; доставка по магазинам потребует подзаказов (`order_id`,
`store_id`, свой статус), `store_id` в позициях позволяет выделить их без
потери данных.

### D7. Снимок позиции, история и остатки

Позиция заказа:

| Поле | Источник | Обязательность |
|---|---|---|
| `product_id` | корзина | NOT NULL |
| `store_id` | каталог `storeId` | NOT NULL |
| `product_name` | каталог `name` | NOT NULL, до 255 символов (длиннее — обрезается) |
| `article_number` | каталог `articleNumber` | NULL допустим |
| `unit_price` | каталог `price` | NOT NULL, `> 0` |
| `quantity` | корзина | 1–99 |
| `line_total` | `unit_price * quantity` | NOT NULL, `> 0` |

Не сохраняются: описание, бренд, категории, характеристики, изображения
(объекты MinIO удаляются при `STORE_PURGED` — ссылка в истории стала бы
битой), названия магазина и продавца (их нет в каталоге).

История: чтение заказа никогда не вызывает каталог и seller-service. Изменение
цены или названия, блокировка, архивация или физическое удаление товара или
магазина не меняют заказ. Текущая доступность товара в заказе не
показывается.

Остатки и резервирование: в каталоге нет количества на складе, поэтому
order-service не проверяет и не резервирует остатки, не уменьшает их и не
уведомляет продавца. Возможна продажа большего количества, чем есть у
продавца; это ограничение MVP. Отмена заказа ничего не возвращает «на склад».

### D8. Событие заказа и eventId

- Топик `order-events-topic`, 3 партиции, RF 1, `NewTopic` в order-service.
  Ключ — `orderId`. Конверт `{aggregateType: "ORDER", eventType, payload}`.
- Payload (JSON-строка):

```
{ "eventId": "<UUID>", "occurredAt": "2026-10-14T12:00:00",
  "orderId": "<UUID>", "keycloakUserId": "<sub>", "status": "CREATED",
  "totalAmount": 1234.50, "itemsCount": 3, "email": "<или null>" }
```

- `eventId` = id записи outbox, присваивается при записи в одной транзакции с
  изменением заказа и не меняется при повторных публикациях той же записи.
  Одному бизнес-изменению соответствует ровно одна запись и один `eventId`.
  `occurredAt` = `created_at` записи. `eventId` сохраняется в обезличенном
  payload.
- Повторная публикация одной и той же записи возможна: подтверждение Kafka
  получено, а фиксация `processed = true` не прошла; перезапуск во время
  цикла. Сообщения-дубли имеют одинаковые `eventId` и payload.
- Публикатор — по образцу seller-service (fixedRate 3 с, до 100 записей по
  `created_at`, ожидание 10 с, остановка на ошибке, неизвестный тип →
  processed без публикации с ERROR в логе). `@EnableScheduling`.
- Порядок: события одного заказа публикуются в порядке `created_at` одним
  публикатором и попадают в одну партицию.

### D9. Персональные данные в событиях

- Минимальный состав: `keycloakUserId` (`sub`, псевдонимный идентификатор
  для связи с аккаунтом) и `email` (единственный канал уведомления). Других
  персональных данных нет: имени, телефона, адреса, состава заказа, названий
  товаров.
- Источник email однозначен: claim `email` access token запроса,
  инициирующего событие (оформление или отмена), только если claim
  `email_verified` = `true`; иначе `email = null`. Адрес не запрашивается у
  user-service и не хранится в таблицах заказов. Если пользователь сменил
  email между оформлением и отменой, письмо об отмене уходит на новый
  подтвержденный адрес.
- Хранение: в `outbox_events` order-service email хранится только до
  подтвержденной публикации, затем payload заменяется на
  `{orderId, eventId, redacted: true}` в той же транзакции, что и
  `processed = true`. В Kafka (`order-events-topic` и `.DLT`) сообщение
  хранится до истечения retention топика (по умолчанию 7 дней); удаление
  отдельных сообщений не поддерживается. notification-service email не
  сохраняет (таблица обработанных событий D10 — без email).
- Логирование: ни order-service, ни notification-service не пишут в лог email,
  payload событий, JWT и заголовок `Authorization`. Допустимые поля логов:
  `orderId`, `eventId`, `eventType`, topic/partition/offset, причина ошибки.

*Альтернативы:* (a) без email — notification-service не может найти адрес: у
него нет хранилища адресов, Telegram-привязка ключуется внутренним id профиля,
а не `sub`; (b) запрос email у user-service — новый синхронный контракт.

### D10. notification-service: классификация, идемпотентность, DLT

Классификация сообщения `order-events-topic`:

| Класс | Признак | Действие | Наблюдаемость |
|---|---|---|---|
| Уведомляемое | eventType ∈ {ORDER_CREATED, ORDER_CANCELLED}, payload валиден | письмо, затем запись `eventId` | INFO: eventId, orderId, eventType |
| Известное без уведомления | eventType в явном списке `ORDER_NOTIFICATION_DISABLED` (в MVP пуст) | без письма, запись `eventId` | INFO «skipped by policy» |
| Без получателя | уведомляемое, `email` null или пуст | без письма, запись `eventId` | WARN: eventId, orderId |
| Дубликат | `eventId` уже обработан | без письма | INFO «duplicate» |
| Неизвестное | eventType не входит ни в один список | DLT сразу, без повторов | ERROR: topic, partition, offset, eventType, причина |
| Некорректное | не JSON; нет eventType или payload; aggregateType ≠ ORDER; нет или невалиден `eventId`/`orderId` | DLT сразу, без повторов | ERROR, как выше |
| Временная ошибка | ошибка SMTP, ошибка БД | 3 повтора по 1 с, затем DLT | ERROR при каждой попытке, без email |

- Ни одно сообщение не пропускается без записи в лог; неизвестные и
  некорректные не теряются — они остаются в `order-events-topic.DLT`.
- Ошибка SMTP для событий заказа — повторяемая (в отличие от
  `user-created-events-topic` и `seller-events-topic`, где она
  перехватывается): с идемпотентностью по `eventId` повтор безопасен.
- Неповторяемые случаи — отдельный класс исключения, добавленный в
  `addNotRetryableExceptions` общего обработчика рядом с
  `UnknownApplicationTypeException`; поведение других топиков не меняется.
- `order-events-topic.DLT` объявляется явно бином `NewTopic` (3 партиции,
  RF 1) в notification-service, чтобы не зависеть от автосоздания
  (`integration-events` ASM-02). Потребителя DLT нет. Процедура разбора:
  обновить потребитель (новый тип) или исправить причину, затем
  переотправить сообщение из DLT в `order-events-topic` с тем же ключом;
  идемпотентность по `eventId` исключает повторные письма для уже
  обработанных событий.
- Идемпотентность: таблица `processed_order_events (event_id UUID PK,
  order_id, event_type, processed_at)` в БД `notification_service`.
  Обработка: проверка `event_id` → отправка письма → вставка записи. Запись
  вставляется только после успешной отправки или осознанного пропуска.
  Нарушение PK при вставке (параллельный дубль) — дубликат, без ошибки.
- Ограничение на дубли писем: гарантия «не более одного письма на eventId»
  кроме сбоя между успешной отправкой SMTP и фиксацией записи (падение
  процесса, ошибка БД) — тогда возможно повторное письмо. Потеря письма
  без записи в лог или DLT исключена. Таблица не очищается в MVP (без
  персональных данных, рост ограничен числом событий).
- Telegram для заказов не используется.

### D11. Несколько экземпляров публикатора

Публикатор не блокирует строки outbox: два экземпляра order-service
опубликуют одни и те же записи (дубли с одинаковым `eventId`), а при
параллельной публикации порядок событий одного заказа может нарушиться.
Ограничение MVP: order-service работает в одном экземпляре. Это обеспечивается
compose (`container_name` исключает масштабирование) и фиксируется в
спецификации. Горизонтальное масштабирование требует отдельного change
(лидер-блокировка публикатора, например `pg_try_advisory_lock`, или
`FOR UPDATE SKIP LOCKED` с сохранением порядка по ключу). Дубли от
переотправки уже обрабатываются потребителем по `eventId` (D10).

### D12. Контракт идемпотентности оформления (реализация — `order-checkout-idempotency`)

Согласованный контракт. В этом change не реализуется: заголовок
`Idempotency-Key` в MVP игнорируется, колонки не создаются.

API:
- `POST /orders` принимает необязательный заголовок `Idempotency-Key`:
  1–64 символа `[A-Za-z0-9_-]`; иное → 400 без изменений.
- Ключ уникален в пределах пользователя (`sub`); одинаковые ключи разных
  пользователей независимы.
- Первый успешный запрос с ключом → 201, `Location`, заказ.
- Повтор с тем же ключом после успеха → 200, тот же `Location` и тот же заказ
  в текущем состоянии (в том числе CANCELLED); каталог не вызывается, корзина
  не меняется, событие не создается. Тела у `POST /orders` нет, поэтому
  конфликт «тот же ключ — другие параметры» невозможен; состав корзины на
  момент повтора не учитывается.
- Конкурентные запросы с одним ключом: блокировка корзины сериализует их;
  второй после фиксации первого получает 200 с тем же заказом. Если первый
  завершился ошибкой, второй выполняется как новый.
- Запрос с ключом, завершившийся 4xx/5xx без создания заказа, ключ не
  закрепляет.
- Без ключа — поведение MVP (D5).
- Ключ хранится вместе с заказом бессрочно (как и заказ).

БД:
- `orders.idempotency_key VARCHAR(64) NULL`.
- `CREATE UNIQUE INDEX uq_orders_user_idempotency_key ON orders (user_id,
  idempotency_key) WHERE idempotency_key IS NOT NULL` в `schema.sql`.
- Порядок в транзакции: блокировка корзины → поиск заказа по
  `(user_id, idempotency_key)` → при отсутствии — шаги D5 с записью ключа.
  Нарушение уникального индекса (последняя защита) → повторное чтение и 200
  с найденным заказом.

Совместимость с MVP: в D5 зарезервировано место проверки ключа после
блокировки корзины; requirement MVP `Duplicate checkout protection` не
запрещает возврат существующего заказа, поэтому change идемпотентности
только добавляет requirement.

### D13. Безопасность и API

- `SecurityFilterChain`: ERROR-dispatch и `/actuator/health` открыты;
  `/cart`, `/cart/**`, `/orders`, `/orders/**` — роль USER; остальное deny
  all. Проверка подписи по `JWT_JWK_SET_URI` и `iss` по `JWT_ISSUER_URI`.
- Все запросы к данным фильтруются по `user_id = sub`: чужой заказ — 404.
  Корзина адресуется только текущим пользователем.
- Ошибки `{message, time}`; 400 — валидация, некорректный UUID и параметры
  пагинации; 503 — недоступен каталог.
- `GET /orders`: `page` от 0, `size` 1–50 (по умолчанию 20), `sort`
  `createdAt` или `totalAmount` (`asc|desc`, по умолчанию `createdAt,desc`);
  ответ — DTO `{content, page, size, totalElements, totalPages}`.

### D14. Интеграция в инфраструктуру

- api-gateway: маршрут `order-service`:
  `Path=/cart,/cart/**,/orders,/orders/**` → `lb://ORDER-SERVICE`. CORS не
  меняется.
- `postgres/init.sql`: `CREATE DATABASE order_service;`.
- `.env.order` (dev): `SPRING_APPLICATION_NAME`, `SERVER_PORT=8086`,
  datasource `order_service`, `SPRING_JPA_*`, `SPRING_KAFKA_BOOTSTRAP_SERVERS`,
  `JWT_JWK_SET_URI`, `JWT_ISSUER_URI`, `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE`,
  `CATALOG_SERVICE_URL=http://CATALOG-SERVICE`.
- compose dev и prod: сервис `order-service` с `container_name`, env-файлами,
  `depends_on` postgres healthy, discovery healthy, kafka started; prod-образ
  `viperanna/order-service:${ORDER_SERVICE_TAG}`.
- Jenkinsfile: `ORDER_IMAGE` и `order-service` во всех четырех картах.

### D15. Тестопригодность без тестов (U7)

Расчет (`CartPricing`), доступность позиции (`CatalogItemPolicy`), таблица
переходов (`OrderTransitions`), построение и обезличивание payload
(`OrderEventFactory`), классификация событий в notification-service
(`OrderEventClassifier`) — чистые классы без Spring. Каталог за интерфейсом
`CatalogClient`, время через `Clock`, зависимости через конструктор.

### D16. Тестирование — change `add-order-service-tests`

Создан отдельным change (только planning). Сборка этого change с
`skipTests=true` подтверждает лишь компиляцию; корректность поведения
подтверждается только тестами того change.

## Risks / Trade-offs

- [R1] Новая БД не появится на существующих томах `pgdata` → ручной
  `CREATE DATABASE order_service` по запросу пользователя (tasks 11.1).
- [R2] Вызов каталога внутри транзакции оформления держит соединение и
  блокировку корзины до 5 с → лимит 50 позиций, таймауты, `lock_timeout`.
- [R3] Цена может измениться между просмотром корзины и оформлением → заказ
  по цене на момент оформления, цена видна в ответе; подтверждение изменения
  цены — отдельное бизнес-требование (U5).
- [R4] `by-ids` не различает причины недоступности → одна формулировка;
  удаленные товары остаются в корзине до удаления покупателем.
- [R5] Повторное письмо возможно только при сбое между отправкой SMTP и
  фиксацией `processed_order_events` (D10).
- [R6] Один экземпляр order-service (D11); масштабирование — отдельный change.
- [R7] Нет подтвержденного email (`email_verified` ≠ true или нет claim) →
  письмо не отправляется (WARN), заказ оформляется.
- [R8] Access token живет 300 с; специальной обработки 401 от каталога нет
  (→ 503).
- [R9] Тестов нет до `add-order-service-tests` → функциональность не
  считается проверенной.
- [R10] Общая учетная запись PostgreSQL технически позволяет другим сервисам
  писать в `order_service` → правило владения соблюдается по контракту;
  отдельная учетная запись — отдельный change (`access-control` OBS-09).
- [R11] Email в Kafka хранится до retention (7 дней), в DLT — до retention
  DLT → удаление по запросу пользователя данных из топиков не
  поддерживается.
- [R12] Нет остатков и резервирования → возможна перепродажа (D7).

## Migration Plan

1. Слить catalog-service (`storeId`) — совместимо с текущими потребителями.
2. По запросу пользователя на существующих стендах: `CREATE DATABASE
   order_service;` под `POSTGRES_USER`.
3. По запросу пользователя: `.env.order` на prod-сервере.
4. Развернуть notification-service (новый потребитель и DLT-топик) не позже
   order-service: при `auto-offset-reset` по умолчанию (`latest`, ASM-03)
   новая подписка пропустит события, опубликованные до нее.
5. Развернуть order-service, затем api-gateway (маршрут).
6. Откат: убрать маршрут из gateway и остановить order-service; данные и
   топики остаются; `storeId` в каталоге можно оставить.
