# Proposal

## Why

Все тесты user-service закомментированы (`user-profile` OBS-12) и написаны под
старый API (`UserRequest` с password), которого больше нет. Ни одно поведение
профиля, outbox или правил доступа автоматически не проверяется. Поэтому
любое следующее исправление observations `user-profile` (C2, C3, C4 и далее)
нельзя проверить на регрессию. Этот change создает опорный набор тестов,
который фиксирует текущее baseline-поведение.

## What Changes

- Удаляются три закомментированных тестовых класса user-service:
  `controller/UserControllerTest`, `repository/UserRepositoryTest`,
  `service/impl/UserServiceImplTest`.
- Добавляются новые тесты, которые фиксируют поведение из baseline
  `user-profile`:
  - unit-тесты `UserServiceImpl`: создание профиля при первом чтении и запись
    `USER_CREATED`, роли из JWT, частичное изменение, удаление, админские
    чтения, 404;
  - unit-тесты `OutboxPublisher`: отправка в `user-created-events-topic` с
    ключом и значением, отметка processed, остановка цикла при ошибке;
  - security slice-тесты для `/users/me` и `/admin/users/**`: 401, 403 и
    допуск по ролям через реальную цепочку `SpringSecurityConfig` и
    `KCRoleConverter`, формат ошибки валидации;
  - репозиторные тесты на PostgreSQL в Testcontainers: ограничения
    `NOT NULL` и `UNIQUE` таблицы `users`, отсутствие уникальности телефона,
    выборка outbox.
- В `user-service/pom.xml` добавляется одна зависимость со scope `test`:
  `org.testcontainers:junit-jupiter`. Версия берется из Spring Boot BOM.
- Production-код, конфигурация, API, Kafka и Keycloak не меняются. Тесты
  фиксируют текущее поведение, включая поведение, описанное как OBS. Они его
  не исправляют.

### Out of scope

- Исправление любых observations `user-profile`, включая OBS-02, OBS-03,
  OBS-09, OBS-10 и OBS-11. Тесты закрепляют их текущее проявление, и будущие
  change будут менять эти тесты осознанно.
- Тесты `wishlists`, `CatalogClient`, gRPC (`UserGrpcService`,
  `KeycloakRoleServiceImpl`).
- Проверка assumptions (ASM-01 гонка, ASM-02 даты адреса) и запуск тестов в
  Jenkins: pipeline собирает с `-DskipTests`.

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

_Нет._ Поведение системы не меняется, delta spec не требуется
(`skip_specs: true`).

## Impact

- **Код:** только `user-service/src/test/**` и тестовые зависимости в
  `user-service/pom.xml`.
- **Окружение разработчика:** репозиторные тесты требуют запущенного Docker
  (Testcontainers, образ `postgres:16`). Без Docker они падают, а не
  пропускаются.
- **CI:** изменений нет, Jenkins по-прежнему собирает с `-DskipTests`.
- **Spec:** `user-profile` не меняется. Связь с OBS-12 нужно отразить в
  секции Observations после завершения change (задача в tasks).
