# Tasks

## 0. Решения до реализации

- [x] 0.1 Получить ответы на Q1–Q2 (design.md) и при необходимости обновить proposal, design и delta specs. Проверка: `openspec validate store-change-moderation --strict` проходит

## 1. seller-service: подача заявки

- [x] 1.1 Тесты (до кода): `PUT /stores/{id}` создает заявку PENDING с trim имени и нормализованным телефоном, магазин не меняется, ответ 202 с заявкой; вторая заявка при PENDING → 409; занятое имя другого своего магазина → 409, свое имя в другом регистре — без конфликта; чужой магазин → 404; заблокированный продавец или магазин → 409; нарушение `uq_store_change_requests_pending` (23505) при одновременной подаче → 409; `GET /stores/{id}/change-requests` — по убыванию даты, 404 для чужого; контроллер отвечает 202 с телом заявки; подача и удаление магазина владельцем читают магазин блокирующим методом (D3b), удаление удаляет заявки, включая PENDING. Пять тестов `updateStore_*` в `StoreServiceImplTest` переписать под создание заявки (поведение «магазин меняется сразу» больше не ожидается). Проверка: тесты падают на текущем коде
- [x] 1.2 `StoreChangeRequest`, `StoreChangeRequestStatus`, репозиторий, связь в `Store` с каскадным удалением, блокирующие методы `StoreRepository` (D3b), частичный индекс в `schema.sql` (D1). Проверка: `mvn -q package -Dmaven.test.skip=true`, `schema.sql` в jar
- [x] 1.3 Подача и просмотр в `StoreServiceImpl` и `StoreController` (`@ResponseStatus(ACCEPTED)`, D2, D3a, D4), DTO ответа заявки. Проверка: тесты 1.1 проходят

## 2. seller-service: модерация

- [x] 2.1 Тесты (до кода): очередь pending по возрастанию даты; одобрение переносит только непустые поля и переводит в APPROVED (204); отклонение с причиной → REJECTED (200), без причины → 400; своя заявка → 403; не PENDING → 409; нет заявки → 404; при одобрении занятое имя или нарушение индекса → 409 и заявка PENDING; заблокированный продавец или магазин → 409 и PENDING; отклонение заявки заблокированного магазина работает; магазин при одобрении читается методом с блокировкой, отсутствие магазина → 404 без изменений; окончательное удаление продавца читает его магазины блокирующим методом (D3b). Проверка: тесты падают на текущем коде
- [x] 2.2 Сервис и `ModeratorStoreChangeController` (D3). Проверка: тесты 2.1 проходят, `mvn test -DskipTests=false` seller-service зеленый

## 3. api-gateway

- [x] 3.1 Путь `/moderator/store-change-requests/**` в маршруте seller-service. Проверка: `mvn -q package -Dmaven.test.skip=true` api-gateway, путь в jar

## 4. Проверка

- [x] 4.1 Изменения ограничены seller-service, api-gateway и тестами. Проверка: `git diff --stat`
- [x] 4.2 `openspec validate store-change-moderation --strict` проходит. После archive в `seller-stores` отметить решение по модерации изменений магазина в non-normative секциях (OBS-06 остается открытым). Проверка: `openspec validate seller-stores --type spec --strict`

## 5. Ручные проверки на стенде

Выполняются отдельно по запросу пользователя, в рамках apply не запускаются.

- [ ] 5.1 Пересобрать seller-service и api-gateway. Через gateway: `PUT /stores/{id}` → 202 и магазин не изменился; модератор видит заявку в `/moderator/store-change-requests/pending`, одобряет → магазин изменился; вторая заявка отклоняется с причиной; удаление магазина удаляет заявки; `pg_indexes` содержит `uq_store_change_requests_pending`
