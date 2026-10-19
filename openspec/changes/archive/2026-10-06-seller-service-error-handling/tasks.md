# Tasks

## 1. Проверка данных до применения (ручной шаг)

- [x] 1.1 На локальной БД `seller_service` проверить: дубли `select lower(btrim(seller_name)), count(*) from sellers group by 1 having count(*) > 1`; имена с пробелами по краям; дубли имен в PENDING-заявках; имена уникальных ограничений `sellers` в `pg_constraint`. Проверка: дублей нет, имен с пробелами нет, ограничение имени — `uk8757g69e614binmjpvpoeda7d UNIQUE (seller_name)` (результат записан в design.md, Context). При деплое на production шаг нужно повторить
- [x] 1.2 Убедиться, что клиенты не разбирают стандартное тело ошибок seller-service: поиск по catalog-service (`SellerClient` и обработка ответов) и frontend. Проверка: catalog-service использует только коды ответов, frontend к seller-service не обращается

## 2. Тесты (до изменения кода)

- [x] 2.1 Добавить `GlobalExceptionHandlerTest` (standalone MockMvc, design.md, D5): 400 с `Validation failed: ` для `@RequestBody` (`POST /stores` без phoneNumber) и для `@ModelAttribute` (`PUT /seller-applications/update` с некорректным paymentAccount); 400 для `{"status":"UNKNOWN"}` в reject и для нечислового id; 404 и 409 с текстом исключения; 500 с `Internal server error` без текста исключения; `AccessDeniedException` пробрасывается. Проверка: тесты не компилируются или падают на текущем коде
- [x] 2.2 Дополнить `SellerApplicationServiceImplTest`: trim sellerName при подаче; 409 `Seller with name [...] already exists.` при `existsSameName = true`; 409 `Seller name [...] is already in review.` при PENDING-заявке с тем же именем; новые тексты `APPLICATION_ALREADY_EXISTS`, `SELLER_ALREADY_EXIST`, `APPLICATION_NOT_FOUND_FOR_USER`. Проверка: тесты падают на текущем коде
- [x] 2.3 Добавить `ModeratorServiceImplTest`: при занятом имени 409 до `saveAndFlush` и без gRPC; `DataIntegrityViolationException` с `SQLException(..."uq_sellers_seller_name"..., "23505")` и с `uk8757g69e614binmjpvpoeda7d` превращается в 409, gRPC не вызывается; другое ограничение или SQLState пробрасывается. Проверка: тесты падают на текущем коде

## 3. Реализация

- [x] 3.1 Исправить и дополнить `MessageKeys` по таблице design.md, D2; в `getMyCurrentApplication` использовать `APPLICATION_NOT_FOUND_FOR_USER`, в `validateNoPendingApplication` — новый `APPLICATION_ALREADY_EXISTS` без аргумента. Проверка: `mvn -q compile` проходит
- [x] 3.2 Добавить `exception/GlobalExceptionHandler` (D1). Проверка: `mvn test -Dtest=GlobalExceptionHandlerTest` проходит
- [x] 3.3 Вынести поиск нарушения уникальности в `util/UniqueViolations` и перевести на него `StoreServiceImpl` без изменения поведения (D4). Проверка: `mvn test -Dtest=StoreServiceImplTest,UniqueViolationsTest` проходит
- [x] 3.4 Добавить `SellerRepository.existsSameName` и `SellerApplicationRepository.existsSameNameWithStatus` (D3); в `SellerApplicationServiceImpl.createRegistrationRequest` — trim и проверки имени. Проверка: `mvn test -Dtest=SellerApplicationServiceImplTest` проходит
- [x] 3.5 В `ModeratorServiceImpl.approveRegistration`: проверка имени, `saveAndFlush` и перевод нарушения `uq_sellers_seller_name` / `uk8757g69e614binmjpvpoeda7d` в 409 до gRPC (D3, D4). Проверка: `mvn test -Dtest=ModeratorServiceImplTest` проходит
- [x] 3.6 Добавить в `schema.sql` `CREATE UNIQUE INDEX IF NOT EXISTS uq_sellers_seller_name ON sellers (lower(btrim(seller_name)));`. Проверка: `mvn -q package` проходит

## 4. Проверка

- [x] 4.1 Полный прогон тестов seller-service: `mvn test` зеленый, включая тесты предыдущих change. Проверка: вывод `mvn test`
- [x] 4.2 Убедиться, что изменения ограничены seller-service (код, `schema.sql`, тесты). Проверка: `git diff --stat`, `git status`
- [x] 4.3 `openspec validate seller-service-error-handling --strict` проходит. После archive в `seller-onboarding` отметить OBS-08, OBS-09, OBS-13 и ASM-08 устраненными, в `seller-stores` — OBS-11 и обновить OQ-01. Проверка: `openspec validate seller-onboarding --type spec --strict` и `openspec validate seller-stores --type spec --strict` проходят

## 5. Ручные проверки на стенде

Выполняются отдельно по запросу пользователя, в рамках apply не запускаются.

- [ ] 5.1 Пересобрать и перезапустить только seller-service на стенде. Проверка: сервис стартовал без ошибок SQL-инициализации; `pg_indexes` содержит `uq_sellers_seller_name` с `lower(btrim((seller_name)::text))`; повторный старт успешен
- [ ] 5.2 Ручная проверка индекса и JPQL на стенде в транзакции с откатом: вставка Seller с именем существующего в другом регистре и с пробелами отклоняется `23505`; после `ROLLBACK` данных нет. Проверка: вывод `psql` в отчете
- [ ] 5.3 Ручной smoke через api-gateway с токеном: `GET /stores/999999` → 404 `{message, time}`; `POST /stores` без phoneNumber → 400; `GET /sellers/me` без роли → 403. Проверка: ответы записаны в отчет
