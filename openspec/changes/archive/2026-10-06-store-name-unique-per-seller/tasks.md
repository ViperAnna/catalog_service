# Tasks

## 1. Проверка данных до применения (ручной шаг)

- [x] 1.1 На текущей (локальной) БД `seller_service` выполнить: дубли `select seller_id, lower(btrim(store_name)), count(*) from stores group by 1,2 having count(*) > 1`; имена с пробелами по краям `store_name <> btrim(store_name)`; наличие индекса с именем `uq_stores_seller_store_name`. Проверка: результат записан в отчет к задаче, дублей нет. Если дубли найдены, работа останавливается до решения по ним. При деплое на production шаг нужно повторить для production БД

## 2. Тесты (до изменения кода)

- [x] 2.1 Добавить в `StoreServiceImplTest` тесты: (а) создание с `"  My Shop  "` сохраняет `"My Shop"` и вызывает проверку `existsSameName(7, "My Shop", null)`; (б) `PUT` магазина 3 вызывает `existsSameName(7, <имя>, 3)`, то есть исключает сам магазин, и при `false` проходит (OBS-05); (в) положительная проверка дает `ResourceConflictException` с `Store with name [...] already exists.`; (г) `DataIntegrityViolationException` с причиной `SQLException(..."uq_stores_seller_store_name"..., "23505")` из `saveAndFlush` превращается в `ResourceConflictException`; (д) `DataIntegrityViolationException` с другим SQLState или другим именем пробрасывается без изменений. Проверка: `mvn test -Dtest=StoreServiceImplTest` не компилируется или падает на текущем коде (метода `existsSameName` нет)

## 3. Реализация

- [x] 3.1 Добавить в `StoreRepository` JPQL-метод `existsSameName(sellerId, name, excludeId)` (design.md, D3) и удалить неиспользуемый `existsBySellerIdAndStoreNameIgnoreCase`. Проверка: `mvn -q compile` проходит
- [x] 3.2 В `StoreServiceImpl`: `trim` storeName при создании и изменении (D1); проверка через `existsSameName` с `excludeId = null` при создании и id магазина при изменении; `saveAndFlush` и перевод нарушения `uq_stores_seller_store_name` (`23505`) в `ResourceConflictException(STORE_ALREADY_EXISTS, storeName)`, прочие ошибки пробрасываются (D4). Проверка: `mvn test -Dtest=StoreServiceImplTest` проходит
- [x] 3.3 Добавить `seller-service/src/main/resources/schema.sql` с `CREATE UNIQUE INDEX IF NOT EXISTS uq_stores_seller_store_name ON stores (seller_id, lower(btrim(store_name)));` и в `application.yaml` `spring.sql.init.mode: always` и `spring.jpa.defer-datasource-initialization: true` (D2). Проверка: `mvn -q package` проходит, `schema.sql` есть в jar (`BOOT-INF/classes/schema.sql`)

## 4. Проверка

- [x] 4.1 Полный прогон тестов seller-service: `mvn test` зеленый, включая тесты предыдущих change. Проверка: вывод `mvn test`
- [x] 4.2 Пересобрать и перезапустить только seller-service на локальной БД. Проверка: (а) в логе нет ошибок SQL-инициализации, сервис стартовал; (б) `select indexdef from pg_indexes where indexname = 'uq_stores_seller_store_name'` возвращает `CREATE UNIQUE INDEX ... (seller_id, lower(btrim((store_name)::text)))`; (в) повторный перезапуск seller-service тоже успешен (`IF NOT EXISTS`). При деплое на production шаг нужно повторить для production БД
- [x] 4.3 Ручная проверка индекса и JPQL-запроса на локальной БД в транзакции с откатом: вставка `'  SHOP  '` для продавца, у которого есть `'Shop'`, отклоняется с `23505` и `uq_stores_seller_store_name`; для другого продавца проходит. Проверка: вывод `psql` записан в отчет, после `ROLLBACK` данные не изменены
- [x] 4.4 Убедиться, что изменения ограничены `StoreRepository`, `StoreServiceImpl`, `schema.sql`, `application.yaml` seller-service и тестами, а catalog-service, frontend и остальные таблицы не тронуты. Проверка: `git diff --stat`, `git status`
- [x] 4.5 Проверка change: `openspec validate store-name-unique-per-seller --strict` проходит. После archive в `openspec/specs/seller-stores/spec.md` отметить OBS-05, OBS-10 и ASM-01 как устраненные этим change, сохранив идентификаторы, и обновить OQ-01. Проверка: `openspec validate seller-stores --type spec --strict` проходит
