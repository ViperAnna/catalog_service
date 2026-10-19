# Design

## Context

- Все модули задают `<skipTests>true</skipTests>`; запуск —
  `mvn test -DskipTests=false`. Jenkins собирает `mvn clean package
  -DskipTests` и тестов не запускает. Профилей Maven и failsafe нет.
- Testcontainers (postgresql, junit-jupiter) подключен только в
  user-service, версии из BOM Spring Boot 3.3.4.
- Классы для unit-тестов выделены в `add-order-service` (D15): без Spring,
  каталог за интерфейсом `CatalogClient`, время через `Clock`.
- Ограничение окружения: Docker может быть недоступен на машине разработчика
  и в агенте Jenkins (не проверено).

## Goals / Non-Goals

**Goals:**
- Покрыть тестами каждый requirement новых specs и критичные сценарии
  (параллельное оформление, повторная отмена, чужие данные, снимки, outbox,
  идемпотентность уведомлений).
- Отделить быстрые тесты без Docker от интеграционных.
- Не менять поведение сборки других модулей.

**Non-Goals:**
- Тесты существующей функциональности других сервисов.
- Изменение `skipTests` других модулей и переход всего проекта на запуск
  тестов в CI.
- e2e на стенде (остаются ручными операциями по запросу пользователя).

## Decisions

### T1. Запуск в Maven (требует подтверждения пользователя)

Рекомендация: в `order-service/pom.xml` оставить `skipTests=true` по
умолчанию и разделить тесты тегом JUnit 5 `integration` (Testcontainers).
- `mvn -f order-service/pom.xml test -DskipTests=false` — все тесты;
- `-Dgroups='!integration'` — без Docker.

*Альтернативы:* (a) failsafe и `*IT` — новый плагин и соглашение, которых в
проекте нет; (b) `skipTests=false` только в order-service — меняет принятое
поведение сборки модуля.

### T2. Запуск в Jenkins (требует подтверждения пользователя)

Рекомендация: отдельный необязательный этап `Test order-service` при
`BUILD_ORDER_SERVICE == true`, выполняющий тесты без тега `integration`;
интеграционные — только если агент поддерживает Docker (параметр сборки).
Существующие этапы сборки с `-DskipTests` не меняются.

### T3. Docker и Testcontainers

Если Docker недоступен, интеграционные тесты не запускаются, и отчет явно
перечисляет непроверенные requirements. Замена PostgreSQL на H2 не
используется: CHECK, частичные индексы, `FOR UPDATE`, `lock_timeout` и
`ON CONFLICT` специфичны для PostgreSQL.

### T4. Заглушки

- Каталог: в тестах сервисов — мок `CatalogClient`; в тестах реализации
  клиента — `WebClient`, собранный с заглушкой `ExchangeFunction` (ответы
  200/4xx/5xx, задержка для таймаута). Новых зависимостей не нужно;
  `okhttp3.mockwebserver` не добавляется.
- Kafka: в тестах order-service проверяется запись outbox, а не брокер;
  публикатор — с моком `KafkaTemplate`. notification-service — вызов
  listener-метода напрямую и проверка `DefaultErrorHandler` с моком
  `ConsumerRecordRecoverer`, как в существующем `KafkaConfigTest`.
- SMTP: мок `JavaMailSender`.

## Risks / Trade-offs

- [Risk] Тесты найдут дефекты реализации → исправления фиксируются
  отдельными задачами и согласуются, а не делаются молча.
- [Risk] Параллельные тесты блокировок нестабильны → явная синхронизация
  (`CountDownLatch`) и таймауты, без `sleep`.
- [Risk] Без Docker в CI интеграционная часть не выполняется → отчет
  перечисляет непроверенные requirements; сборка без тестов не считается
  подтверждением.

## Open Questions

- Доступен ли Docker в агенте Jenkins и на машине разработчика (влияет только
  на то, где выполняется интеграционная часть, не на состав тестов).
