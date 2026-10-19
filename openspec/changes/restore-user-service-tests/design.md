# Design

## Context

Мотивация описана в proposal.md, раздел Why. Фиксируемое поведение задано
baseline `openspec/specs/user-profile/spec.md`. Delta spec нет
(`skip_specs: true`).

Что важно для тестов:
- В user-service есть gRPC-сервер (`@GrpcService`), Eureka-клиент, Kafka,
  Keycloak Admin client (`KeycloakConfig`) и планировщик (`@EnableScheduling`).
  Полный `@SpringBootTest` потребовал бы всю эту инфраструктуру, поэтому
  используются срезы и unit-тесты.
- `SpringSecurityConfig` объявлен как обычный `@Configuration`. Срез
  `@WebMvcTest` подхватывает его только через явный `@Import`.
- Аудит JPA включен в отдельном `JpaConfig` (`@EnableJpaAuditing`), и
  `@DataJpaTest` его без импорта не подключает.
- В `application.yml` много `${...}` из env. В срезах они не нужны, кроме
  настроек JWT. Их заменяет мок `JwtDecoder`.
- Есть пример стиля: `seller-service/.../SellerApplicationServiceImplTest`
  (JUnit 5, Mockito, реальный сгенерированный маппер, AssertJ).

## Goals / Non-Goals

**Goals:**
- Тесты проходят на текущем коде без изменений production-кода.
- Каждый тест соответствует requirement или scenario baseline `user-profile`.
  В имени или `@DisplayName` есть ссылка на этот requirement.
- Поведение, отмеченное как OBS, закрепляется так, как оно есть сейчас,
  например «телефон не уникален», а не так, как «должно быть».

**Non-Goals:**
- Полный контекст приложения (`@SpringBootTest`) и интеграция с Kafka,
  Keycloak, Eureka и gRPC.
- Покрытие wishlists, CatalogClient и gRPC.
- Метрики покрытия и обязательные пороги.

## Decisions

### D1. Unit-тесты `UserServiceImpl` с реальными мапперами
Используются Mockito для `UserRepository` и `OutboxRepository`, реальный
сгенерированный `UserMapperImpl` и реальный `EventMapper` с `ObjectMapper`.
Jwt строится через `Jwt.withTokenValue(...)` с нужными claims.
Так проверяется фактический маппинг (null-ignore, вложенный адрес) и
фактический JSON payload события, а не ожидания мока. Альтернатива с моками
мапперов отклонена: она не поймала бы расхождение с baseline.

Сохранение мокается ответом, который проставляет UUID в переданный объект, по
аналогии с seller-тестами: сервис берет `user.getId()` из результата `save`.

### D2. Unit-тесты `OutboxPublisher` с моком `KafkaTemplate`
`KafkaTemplate.send(topic, key, value)` возвращает
`CompletableFuture.completedFuture(...)` для успеха и `failedFuture(...)` для
ошибки. Проверяются топик, ключ (aggregateId), значение (payload без
обертки), отметка processed и остановка цикла на первой ошибке. Интервал
`@Scheduled(fixedDelay = 3000)` проверяется через reflection по аннотации
метода. Это дешево и закрепляет значение из baseline. Таймаут 10 с напрямую
не проверяется: тест был бы медленным, а покрытие ветки `TimeoutException`
совпадает с `ExecutionException`.

### D3. Security slice через реальную цепочку конвертации ролей
`@WebMvcTest(controllers = {UserController.class, AdminController.class})` с
`@Import(SpringSecurityConfig.class)` и `@MockBean UserService`. Вместо
post-processor `jwt()` из spring-security-test подменяется бин `JwtDecoder`
(`@MockBean`). Тест отправляет настоящий заголовок `Authorization: Bearer <t>`,
а декодер возвращает Jwt с нужным `realm_access`.

Альтернатива `jwt()` отклонена: этот post-processor подставляет
аутентификацию в обход настроенного `JwtAuthenticationConverter`, то есть не
проверяет `KCRoleConverter` и сопоставление ролей без учета регистра. Поэтому
зависимость `spring-security-test` не нужна: `@WebMvcTest` и так регистрирует
бин `springSecurityFilterChain` в `MockMvc`, а заголовок `Authorization`
формируется вручную.

Покрываемые случаи: без токена 401; USER для `/users/me` 200; токен только с
ADMIN для `/users/me` 403; USER для `/admin/users` 403; ADMIN для
`/admin/users` 200; роль в нижнем регистре `user` дает доступ; нет
`realm_access` — 403; `DELETE /users/me` 204; невалидный phone — 400 с
`Validation failed:`.

### D4. Репозиторные тесты на PostgreSQL в Testcontainers
`@DataJpaTest` + `@AutoConfigureTestDatabase(replace = NONE)` + `@Testcontainers`
со статическим `PostgreSQLContainer("postgres:16")`, совпадающим с compose.
Datasource подставляется через `@DynamicPropertySource`, схема создается
`spring.jpa.hibernate.ddl-auto=create-drop`. Аудит подключается через
`@Import(JpaConfig.class)`.

`spring-boot-testcontainers` с `@ServiceConnection` не используется: это была
бы еще одна зависимость ради одной строки конфигурации. Модуль
`org.testcontainers:junit-jupiter` добавляется ради `@Testcontainers` и
`@Container` (управление жизненным циклом).
`disabledWithoutDocker` не включается: без Docker тест должен падать явно, а
не молча пропускаться.

Проверяется:
- сохранение и поиск по keycloakUserId и email;
- `DataIntegrityViolationException` для дубля email, дубля keycloakUserId и
  null в first_name, last_name, email;
- два пользователя с одинаковым phone сохраняются (OBS-09 as-is);
- `date_create` заполняется аудитом;
- `OutboxRepository.findTop100ByProcessedFalseOrderByCreatedAtAsc`
  возвращает только необработанные записи по возрастанию createdAt и не
  больше 100.

### D5. Удаление старых тестов отдельным шагом
Три закомментированных файла удаляются целиком в первой задаче, до добавления
новых. Новые классы получают те же имена пакетов, но пишутся с нуля. Так в
истории git видно, что это удаление, а не правка.

## Risks / Trade-offs

- [Тест закрепляет поведение OBS, и будущий fix его сломает] → Это ожидаемо.
  Такие тесты помечаются в `@DisplayName` ссылкой на OBS, чтобы при
  исправлении было видно, какой тест меняется осознанно.
- [Репозиторные тесты требуют Docker и скачивают `postgres:16`] → На машине
  разработчика Docker есть. В CI тесты не запускаются (`-DskipTests`).
  Отключение без Docker не делаем намеренно.
- [Slice-контекст падает на неразрешенных `${...}` из `application.yml`] →
  `JwtDecoder` подменяется моком, поэтому автоконфигурация resource server не
  читает `jwk-set-uri`. Если какое-то свойство все же потребуется, оно
  задается в тестовом `@TestPropertySource`, а не в `src/main`.
- [Версия `testcontainers-junit-jupiter`] → Берется из BOM Spring Boot 3.3.4
  без явной версии.

## Migration Plan

Не требуется: изменения только в тестах и test-scope зависимостях.
Откат — revert коммита.
