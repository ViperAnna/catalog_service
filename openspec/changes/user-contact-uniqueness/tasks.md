# Tasks

## 1. Проверка данных до применения (ручной шаг)

- [x] 1.1 На текущей БД `user_service` (сейчас локальной) выполнить проверку конфликтов с учетом нормализации: дубли `lower(email)`; дубли телефона после приведения к `+7XXXXXXXXXX` (`regexp_replace` для форм `8XXXXXXXXXX` и `7XXXXXXXXXX`); количество email не в lowercase; количество телефонов, не проходящих `^(\+7|7|8)\d{10}$`. Проверка: результат запросов записан в отчет к задаче. Если найдены дубли, работа останавливается до выбора стратегии их устранения. При деплое на production шаг нужно повторить для production БД

## 2. Нормализация email и телефона

- [x] 2.1 Добавить компонент нормализации (design.md, D1): `email` — `trim` и `toLowerCase(Locale.ROOT)`; `phone` — `8XXXXXXXXXX` и `7XXXXXXXXXX` в `+7XXXXXXXXXX`, `+7XXXXXXXXXX` без изменений. Проверка: `mvn -q compile` в user-service проходит
- [x] 2.2 Заменить валидацию `UserUpdateRequest.phone` на `^(\+7|7|8)\d{10}$` с сообщением о допустимых формах. Проверка: `mvn -q compile` проходит, шаблон совпадает с requirement `Profile field validation` в delta spec
- [x] 2.3 В `UserServiceImpl` приводить email из JWT к lowercase при создании профиля, а email и phone из запроса нормализовать перед проверками и записью при `updateUserById` (общий для `PUT /users/me` и `PUT /admin/users/{keycloakId}`). Проверка: `mvn -q compile` проходит; изменения ограничены `UserServiceImpl` и компонентом нормализации

## 3. Уникальность и 409

- [x] 3.1 Добавить в `UserRepository` `existsByEmailIgnoreCaseAndKeycloakUserIdNot` и `existsByPhoneAndKeycloakUserIdNot`; в `updateUserById` проверять только переданные и отличающиеся после нормализации значения и выбрасывать `ResourceConflictException` с `USER_EMAIL_ALREADY_EXIST` или `USER_PHONE_ALREADY_EXIST` и нормализованным значением (design.md, D2). Проверка: `mvn -q compile` проходит
- [x] 3.2 Объявить `@Column(name = "phone", unique = true)` в `User` (design.md, D4). Проверка: `mvn -q compile` проходит
- [x] 3.3 Добавить перевод нарушения unique constraint при гонке (design.md, D3): `saveAndFlush` в `updateUserById`; разбор `DataIntegrityViolationException` (SQLState `23505`, detail или column PostgreSQL) в 409 для email и phone; прочие ошибки пробрасываются без изменений. Проверка: `mvn -q compile` проходит; имена constraint в коде не используются (`grep -r "uk" ` по новому коду не находит имен ограничений)

## 4. Frontend

- [x] 4.1 В `frontend/src/pages/ProfilePage/index.jsx` и `frontend/src/pages/AdminUsersPage/components/UserEditModal.jsx` заменить шаблон телефона на `/^(\+7|7|8)\d{10}$/` и обновить текст ошибки о допустимых формах (design.md, D5). Проверка: `npm run build:prod` во frontend проходит; `grep -rn "7,15" frontend/src` ничего не находит

## 5. Проверка после запуска (ручной шаг)

- [ ] 5.1 Пересобрать и запустить user-service на текущей (локальной) БД, затем проверить, что unique constraint на `users.phone` создан: `select conname, pg_get_constraintdef(oid) from pg_constraint where conrelid = 'users'::regclass and contype = 'u'` показывает `UNIQUE (phone)`. Проверка: вывод запроса записан в отчет к задаче. При деплое на production шаг нужно повторить для production БД

> **Статус на 2026-10-06 (зафиксировано, реализация приостановлена до решения по D4/D3):**
> - Новый код задач 1.1–4.1 применен: user-service пересобран и запущен на локальной БД.
> - Нормализация email и телефона и проверки уникальности в сервисе (409) работают.
> - `UNIQUE(phone)` в текущей БД **отсутствует**: `ddl-auto: update` не добавил его для существующей колонки, хотя `@Column(unique = true)` объявлен (3.2), а дублей нет (1.1). Есть только `UNIQUE (email)` и `UNIQUE (keycloak_user_id)`.
> - Поэтому задача 5.1 **не выполнена**.
> - Защита от гонки для phone через DB constraint (решение 3, требования `Contact data uniqueness` и `User service error responses`) **не считается выполненной**: код перевода в 409 (3.3) есть, но без constraint в БД ему нечего перехватывать. Для email защита опирается на существующий `UNIQUE (email)`.
> - Отклонения реализации от design, которые нужно отразить при правке D3/D4: `ContactNormalizer` и `ContactUniqueViolationTranslator` сделаны статическими утилитами, а не Spring-бинами (конструктор `UserServiceImpl` не меняется). В D3 вместо `PSQLException`/`ServerErrorMessage` используется `java.sql.SQLException` (`getSQLState()` и текст `(email)=` / `(phone)=`), потому что драйвер PostgreSQL подключен со scope `runtime`.

## 6. Тесты (ОТЛОЖЕНО до возобновления C1 `restore-user-service-tests`)

Тестовая инфраструктура user-service (Testcontainers, security slice) относится к C1 и не входит в реализацию C3. Эти задачи нельзя отмечать выполненными, пока они не реализованы. C3 не считается завершенным без них.

- [ ] 6.1 (ОТЛОЖЕНО до C1) Unit-тесты нормализации: `8XXXXXXXXXX`, `7XXXXXXXXXX`, `+7XXXXXXXXXX` дают `+7XXXXXXXXXX`; email в lowercase. Проверка: тесты проходят
- [ ] 6.2 (ОТЛОЖЕНО до C1) Unit-тесты `UserServiceImpl`: 409 `USER_EMAIL_ALREADY_EXIST` для чужого email в другом регистре; 409 `USER_PHONE_ALREADY_EXIST` для чужого телефона в другой форме; свои email и телефон не дают конфликта; email из JWT сохраняется в lowercase, в том числе в payload USER_CREATED. Проверка: тесты проходят
- [ ] 6.3 (ОТЛОЖЕНО до C1) Тест гонки на реальном PostgreSQL: нарушение unique constraint email и phone при `saveAndFlush` превращается в 409 с сообщением для нужного поля; прочие нарушения целостности 409 не дают. Несколько профилей без телефона сохраняются. Проверка: тесты проходят
- [ ] 6.4 (ОТЛОЖЕНО до C1) Тест валидации: `+7 999 123-45-67`, `+12025550123`, `12` дают 400 `Validation failed:`. Проверка: тесты проходят

## 7. Spec

- [ ] 7.1 Проверка change: `openspec validate user-contact-uniqueness --strict` проходит. После archive обновить non-normative секции `openspec/specs/user-profile/spec.md`: отметить OBS-09 и часть a OBS-03 (500 при занятом email) как устраненные этим change, сохранив идентификаторы; OBS-03 часть b (синхронизация с Keycloak) остается открытой. Проверка: `openspec validate user-profile --type spec --strict` проходит
