# Tasks

## 1. Подготовка

- [x] 1.1 Удалить закомментированные `user-service/src/test/java/ru/klimovich/user_service/controller/UserControllerTest.java`, `repository/UserRepositoryTest.java`, `service/impl/UserServiceImplTest.java` (design.md, D5). Проверка: `git status` показывает три удаленных файла, а `src/test/java` не содержит других классов
- [x] 1.2 Добавить в `user-service/pom.xml` зависимость `org.testcontainers:junit-jupiter` со scope `test` без явной версии. Проверка: `mvn -q -pl user-service test-compile` (из корня) или `mvn -q test-compile` в `user-service` завершается успешно, а `mvn dependency:tree -Dscope=test` показывает версию из BOM

## 2. Unit-тесты UserServiceImpl

- [x] 2.1 Создать `service/impl/UserServiceImplTest` (JUnit 5, Mockito, реальные `UserMapperImpl` и `EventMapper`, design.md, D1) с тестами на `Profile provisioning on first read` и `USER_CREATED outbox event`: (а) первый `getUser` сохраняет профиль с firstName/lastName/email из `given_name`/`family_name`/`email` и пишет одну outbox-запись (aggregateType `USER`, aggregateId = id, eventType `USER_CREATED`, processed = false, payload `{userId, keycloakUserId, email, username = preferred_username}`); (б) повторный `getUser` при существующем профиле не сохраняет и не пишет outbox. Проверка: тесты проходят на текущем коде
- [x] 2.2 Добавить тесты `Own profile retrieval`: roles = realm-роли без `offline_access`, `uma_authorization`, `default-roles-user-service-realm`; без `realm_access` roles = []. Проверка: тесты проходят
- [x] 2.3 Добавить тесты `Own profile update` и `Admin user update and deletion`: меняются только non-null поля, в том числе отдельные поля адреса, при `address == null` у профиля создается новый адрес; email меняется локально; несуществующий keycloakId — `ResourceNotFoundException` с текстом `User not found with id: [<id>].`; `deleteUserById` вызывает `delete`, а для несуществующего — 404. Проверка: тесты проходят
- [x] 2.4 Добавить тесты `Admin user listing and retrieval`: `getAllUsers` и `getUserByKeycloakId` возвращают профили с roles = null, несуществующий — 404. Проверка: `mvn test -Dtest=UserServiceImplTest` в user-service зеленый

## 3. Unit-тесты OutboxPublisher

- [x] 3.1 Создать `outobox/OutboxPublisherTest` (Mockito, design.md, D2): (а) для двух необработанных записей `send` вызывается с топиком `user-created-events-topic`, ключом aggregateId и значением payload, обе помечены processed; (б) при `failedFuture` на второй записи первая помечена processed, вторая и третья — нет, третья не отправлялась; (в) пустая выборка — `send` не вызывается; (г) у `publishEvents` аннотация `@Scheduled(fixedDelay = 3000)`. Проверка: `mvn test -Dtest=OutboxPublisherTest` зеленый

## 4. Security slice-тесты

- [x] 4.1 Создать `controller/UserSecurityWebMvcTest`: `@WebMvcTest(controllers = {UserController.class, AdminController.class})`, `@Import(SpringSecurityConfig.class)`, `@MockBean UserService`, `@MockBean JwtDecoder`, который возвращает Jwt с заданным `realm_access` (design.md, D3). Проверка: контекст среза поднимается без env-переменных
- [x] 4.2 Добавить тесты `User profile API access`: `GET /users/me` без токена — 401; с ролью USER — 200; только ADMIN — 403; роль `user` в нижнем регистре — 200; без `realm_access` — 403; `GET /admin/users` с USER — 403, с ADMIN — 200; `DELETE /users/me` с USER — 204. Проверка: тесты проходят
- [x] 4.3 Добавить тест `User service error responses` и `Profile field validation`: `PUT /users/me` с `phone=12` дает 400 с JSON `{message, time}`, где message начинается с `Validation failed:`. Проверка: `mvn test -Dtest=UserSecurityWebMvcTest` зеленый

## 5. Репозиторные тесты на PostgreSQL (Testcontainers)

- [x] 5.1 Создать `repository/UserRepositoryTest`: `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)`, `@Testcontainers` со статическим `PostgreSQLContainer("postgres:16")`, `@DynamicPropertySource`, `ddl-auto=create-drop`, `@Import(JpaConfig.class)` (design.md, D4). Добавить тесты: сохранение и поиск по keycloakUserId и email, `date_create` заполнен. Проверка: при запущенном Docker тест проходит
- [x] 5.2 Добавить тесты ограничений БД: дубль email и дубль keycloakUserId дают `DataIntegrityViolationException` при flush; null в firstName, lastName или email дает `DataIntegrityViolationException`; два профиля с одинаковым phone сохраняются (OBS-09 as-is, отметить в `@DisplayName`). Проверка: тесты проходят
- [x] 5.3 Создать `repository/OutboxRepositoryTest` на том же подходе: `findTop100ByProcessedFalseOrderByCreatedAtAsc` возвращает только processed = false по возрастанию createdAt и не больше 100 записей. Проверка: тест проходит при запущенном Docker

## 6. Итоговая проверка

- [ ] 6.1 Полный прогон: `mvn test` в user-service зеленый при запущенном Docker; `git diff --stat` показывает изменения только в `user-service/src/test/**` и `user-service/pom.xml`, production-код не тронут. Проверка: вывод `mvn test` (число тестов, 0 failures) и `git diff --stat`
- [ ] 6.2 Валидация change: `openspec validate restore-user-service-tests --strict` проходит. После archive в `openspec/specs/user-profile/spec.md` отметить OBS-12 как устраненный этим change, сохранив идентификатор. Проверка: `openspec validate user-profile --type spec --strict` проходит
