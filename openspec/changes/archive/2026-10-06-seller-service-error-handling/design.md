# Design

## Context

Мотивация описана в proposal.md, требования — в delta specs
`specs/seller-onboarding/spec.md` и `specs/seller-stores/spec.md`.

Текущее состояние:
- В seller-service нет `@ControllerAdvice`. `ResourceNotFoundException` и
  `ResourceConflictException` помечены `@ResponseStatus`, поэтому коды
  правильные, но тело стандартное (`timestamp`, `status`, `error`, `path`) и без
  текста: `server.error.include-message` не задан.
- `dto/Response {message, time}` в seller-service уже есть, но не используется.
  Тот же формат у `GlobalExceptionHandler` catalog-service и user-service.
- Классы контроллеров помечены `@PreAuthorize`. `AccessDeniedException` от
  method security выбрасывается внутри `DispatcherServlet` и проходит через
  `@ExceptionHandler`-ы, поэтому catch-all по `Exception` превратил бы 403 в 500.
- `PUT /seller-applications/update` принимает `@ModelAttribute`. В Spring 6.1
  ошибка валидации `@Valid @ModelAttribute` выбрасывается как
  `MethodArgumentNotValidException`, так же, как для `@RequestBody`.
- Ограничения `sellers` на стенде (`pg_constraint`):
  `ukpiumhdqb6mypa86tshmed7cuj UNIQUE (inn)`,
  `uk7rs36he9gn68sg1ewk0cakf4u UNIQUE (keycloak_user_id)`,
  `uk8757g69e614binmjpvpoeda7d UNIQUE (seller_name)`. Имена генерирует
  Hibernate 6 (`ImplicitNamingStrategyJpaCompliantImpl`) хешем от имени
  таблицы и колонок, поэтому они одинаковы на всех средах.
- Данные стенда: 1 Seller, дублей `lower(btrim(seller_name))` в `sellers` и в
  PENDING-заявках нет, имен с пробелами по краям нет.

## Goals / Non-Goals

**Goals:**
- Один формат тела ошибки для всех эндпоинтов seller-service, без утечки
  внутренних деталей в 500.
- Правильные тексты ошибок.
- sellerName уникален без учета регистра и пробелов, 409 вместо 500.

**Non-Goals:**
- Тело 401 и 403 из фильтров Spring Security.
- Обработка `UNIQUE (inn)`, обязательный email, gRPC как 503.

## Decisions

### D1. `GlobalExceptionHandler` на базе `ResponseEntityExceptionHandler`
`exception/GlobalExceptionHandler`:
- наследуется от `ResponseEntityExceptionHandler`, который уже покрывает
  стандартные исключения Spring MVC (`HttpMessageNotReadableException`,
  `TypeMismatchException` / `MethodArgumentTypeMismatchException`,
  `NoResourceFoundException`, `HttpRequestMethodNotSupportedException`,
  `HttpMediaTypeNotSupportedException` и др.) с правильными кодами;
- переопределяет `handleExceptionInternal`: тело заменяется на
  `Response(message, now)`, где message — `ProblemDetail.detail` из
  исключения (`ErrorResponse.getBody().getDetail()`), а при его отсутствии —
  стандартная фраза статуса. Для `HttpMessageNotReadableException` detail —
  `Failed to read request`, без текста Jackson с внутренними именами классов;
- переопределяет `handleMethodArgumentNotValid`: message
  `Validation failed: ` + `defaultMessage` ошибок полей через `, `, как в
  user-service и catalog-service;
- `@ExceptionHandler(ResourceNotFoundException)` → 404,
  `@ExceptionHandler(ResourceConflictException)` → 409, message — текст
  исключения;
- `@ExceptionHandler({AccessDeniedException, AuthenticationException})`
  пробрасывает исключение дальше (`throw ex`), чтобы его обработал
  `ExceptionTranslationFilter`, и коды 401/403 сохранились;
- `@ExceptionHandler(Exception)` → 500, message `Internal server error`,
  `log.error` с stack trace.

Альтернатива — скопировать обработчик catalog-service (отдельные
`@ExceptionHandler` на каждое исключение). Отклонена: без базового класса
405, 415 и 404 «нет маршрута» стали бы 500 через catch-all, а без catch-all
ошибки БД остались бы в стандартном теле. `@ResponseStatus` на исключениях
остается, но больше не используется для ответа.

### D2. Тексты ошибок
`MessageKeys`:

| Ключ | Было | Стало |
|---|---|---|
| `SELLER_NOT_FOUND` | `User not found with id: [%s].` | `Seller not found for user [%s].` |
| `SELLER_ALREADY_EXIST` | `User not found with id: [%s].` | `Seller already exists for user [%s].` |
| `APPLICATION_ALREADY_EXISTS` | `Application with email [%s] already exist` | `User already has a pending application.` |
| `INN_ALREADY_IN_REVIEW` | `Inn already in review` | `Inn [%s] already in review.` |
| `STORE_NOT_FOUND` | `Store notfound with id: [%s].` | `Store not found with id: [%s].` |
| `APPLICATION_ALREADY_REVIEWED` | `... already_reviewed.` | `Application with id: [%s] already reviewed.` |
| `APPLICATION_NOT_FOUND_FOR_USER` (новый) | — | `No applications found for user [%s].` |
| `SELLER_NAME_ALREADY_EXISTS` (новый) | — | `Seller with name [%s] already exists.` |
| `SELLER_NAME_ALREADY_IN_REVIEW` (новый) | — | `Seller name [%s] is already in review.` |

`getMyCurrentApplication` использует `APPLICATION_NOT_FOUND_FOR_USER` вместо
`APPLICATION_NOT_FOUND`, который подставлял userId как id заявки. В
сообщениях с userId показывается собственный `sub` пользователя, а не чужие
данные. Ключи `SELLER_NAME_ALREADY_IN_REVIEW` и `INN_ALREADY_IN_REVIEW`
раскрывают, что имя или ИНН заняты чужой заявкой, так же, как сейчас для ИНН.

### D3. Уникальность sellerName: та же схема, что у имени магазина
- При подаче REGISTRATION `SellerApplicationServiceImpl` берет
  `sellerName.trim()`, проверяет и сохраняет обрезанное имя в заявку.
- Проверки (JPQL, `lower(trim(...))`, `trim` в Hibernate для PostgreSQL —
  `btrim` по пробелам, как в индексе):
  ```java
  // SellerRepository
  @Query("select count(s) > 0 from Seller s where lower(trim(s.sellerName)) = lower(trim(:name))")
  boolean existsSameName(String name);
  // SellerApplicationRepository
  @Query("select count(a) > 0 from SellerApplication a where a.sellerApplicationStatus = :status " +
         "and lower(trim(a.sellerName)) = lower(trim(:name))")
  boolean existsSameNameWithStatus(String name, SellerApplicationStatus status);
  ```
- Порядок проверок при подаче: Seller пользователя → PENDING-заявка
  пользователя → ИНН → имя у Seller (`SELLER_NAME_ALREADY_EXISTS`) → имя в
  PENDING-заявках (`SELLER_NAME_ALREADY_IN_REVIEW`). Заявки UPDATE и
  DELETION несут имя существующего Seller, поэтому учитываются любые типы
  PENDING-заявок, а результат от этого не меняется.
- При одобрении REGISTRATION `ModeratorServiceImpl.approveRegistration`
  после `validateSellerNotExist` вызывает `existsSameName` (→ 409), затем
  `saveAndFlush(seller)` и только потом gRPC.
- Индекс в `schema.sql`, вторая команда рядом с индексом магазинов:
  ```sql
  CREATE UNIQUE INDEX IF NOT EXISTS uq_sellers_seller_name
      ON sellers (lower(btrim(seller_name)));
  ```
  Механизм SQL-инициализации уже настроен change
  `store-name-unique-per-seller` (`spring.sql.init.mode=always`,
  `defer-datasource-initialization=true`, порядок проверен по байткоду).
- Существующий `UNIQUE (seller_name)` Hibernate остается: `ddl-auto: update`
  его не удаляет, а убирать `unique = true` из `Seller` не нужно. Он
  избыточен: любое его нарушение нарушает и новый индекс.

Альтернатива — проверка только в сервисе. Отклонена: имя — общий ресурс для
всех пользователей, и гонка двух одобрений иначе дает дубль.

### D4. Перевод нарушения уникальности имени в 409
`saveAndFlush` в `approveRegistration` оборачивается в
`try/catch (DataIntegrityViolationException)`. В цепочке причин ищется
`SQLException` с `SQLState = "23505"`, в тексте которого есть
`uq_sellers_seller_name` **или** `uk8757g69e614binmjpvpoeda7d`. Тогда
выбрасывается `ResourceConflictException(SELLER_NAME_ALREADY_EXISTS, sellerName)`,
иначе исключение пробрасывается (и становится 500 через D1).

Оба имени нужны, потому что при точном совпадении имени PostgreSQL может
сообщить о любом из двух уникальных индексов (порядок проверки не
гарантирован). Имя Hibernate детерминировано (Context), но если оно
когда-нибудь изменится, точный дубль при гонке даст 500, а не дубль в данных.
Это допустимо. Помощник поиска `SQLState` выносится из `StoreServiceImpl` в
`util/UniqueViolations.isUniqueViolation(Throwable, String... constraintNames)`
и используется в обоих сервисах.

Проверка после ошибки повторным запросом невозможна: после ошибки в
PostgreSQL транзакция в состоянии aborted.

### D5. Стратегия тестов
- `GlobalExceptionHandlerTest`: standalone `MockMvc` (`MockMvcBuilders.standaloneSetup`)
  с реальными `StoreController` и `ModeratorController` поверх
  Mockito-сервисов и `setControllerAdvice(new GlobalExceptionHandler())`.
  Проверяются 400 (валидация `@RequestBody` и `@ModelAttribute` через
  `SellerApplicationController`, неизвестный enum, нечисловой id), 404, 409,
  500 (`RuntimeException` из сервиса, тело без текста исключения) и проброс
  `AccessDeniedException`.
- `SellerApplicationServiceImplTest`: `trim`, 409 для имени у Seller и в
  PENDING-заявке, новые тексты.
- `ModeratorServiceImplTest` (новый): 409 по имени до `saveAndFlush` и gRPC,
  перевод `23505` для обоих имен ограничения, проброс других ошибок, gRPC не
  вызывается при конфликте.
- `UniqueViolationsTest` и существующий `StoreServiceImplTest` (после выноса
  помощника).
- JPQL и индекс на реальном PostgreSQL проверяются вручную на стенде в
  транзакции с откатом (Testcontainers не используется, см. C1).

## Risks / Trade-offs

- [Клиенты, разбирающие стандартное тело Spring] → Известный клиент
  (catalog-service) смотрит только на коды. frontend к seller-service не
  обращается. Изменение отмечено как BREAKING.
- [Дубли имен на production блокируют старт] → Проверка перед деплоем
  (tasks), как для индекса магазинов.
- [Catch-all скрывает текст ошибок 500] → Текст пишется в лог seller-service.
- [Новые 409 при подаче] → Заявитель раньше узнает о занятом имени, это
  цель change.

## Migration Plan

1. Проверка `sellers` на дубли `lower(btrim(seller_name))` (стенд —
   выполнена: дублей нет, на production — повторить).
2. Деплой seller-service: `schema.sql` создаст `uq_sellers_seller_name`.
3. Проверка `pg_indexes` после старта.
4. Откат: прежний образ. Индекс остается и совместим со старым кодом (гонка
   даст 500). При необходимости `DROP INDEX uq_sellers_seller_name`.
