## ADDED Requirements

### Requirement: Seller service error response format
Ответ seller-service об ошибке на любом эндпоинте SHALL иметь тело JSON
`{message, time}`, где `time` имеет формат `yyyy-MM-dd HH:mm:ss`. Ошибки
валидации, нечитаемое тело и неверный тип параметра SHALL давать 400,
`ResourceNotFoundException` — 404, `ResourceConflictException` — 409 с текстом
исключения, прочие исключения — 500 с message `Internal server error` без
деталей. Ответы 401 и 403 Spring Security SHALL NOT меняться.

#### Scenario: Ошибка валидации
- **WHEN** пользователь подает заявку на регистрацию с inn из 11 цифр
- **THEN** seller-service отвечает 400 с телом `{message, time}`, где message начинается с `Validation failed: ` и содержит `INN must contain 10 or 12 digits.`

#### Scenario: Ошибка валидации формы
- **WHEN** продавец отправляет `PUT /seller-applications/update` с paymentAccount из 19 цифр
- **THEN** seller-service отвечает 400 с телом `{message, time}`, где message содержит `Payment account must contain exactly 20 digits.`

#### Scenario: Неизвестное значение enum
- **WHEN** модератор отклоняет заявку с телом `{"status": "UNKNOWN"}`
- **THEN** seller-service отвечает 400 с телом `{message, time}`

#### Scenario: Ресурс не найден
- **WHEN** модератор запрашивает `GET /moderator/seller-applications/999999`, а заявки нет
- **THEN** seller-service отвечает 404 с message `Application not found with id: [999999].`

#### Scenario: Конфликт
- **WHEN** модератор одобряет уже одобренную заявку 5
- **THEN** seller-service отвечает 409 с message `Application with id: [5] already reviewed.`

#### Scenario: Внутренняя ошибка
- **WHEN** при обработке запроса возникает ошибка БД или gRPC, не переведенная в 4xx
- **THEN** seller-service отвечает 500 с message `Internal server error`, без текста исключения и stack trace
- **AND** исключение записывается в лог

#### Scenario: Нет роли
- **WHEN** пользователь только с ролью USER вызывает `GET /sellers/me`
- **THEN** seller-service отвечает 403, как и раньше

### Requirement: Seller service error messages
seller-service SHALL возвращать в message ошибки текст, соответствующий
причине, без незаполненных шаблонов вида `[%s]`: `Seller not found for user [<userId>].`,
`Seller already exists for user [<userId>].`, `User already has a pending application.`,
`Inn [<inn>] already in review.`, `No applications found for user [<userId>].`,
`Store not found with id: [<id>].`, `Application with id: [<id>] already reviewed.`

#### Scenario: Повторная PENDING-заявка
- **WHEN** пользователь с PENDING-заявкой подает еще одну заявку
- **THEN** seller-service отвечает 409 с message `User already has a pending application.`

#### Scenario: Пользователь уже продавец
- **WHEN** пользователь `u-1`, для которого существует Seller, подает заявку REGISTRATION
- **THEN** seller-service отвечает 409 с message `Seller already exists for user [u-1].`

#### Scenario: Заявок нет
- **WHEN** пользователь `u-2` без заявок вызывает `GET /seller-applications/me`
- **THEN** seller-service отвечает 404 с message `No applications found for user [u-2].`

### Requirement: Seller name uniqueness
sellerName SHALL быть уникальным среди Seller в любом статусе после удаления
пробелов по краям и без учета регистра. При подаче REGISTRATION sellerName
SHALL сохраняться без пробелов по краям. Уникальность SHALL обеспечиваться
проверками при подаче и при одобрении REGISTRATION и уникальным индексом
`uq_sellers_seller_name` по `lower(btrim(seller_name))`. Нарушение индекса или
`UNIQUE (seller_name)` при сохранении Seller SHALL давать 409.

#### Scenario: Имя с пробелами по краям
- **WHEN** пользователь подает заявку REGISTRATION с sellerName "  My Shop  "
- **THEN** заявка сохраняется с sellerName "My Shop"

#### Scenario: Конкурентное одобрение
- **WHEN** проверка имени при одобрении прошла, но сохранение Seller нарушает `uq_sellers_seller_name` или `UNIQUE (seller_name)` (SQLState 23505)
- **THEN** seller-service отвечает 409 с message `Seller with name [<sellerName>] already exists.`
- **AND** транзакция откатывается, заявка остается PENDING, gRPC-вызов не выполняется

#### Scenario: Индекс при старте
- **WHEN** seller-service стартует
- **THEN** `schema.sql` после `ddl-auto: update` выполняет `CREATE UNIQUE INDEX IF NOT EXISTS uq_sellers_seller_name ON sellers (lower(btrim(seller_name)))`
- **AND** при дублях имен в `sellers` сервис не стартует

## MODIFIED Requirements

### Requirement: Registration application constraints
seller-service SHALL отклонять заявку на регистрацию с кодом 409, если для
пользователя существует Seller в любом статусе, если inn принадлежит
существующему Seller в любом статусе или указан в любой заявке в статусе
PENDING, а также если sellerName после удаления пробелов по краям и без учета
регистра совпадает с именем Seller в любом статусе или с sellerName любой
заявки в статусе PENDING.

#### Scenario: Пользователь уже продавец
- **WHEN** для keycloakUserId пользователя существует Seller со статусом ACTIVE
- **THEN** заявка REGISTRATION отклоняется с 409

#### Scenario: Бывший продавец
- **WHEN** для пользователя существует Seller со статусом DELETED
- **THEN** заявка REGISTRATION отклоняется с 409

#### Scenario: ИНН на рассмотрении у другого пользователя
- **WHEN** inn указан в PENDING-заявке другого пользователя
- **THEN** заявка REGISTRATION отклоняется с 409

#### Scenario: ИНН удаленного продавца
- **WHEN** inn принадлежит Seller со статусом DELETED
- **THEN** заявка REGISTRATION отклоняется с 409

#### Scenario: Имя занято продавцом
- **WHEN** существует Seller с sellerName "Shop" (в любом статусе) и пользователь подает заявку с sellerName "  SHOP "
- **THEN** заявка REGISTRATION отклоняется с 409 и message `Seller with name [SHOP] already exists.`

#### Scenario: Имя на рассмотрении
- **WHEN** sellerName "shop" указан в PENDING-заявке другого пользователя и пользователь подает заявку с sellerName "Shop"
- **THEN** заявка REGISTRATION отклоняется с 409 и message `Seller name [Shop] is already in review.`

#### Scenario: Имя из рассмотренной заявки
- **WHEN** sellerName "Shop" указан только в REJECTED-заявке, а Seller с таким именем нет
- **THEN** ограничение имени не препятствует подаче

**Observation**: OBS-04

### Requirement: Registration application approval
При одобрении заявки REGISTRATION seller-service SHALL проверить, что Seller для
keycloakUserId заявки не существует (иначе 409) и что sellerName заявки не
занят другим Seller по правилам `Seller name uniqueness` (иначе 409), SHALL
создать Seller со статусом ACTIVE и значениями keycloakUserId, sellerName, inn,
phoneNumber, email, paymentAccount из заявки, записав его в БД до gRPC-вызова,
и SHALL синхронно запросить у user-service назначение роли SELLER пользователю
keycloakUserId.

#### Scenario: Успешное одобрение регистрации
- **WHEN** модератор одобряет PENDING-заявку REGISTRATION и user-service успешно назначает роль
- **THEN** создается Seller(ACTIVE) с данными заявки
- **AND** пользователю в Keycloak назначена realm-роль SELLER
- **AND** заявка APPROVED, в outbox записано SELLER_APPROVED

#### Scenario: Seller уже существует
- **WHEN** на момент одобрения для пользователя уже существует Seller
- **THEN** seller-service отвечает 409, gRPC-вызов не выполняется

#### Scenario: Имя занято
- **WHEN** на момент одобрения sellerName заявки совпадает (без учета регистра и пробелов по краям) с именем существующего Seller
- **THEN** seller-service отвечает 409 с message `Seller with name [<sellerName>] already exists.`
- **AND** Seller не создается, заявка остается PENDING, gRPC-вызов не выполняется

#### Scenario: Нарушено ограничение БД на Seller
- **WHEN** inn уже занят другим Seller, либо email заявки равен null
- **THEN** сохранение Seller завершается ошибкой БД до gRPC-вызова
- **AND** транзакция откатывается, заявка остается PENDING, ответ 500 с message `Internal server error`

**Observation**: OBS-05, OBS-07
