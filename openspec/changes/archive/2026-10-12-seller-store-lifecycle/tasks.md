# Tasks

## 0. Решения и данные до реализации

- [x] 0.1 Получить ответы на открытые вопросы N1–N3 (design.md) и при необходимости обновить proposal, design и delta specs. Проверка: `openspec validate seller-store-lifecycle --strict` проходит
- [x] 0.2 Выполнить шаг 1 процедуры D10 (проверка Seller в статусе DELETED, их магазинов, товаров, заявок) на стенде. Проверка: результат записан в design.md; не утверждать отсутствие данных без проверки

## 1. seller-service: статусы, блокировка, ограничения

- [x] 1.1 Тесты (до кода): блокировка и разблокировка продавца и магазина (204, 404, 409); события STORE_BLOCKED/UNBLOCKED и SELLER_BLOCKED/UNBLOCKED с ключом storeId; независимость уровней; при блокировке нет STORE_PURGED и изменений данных; ограничения D6 по текущему статусу: `POST /stores` (продавец BLOCKED), `PUT`/`DELETE /stores/{id}` (продавец или магазин BLOCKED), подача UPDATE → 409, просмотр и DELETION разрешены; одобрение UPDATE заблокированного продавца → 409, заявка PENDING, отклонение работает; `status` и `sellerStatus` в ответе магазина. Проверка: тесты падают на текущем коде
- [x] 1.2 `Store.status`, `schema.sql` (ACTIVE, DEFAULT, NOT NULL), `StoreResponse` и маппер (D1, D6). Проверка: `mvn -q package`, `schema.sql` в jar
- [x] 1.3 API модератора, проверки статуса, outbox-события блокировки (D2, D6). Проверка: тесты 1.1 проходят

## 2. seller-service: заявка UNBLOCK

- [x] 2.1 Тесты (до кода): подача UNBLOCK только BLOCKED-продавцом без PENDING (иначе 409); одобрение → ACTIVE, SELLER_UNBLOCKED для магазинов, SELLER_APPROVED с applicationType UNBLOCK; отдельно заблокированные магазины остаются BLOCKED; повторное одобрение при ACTIVE → 409; при PENDING UPDATE подача UNBLOCK и DELETION → 409 (N3). Проверка: тесты падают на текущем коде
- [x] 2.2 `SellerApplicationType.UNBLOCK`, эндпоинт подачи, ветка одобрения (D7). Проверка: тесты 2.1 проходят

## 3. seller-service: окончательное удаление и outbox

- [x] 3.1 Тесты (до кода): `DELETE /stores/{id}` ACTIVE-магазина ACTIVE-продавцом пишет STORE_PURGED и удаляет строку, STORE_DELETED не пишется; одобрение DELETION (ACTIVE и BLOCKED) пишет STORE_PURGED на каждый магазин и SELLER_APPROVED, обезличивает опубликованные записи outbox заявок продавца, удаляет магазины, Seller и все его заявки; ошибка gRPC откатывает все; новая REGISTRATION после удаления проходит. Проверка: тесты падают на текущем коде
- [x] 3.2 Тесты (до кода) `OutboxPublisher`: после подтверждения Kafka payload `SELLER_APPLICATION` заменен на `{"applicationId", "redacted": true}` и processed = true в той же транзакции; при ошибке или таймауте отправки payload и processed не меняются; payload `STORE` не меняется; запись неизвестного типа, помеченная processed, обезличивается. Проверка: тесты падают на текущем коде
- [x] 3.3 Реализация удаления (D8, D11) и обезличивания (D9). Проверка: тесты 3.1 и 3.2 проходят, `mvn test` seller-service зеленый

## 4. catalog-service

- [x] 4.1 Тесты (до кода): признаки по событиям (абсолютные значения, повтор, независимость); публичные `GET /products`, по категории, поиск и `by-ids` — только доступные; `GET /products/{id}`: доступный — всем, недоступный — только владельцу с JWT SELLER, иначе 404; изменения товаров при не-ACTIVE status или sellerStatus → 409; создание: повторная проверка после сохранения удаляет документ при BLOCKED (409), удаленном магазине (404), недоступном seller-service (503) — все порядки таблицы D3; изменение: частичное обновление не перезаписывает признаки, выставленные событием во время изменения; STORE_PURGED удаляет неиспользуемые изображения (по hash среди товаров других магазинов), затем документы, повтор после частичного сбоя завершает удаление; STORE_DELETED по-прежнему архивирует. Проверка: тесты падают на текущем коде
- [x] 4.2 `Product` (признаки), обработка событий в `StoreEventConsumer` (D2). Проверка: тесты по событиям проходят
- [x] 4.3 Фильтры публичных запросов и доступ владельца (D5), проверка статусов при изменениях, повторная проверка при создании, частичное обновление при изменении (D3, D6). Проверка: тесты 4.1 по выдаче, созданию и изменению проходят
- [x] 4.4 Удаление товаров и изображений по STORE_PURGED, метод удаления объекта в `FileStorageService` (D8). Проверка: все тесты 4.1 проходят, `mvn test -DskipTests=false` новых тестов catalog-service зеленый

## 5. notification-service и api-gateway

- [x] 5.1 Тесты (до кода): SELLER_APPROVED с applicationType UNBLOCK отправляет письмо "Seller account unblocked" с текстом D7 и не уходит в DLT; при MailException в лог не попадает адрес получателя (applicationId и eventType есть). Проверка: тесты падают на текущем коде
- [x] 5.2 Тип UNBLOCK и письмо (D7), лог ошибки отправки без адреса (D9). Проверка: тесты 5.1 проходят
- [x] 5.3 Маршрут api-gateway: `/moderator/sellers/**` и `/moderator/stores/**` в предикате seller-service. Проверка: `mvn -q package` api-gateway

## 6. Проверка и документация

- [x] 6.1 Процедура повторной отправки из DLT (D4) и ручные операции D10 описаны в design.md; при архивации перенести их в non-normative секции `integration-events`. Проверка: разделы присутствуют
- [x] 6.2 Изменения ограничены seller-service, catalog-service, notification-service, api-gateway и тестами; user-service и frontend не изменены. Проверка: `git diff --stat`
- [x] 6.3 `openspec validate seller-store-lifecycle --strict` проходит. После archive обновить non-normative секции: state model и OBS-04, OBS-14, OQ-02–OQ-04 (`seller-onboarding`), OBS-08, OBS-09 (`seller-stores`), OBS-04 (`catalog-products`), Event Catalog (`integration-events`). Проверка: strict validate затронутых specs

## 7. Ручные операции (по запросу пользователя, в рамках apply не выполняются)

- [ ] 7.1 Процедура D10 для существующих Seller в статусе DELETED: проверка, резервная копия (`pg_dump`, `mongodump`, список MinIO), идемпотентная очистка через outbox `STORE_PURGED`, проверка результата
- [ ] 7.2 Однократная обезличка уже обработанных записей outbox `SELLER_APPLICATION`, созданных до этого change: резервная копия, `UPDATE ... SET payload = '{"applicationId": ..., "redacted": true}' WHERE aggregate_type = 'SELLER_APPLICATION' AND processed = true`, проверка
- [ ] 7.3 Стенд: блокировка и разблокировка продавца и магазина (выдача, статусы товаров не меняются, независимость уровней), запрет `PUT`/`DELETE` заблокированного магазина, UPDATE при блокировке, заявка UNBLOCK с письмом, окончательное удаление магазина и продавца (PostgreSQL, MongoDB, MinIO, обезличенный outbox, письмо), повторная регистрация, процедура D4 на тестовом сообщении DLT
