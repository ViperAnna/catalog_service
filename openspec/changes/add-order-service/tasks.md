# Tasks

Тестовые классы в этом change не создаются (U7). `skipTests=true` сохраняется,
настройки тестов существующих модулей не меняются. Пункты «Проверка»
подтверждают только компиляцию, сборку и соответствие кода design/specs при
ревью; корректность поведения ими НЕ подтверждается и проверяется в change
`add-order-service-tests`. Отчет apply не должен утверждать, что
функциональность работает или протестирована.

## 1. catalog-service: storeId в ответе товара

- [x] 1.1 Добавить `storeId` в `ProductResponse` и маппинг `Product.storeId` → `storeId` в `ProductMapper.toDTO` (D3); `toEntity` не менять. Проверка (компиляция): `mvn -q -f catalog-service/pom.xml package`; в сгенерированном `ProductMapperImpl` `storeId` берется из `Product`
- [x] 1.2 Проверить совместимость потребителей: user-service `ProductResponse` и `WishlistResponseBuilder` не зависят от набора полей, frontend не использует `storeId`. Проверка (ревью): `git grep -n storeId user-service frontend/src` — изменений не требуется

## 2. order-service: каркас модуля

- [x] 2.1 Модуль `order-service` (`pom.xml` по образцу seller-service без gRPC и commons-codec, `<skipTests>true</skipTests>`, Dockerfile, `.gitignore`), `OrderServiceApplication` с `@EnableScheduling`; модуль в корневом `pom.xml` (D1). Проверка (компиляция): `mvn -q -f order-service/pom.xml package` собирает jar
- [x] 2.2 `application.yaml`: имя, порт, datasource, `ddl-auto`, `defer-datasource-initialization`, `sql.init.mode: always`, Kafka producer, JWT, Eureka, `catalog-service.url`, значения из env (D1, D14). Проверка (ревью): литеральных секретов нет; сборка проходит
- [x] 2.3 Безопасность: копия `KCRoleConverter`, `SecurityFilterChain` по D13, `CurrentUserService` (`sub`, токен, email только при `email_verified` = true) (D9, D13). Проверка (ревью): соответствие delta `access-control` и `integration-events` `Order event personal data`
- [x] 2.4 Ошибки: `Response {message, time}`, исключения NotFound/Conflict/CatalogUnavailable, `GlobalExceptionHandler` (валидация с префиксом "Validation failed:", несоответствие типа → 400, 404, 405, 409, 503, без стектрейсов) (D13). Проверка (ревью): коды совпадают со specs `shopping-cart` и `orders`
- [x] 2.5 Логирование order-service: ни одна строка лога не выводит email, payload, JWT или `Authorization`; SQL-логирование значений параметров выключено (D9). Проверка (ревью): `git grep -n "log\." order-service/src` без email/payload/token

## 3. order-service: модель данных

- [x] 3.1 Сущности `Cart`, `CartItem`, `Order` (`version`), `OrderItem` (поля снимка D7), `OrderStatus`, `OutboxEvent`; репозитории с выборками только по `user_id`, блокировкой корзины `FOR UPDATE`, lazy-созданием корзины `ON CONFLICT DO NOTHING` (D2, D4, D7). Проверка (ревью): ни одна выборка заказа или корзины не выполняется без `user_id`; email ни в одной сущности, кроме payload outbox
- [x] 3.2 `schema.sql`: CHECK (quantity 1–99, суммы > 0, `items_count` > 0, статус), уникальности и индексы D2; все команды идемпотентны. Проверка (компиляция и, если доступна локальная БД, двукратный запуск скрипта без ошибок; иначе отметить «не проверялось»)

## 4. order-service: каталог и доменные правила

- [x] 4.1 Интерфейс `CatalogClient` и реализация на `@LoadBalanced WebClient` с JWT пользователя, таймаутами 2 с / 5 с, отображением ошибок в 503 (D3). Проверка (ревью): нет вызова каталога без `Authorization`
- [x] 4.2 Чистые классы без Spring: `CatalogItemPolicy` (D3), `CartPricing`, `OrderTransitions` (таблица D6 с инициаторами), `OrderEventFactory` (payload и обезличивание D8, D9) (D15). Проверка (ревью): классы не импортируют `org.springframework`

## 5. order-service: корзина

- [x] 5.1 `GET /cart`, `POST /cart/items`, `PUT /cart/items/{productId}`, `DELETE /cart/items/{productId}`, `DELETE /cart`: DTO с Bean Validation, блокировка корзины и `lock_timeout` 5 с, лимит 50 позиций, ответы по spec `shopping-cart` (D4). Проверка (ревью): соответствие каждому requirement `shopping-cart`; цена из запроса нигде не читается
- [x] 5.2 OpenAPI-аннотации эндпоинтов корзины. Проверка (компиляция)

## 6. order-service: заказы

- [x] 6.1 `POST /orders` без тела: последовательность D5 в одной транзакции (блокировка, каталог, проверка доступности, снимки D7, outbox ORDER_CREATED, очистка корзины), 201 + `Location`; тело и `Idempotency-Key` игнорируются. Проверка (ревью): все записи в одном транзакционном методе, ошибка каталога не меняет данные, в коде нет `expectedTotal`
- [x] 6.2 `GET /orders/{orderId}` и `GET /orders` (ограничения `page`, `size`, `sort`, DTO страницы); чтение не вызывает каталог (D7, D13). Проверка (ревью): в сервисе чтения нет зависимости от `CatalogClient`
- [x] 6.3 `POST /orders/{orderId}/cancel`: переход через `OrderTransitions`, повторная отмена → 409 без события, `cancelledAt`, outbox ORDER_CANCELLED в той же транзакции, optimistic lock → 409 (D6). Проверка (ревью): статус меняется только в доменном методе перехода
- [x] 6.4 OpenAPI-аннотации эндпоинтов заказов. Проверка (компиляция)

## 7. order-service: события

- [x] 7.1 Запись события: `eventId` = id записи outbox, присвоенный до сохранения и включенный в payload; `occurredAt` = `created_at`; ключ `orderId`; одна запись на изменение (D8). Проверка (ревью): `eventId` не генерируется заново при публикации
- [x] 7.2 `OutboxPublisher` по образцу seller-service с обезличиванием `ORDER` до `{orderId, eventId, redacted}` после `send().get()`, ERROR в логе для неизвестного типа; бин `NewTopic` `order-events-topic` (D8, D9). Проверка (ревью): обезличивание только после подтверждения, в логах публикатора нет payload

## 8. notification-service: события заказа

- [x] 8.1 Конверт заказа с `eventType` строкой, `OrderEvent`, `OrderEventClassifier` (уведомляемые, список без уведомления — пуст, неизвестные, некорректные), неповторяемое исключение для неизвестных и некорректных в `addNotRetryableExceptions`; поведение других топиков не меняется (D10). Проверка (компиляция и ревью): `mvn -q -f notification-service/pom.xml package`; diff `KafkaConfig` только добавляет исключение и `NewTopic` `order-events-topic.DLT`
- [x] 8.2 Таблица `processed_order_events` (event_id PK, order_id, event_type, processed_at, без email) и обработка: проверка дубликата → письмо → запись; конфликт PK — дубликат без ошибки (D10). Проверка (ревью): отметка выполняется только после успешной отправки или осознанного пропуска
- [x] 8.3 Письма "Order {orderId} created" и "Order {orderId} cancelled"; нет email → WARN и отметка; ошибка SMTP пробрасывается для повторов и DLT; логи без адреса и payload (D9, D10). Проверка (ревью): `git grep` новых строк логирования не находит email или payload

## 9. Инфраструктура и развертывание

- [x] 9.1 api-gateway: маршрут `order-service` `Path=/cart,/cart/**,/orders,/orders/**` → `lb://ORDER-SERVICE`; CORS не менять (D14). Проверка (компиляция): `mvn -q -f api-gateway/pom.xml package`
- [x] 9.2 `postgres/init.sql` (`CREATE DATABASE order_service;`), `.env.order` для dev (D14). Проверка (ревью): только перечисленные переменные
- [x] 9.3 `docker-compose.yml` и `docker-compose.prod.yml`: сервис `order-service` с `container_name` (один экземпляр, D11), env-файлами, `depends_on`; prod-образ `viperanna/order-service:${ORDER_SERVICE_TAG}`. Проверка: `docker compose -f <файл> config -q` для обоих файлов (если Docker недоступен — отметить «не проверялось»)
- [x] 9.4 `Jenkinsfile`: `ORDER_IMAGE` и `order-service` во всех картах сервисов (D14). Проверка (ревью): `git diff Jenkinsfile` показывает `order-service` в каждой карте с `seller-service`

## 10. Интеграционная проверка change

- [x] 10.1 Сборка reactor `mvn -q package` в корне. Проверка (компиляция): сборка проходит; в отчете явно указано, что тесты не запускались и корректность не подтверждена
- [x] 10.2 Границы изменений: только `order-service/`, корневой `pom.xml`, `catalog-service` (`ProductResponse`, маппер), `notification-service` (события заказа, таблица, письма, DLT-топик, неповторяемое исключение), `api-gateway` yaml, `postgres/init.sql`, `.env.order`, compose-файлы, `Jenkinsfile`; `skipTests` и surefire существующих модулей не изменены. Проверка: `git diff --stat` и `git diff -- '*/pom.xml'`
- [ ] 10.3 `openspec validate add-order-service --strict` проходит. После archive обновить non-normative секции: Event Catalog (`integration-events`, `order-events-topic` и его DLT), Access Matrix (`access-control`), OBS-04 и OBS-15 (`integration-events`, частично устранены для заказов). Проверка: strict validate затронутых specs

## 11. Ручные операции (только по запросу пользователя, в рамках apply не выполняются)

- [ ] 11.1 Существующие стенды: `CREATE DATABASE order_service;` в контейнере PostgreSQL до первого запуска order-service (dev и prod)
- [ ] 11.2 prod: `.env.order` на сервере; порядок развертывания design.md Migration Plan
- [ ] 11.3 Стенд: Eureka, маршруты gateway, сценарии корзины и оформления (несколько магазинов, недоступный товар, изменение цены, двойное оформление, отмена и повторная отмена, чужой заказ), события и их `eventId`, обезличенный outbox, письма в mailpit, повторная доставка без второго письма, DLT для неизвестного типа

## Отложено (вне scope этого change, не отслеживается)

Реализация `Idempotency-Key` для `POST /orders` по контракту design.md D12 — следующий этап, отдельный change `order-checkout-idempotency` по согласованию с пользователем. В текущей реализации заголовок игнорируется.
