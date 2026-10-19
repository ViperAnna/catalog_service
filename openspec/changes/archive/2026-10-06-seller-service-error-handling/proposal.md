# Proposal

## Why

Ошибки seller-service клиент не может понять (`seller-onboarding` OBS-08,
OBS-09, OBS-13, ASM-08; `seller-stores` OBS-11):
- в seller-service нет `@ControllerAdvice`. 400, 404 и 409 приходят в
  стандартном теле Spring Boot без текста причины
  (`server.error.include-message` не задан), а ошибки БД и gRPC дают 500;
- часть текстов ошибок неверна: `SELLER_ALREADY_EXIST` говорит
  "User not found…", `APPLICATION_ALREADY_EXISTS` содержит литерал `[%s]`,
  `STORE_NOT_FOUND` — "notfound";
- уникальность `sellerName` проверяется только ограничением БД
  `UNIQUE (seller_name)` при одобрении. Занятое имя дает 500 модератору, а
  заявитель узнает об этом только из отказа. Имена `Shop` и `shop ` считаются
  разными.

Решение: единый формат ошибок `{message, time}`, как в catalog-service и
user-service, исправленные тексты и уникальность `sellerName` без учета
регистра и пробелов по краям с проверкой при подаче и при одобрении. Это
change G4 из разбора OBS `seller-onboarding`. Он закрывает и S4 из
`seller-stores`.

## What Changes

- **`GlobalExceptionHandler`** (`@RestControllerAdvice`, наследник
  `ResponseEntityExceptionHandler`) для всех эндпоинтов seller-service. Тело
  ответа об ошибке — `{message, time}` (существующий `dto/Response`, формат
  `time` — `yyyy-MM-dd HH:mm:ss`):
  - 400: ошибки валидации (`Validation failed: <сообщения полей>`),
    нечитаемое тело (например, неизвестное значение enum), неверный тип
    параметра пути;
  - 404 `ResourceNotFoundException`, 409 `ResourceConflictException` — с
    текстом исключения;
  - стандартные ошибки Spring MVC (404 нет маршрута, 405, 415 и т.п.) — с тем же
    кодом и кратким описанием;
  - прочие исключения (БД, gRPC) — 500 с текстом `Internal server error`, без
    деталей; исключение логируется;
  - `AccessDeniedException` и `AuthenticationException` не перехватываются и
    по-прежнему дают 403 и 401 через Spring Security.
- **Тексты ошибок** в `MessageKeys` исправлены. У `getMyCurrentApplication`
  своё сообщение вместо "Application not found with id: [<userId>]".
- **Уникальность `sellerName`:**
  - при подаче заявки REGISTRATION sellerName сохраняется без пробелов по
    краям (регистр сохраняется);
  - 409, если имя (после `trim`, без учета регистра) занято Seller в любом
    статусе или указано в PENDING-заявке;
  - при одобрении REGISTRATION та же проверка по Seller дает 409 до записи и
    до gRPC-вызова;
  - уникальный индекс `uq_sellers_seller_name ON sellers (lower(btrim(seller_name)))`
    через `schema.sql`. Нарушение индекса или существующего
    `UNIQUE (seller_name)` при гонке переводится в 409.
- **BREAKING (формат ответа):** тело ошибок seller-service меняется со
  стандартного тела Spring Boot (`timestamp`, `status`, `error`, `path`) на
  `{message, time}`. Коды ответов не меняются, кроме 500 → 409 для занятого
  sellerName при одобрении. catalog-service анализирует только коды ответов
  seller-service. frontend к seller-service не обращается (проверено поиском
  по `frontend/src`). Общий обработчик frontend уже читает `message`
  (`realApi.js`), так что новый формат с ним совместим.

### Out of scope

- 401 и 403 от фильтров Spring Security (без токена, нет роли) — тело не меняется.
- Перевод в 409 нарушения `UNIQUE (inn)` при одобрении. ИНН уже проверяется
  при подаче, гонку закрывает отдельное решение (`seller-onboarding` ASM-02).
- Обязательный email (OBS-07, G1) — его нарушение по-прежнему дает 500, но
  с телом `{message, time}`.
- gRPC-ошибки как 503 и согласованность с Keycloak (OBS-05, A1).
- Удаление неиспользуемых ключей `MessageKeys` (user, wishlist, product).

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-onboarding`: добавляются требования `Seller service error response format`,
  `Seller service error messages`, `Seller name uniqueness`; изменяются
  `Registration application constraints` и `Registration application approval`.
- `seller-stores`: добавляется требование `Store API error responses`.

## Impact

- **seller-service:** новый `exception/GlobalExceptionHandler`, `MessageKeys`,
  `SellerApplicationServiceImpl` (trim и проверка имени при подаче, новый
  текст «заявок нет»), `ModeratorServiceImpl` (проверка имени, `saveAndFlush`
  и перевод нарушения уникальности в 409), `SellerRepository` и
  `SellerApplicationRepository` (JPQL-проверки), `schema.sql` (второй индекс).
- **БД `seller_service`:** новый уникальный индекс на `sellers`, создается при
  старте. При дублях имен (без учета регистра и пробелов) сервис не стартует.
  На стенде дублей нет (проверено), на production проверку нужно повторить.
- **Клиенты:** формат тела ошибок (BREAKING, см. выше). catalog-service,
  Kafka, notification-service, Keycloak и user-service не меняются.
- **Тесты:** unit-тесты seller-service и тесты обработчика через
  standalone MockMvc.
