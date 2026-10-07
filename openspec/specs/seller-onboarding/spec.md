# seller-onboarding Specification

## Purpose
Baseline (as-is) поведения, по которому пользователь становится продавцом и
перестает им быть: подача заявок REGISTRATION / UPDATE / DELETION в
seller-service, их модерация, ведение сущности Seller, синхронизация realm-роли
SELLER в Keycloak через user-service (gRPC), публикация событий через outbox в
Kafka-топик `seller-events-topic` и email-уведомления notification-service.
Спецификация описывает фактическое текущее поведение кода, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Незакоммиченные изменения
  рабочей копии на дату снятия baseline не затрагивают описанное поведение.
- Метод: статический анализ кода и конфигурации. В seller-service тестов нет,
  поэтому runtime-поведение не проверялось.
- Затронутые компоненты: `seller-service` (REST, JPA/PostgreSQL, outbox, gRPC-клиент),
  `user-service` (gRPC-сервер, Keycloak Admin client), Keycloak realm
  `user-service-realm`, Kafka, `notification-service` (consumer, SMTP).
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Если requirement описывает поведение,
  которое помечено как observation, оно зафиксировано потому, что так работает
  код сейчас, а не потому, что оно признано правильным. Изменять его нужно
  отдельным change.
- Пути указаны относительно seller-service. Через api-gateway они доступны без
  изменений, через nginx — с префиксом `/api`.

## Requirements

### Requirement: Role-based access to onboarding API
seller-service SHALL разрешать запросы к `/seller-applications/**` только с JWT,
содержащим realm-роль USER, а к `/moderator/seller-applications/**` только с
JWT, содержащим realm-роль MODERATOR. Роли SHALL извлекаться из claim
`realm_access.roles` и сопоставляться без учета регистра. Идентификатором
пользователя SHALL служить claim `sub`.

#### Scenario: Запрос без токена
- **WHEN** клиент вызывает `GET /seller-applications/me` без Bearer-токена
- **THEN** seller-service отвечает 401

#### Scenario: Нет роли MODERATOR
- **WHEN** клиент с JWT без роли MODERATOR вызывает `GET /moderator/seller-applications/pending`
- **THEN** seller-service отвечает 403

#### Scenario: Модератор подает собственную заявку
- **WHEN** пользователь с ролями USER и MODERATOR подает заявку
- **THEN** заявка принимается на общих основаниях
- **AND** проверки, запрещающей этому пользователю модерировать свою заявку, нет

**Observation**: OBS-10

### Requirement: Seller registration application submission
seller-service SHALL принимать `POST /seller-applications/registration` с телом
JSON и при успешных проверках SHALL создавать заявку с типом REGISTRATION,
статусом PENDING, keycloakUserId текущего пользователя и полями sellerName,
inn, phoneNumber, email, paymentAccount из запроса, причем phoneNumber
нормализуется по правилам `Seller phone normalization`. Ответ SHALL иметь код
201 и содержать созданную заявку.

#### Scenario: Успешная подача
- **WHEN** пользователь, удовлетворяющий ограничениям на регистрацию, отправляет валидную заявку
- **THEN** создается заявка REGISTRATION/PENDING
- **AND** ответ имеет код 201 и содержит id, поля заявки, sellerApplicationStatus=PENDING, applicationType=REGISTRATION
- **AND** в outbox в той же транзакции записывается событие SELLER_APPLICATION_CREATED

### Requirement: Registration application field validation
seller-service SHALL отклонять с кодом 400 заявку на регистрацию, если
sellerName пуст или длиннее 50 символов, inn не состоит ровно из 10 или 12 цифр,
phoneNumber отсутствует или не соответствует `^(\+7|8)\d{10}$`, email
отсутствует, не является email или длиннее 100 символов, paymentAccount не
состоит ровно из 20 цифр.

#### Scenario: Некорректный ИНН
- **WHEN** inn содержит 11 цифр
- **THEN** seller-service отвечает 400 и заявку не создает

#### Scenario: Заявка без email
- **WHEN** поле email отсутствует или пустое, остальные поля валидны
- **THEN** seller-service отвечает 400 с message, содержащим `Email is required.`, и заявку не создает

#### Scenario: Телефон в старом формате
- **WHEN** phoneNumber равен "1234567"
- **THEN** seller-service отвечает 400 и заявку не создает

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

### Requirement: Single pending application per user
seller-service SHALL отклонять с кодом 409 подачу заявки любого типа
(REGISTRATION, UPDATE, DELETION), если у пользователя уже есть заявка любого
типа в статусе PENDING. Проверка SHALL выполняться на уровне приложения перед
сохранением заявки.

#### Scenario: Повторная подача при PENDING
- **WHEN** у пользователя есть PENDING-заявка типа REGISTRATION и он подает еще одну заявку
- **THEN** seller-service отвечает 409 и новую заявку не создает

#### Scenario: Подача после рассмотрения
- **WHEN** все заявки пользователя в статусах APPROVED или REJECTED
- **THEN** ограничение на одну PENDING-заявку не препятствует подаче

**Assumption**: ASM-02

### Requirement: Seller update application submission
seller-service SHALL принимать `PUT /seller-applications/update` с параметрами
формы phoneNumber (обязателен), email (необязателен) и paymentAccount
(обязателен) и SHALL создавать заявку UPDATE/PENDING, только если Seller
пользователя в статусе ACTIVE. В заявку SHALL копироваться sellerName и inn
Seller, из запроса — нормализованный phoneNumber, paymentAccount и email, а без
email — email Seller. Ответ SHALL иметь код 200. В той же транзакции SHALL
записываться SELLER_APPLICATION_CREATED.

#### Scenario: Успешная подача изменения
- **WHEN** ACTIVE-продавец без PENDING-заявок отправляет валидные phoneNumber, email, paymentAccount
- **THEN** создается заявка UPDATE/PENDING с sellerName и inn из Seller и phoneNumber, email, paymentAccount из запроса
- **AND** в outbox в той же транзакции записывается событие SELLER_APPLICATION_CREATED с applicationId, keycloakUserId, sellerName и email созданной заявки

#### Scenario: Подача изменения без email
- **WHEN** ACTIVE-продавец с email "shop@example.com" без PENDING-заявок отправляет валидную заявку UPDATE без email
- **THEN** создается заявка UPDATE/PENDING с email "shop@example.com"
- **AND** в outbox записывается событие SELLER_APPLICATION_CREATED с email "shop@example.com"

#### Scenario: Старое имя параметра телефона
- **WHEN** форма содержит `phone`, но не содержит `phoneNumber`
- **THEN** seller-service отвечает 400 и заявку не создает

#### Scenario: Отклоненная подача изменения не создает событие
- **WHEN** подача UPDATE отклоняется с 400, 404 или 409
- **THEN** заявка не создается и событие в outbox не записывается

#### Scenario: Продавец не найден
- **WHEN** для пользователя нет Seller
- **THEN** seller-service отвечает 404

#### Scenario: Продавец не ACTIVE
- **WHEN** Seller пользователя имеет статус DELETED
- **THEN** seller-service отвечает 409

#### Scenario: Невалидный счет
- **WHEN** paymentAccount содержит не 20 цифр
- **THEN** seller-service отвечает 400

### Requirement: Seller deletion application submission
seller-service SHALL принимать `DELETE /seller-applications/deletion` с телом
JSON `{reason}` (обязательно, не длиннее 500 символов) и SHALL создавать заявку
DELETION/PENDING со снимком sellerName, inn, phoneNumber, email, paymentAccount
текущего Seller и requestReason = reason, только если Seller существует и
ACTIVE. Ответ SHALL иметь код 200. В той же транзакции SHALL записываться
событие SELLER_APPLICATION_CREATED.

#### Scenario: Успешная подача удаления
- **WHEN** ACTIVE-продавец без PENDING-заявок отправляет непустую причину
- **THEN** создается заявка DELETION/PENDING с данными Seller и requestReason
- **AND** в outbox в той же транзакции записывается событие SELLER_APPLICATION_CREATED с applicationId, keycloakUserId, sellerName и email созданной заявки

#### Scenario: Отклоненная подача удаления не создает событие
- **WHEN** подача DELETION отклоняется с 400, 404 или 409
- **THEN** заявка не создается и событие в outbox не записывается

#### Scenario: Причина не указана
- **WHEN** reason пуст
- **THEN** seller-service отвечает 400

### Requirement: Own applications retrieval
seller-service SHALL возвращать по `GET /seller-applications/me` заявку
текущего пользователя с наибольшим createdAt (любого типа и статуса), а по
`GET /seller-applications/history` все заявки пользователя по убыванию
createdAt. Если у пользователя нет заявок, `/me` SHALL отвечать 404, а
`/history` SHALL возвращать пустой список.

#### Scenario: Последняя заявка
- **WHEN** у пользователя есть отклоненная REGISTRATION и более поздняя PENDING REGISTRATION
- **THEN** `/me` возвращает PENDING-заявку

#### Scenario: Нет заявок
- **WHEN** пользователь без заявок вызывает `/me`
- **THEN** seller-service отвечает 404

### Requirement: Moderator application retrieval
seller-service SHALL возвращать по `GET /moderator/seller-applications/pending`
все заявки в статусе PENDING любого типа и любого пользователя по возрастанию
createdAt, а по `GET /moderator/seller-applications/{id}` заявку с указанным id
в любом статусе. Для несуществующего id SHALL возвращаться 404.

#### Scenario: Очередь модерации
- **WHEN** модератор запрашивает pending-список
- **THEN** первой идет самая ранняя по createdAt PENDING-заявка

#### Scenario: Заявка не найдена
- **WHEN** модератор запрашивает несуществующий id
- **THEN** seller-service отвечает 404

### Requirement: Seller account retrieval
seller-service SHALL разрешать `GET /sellers/me` только с JWT, содержащим
realm-роль SELLER, и SHALL возвращать Seller, у которого keycloakUserId
совпадает с claim `sub`, в виде объекта с полями id, type, sellerStatus,
moderatorComment, createdAt. Поля type и moderatorComment SHALL всегда иметь
значение null. Если Seller не найден, SHALL возвращаться 404. Статус Seller
SHALL NOT проверяться.

#### Scenario: Активный продавец
- **WHEN** пользователь с ролью SELLER и Seller в статусе ACTIVE вызывает `GET /sellers/me`
- **THEN** ответ 200 содержит id, sellerStatus = ACTIVE, createdAt
- **AND** type = null и moderatorComment = null

#### Scenario: Нет роли SELLER
- **WHEN** пользователь только с ролью USER вызывает `GET /sellers/me`
- **THEN** seller-service отвечает 403

#### Scenario: Роль SELLER без записи Seller
- **WHEN** пользователь с ролью SELLER, для которого нет Seller, вызывает `GET /sellers/me`
- **THEN** seller-service отвечает 404

#### Scenario: Seller в статусе DELETED
- **WHEN** запрос приходит с JWT, в котором еще есть роль SELLER, от пользователя, чей Seller имеет статус DELETED
- **THEN** ответ 200 содержит sellerStatus = DELETED

**Observation**: OBS-15
**Assumption**: ASM-01

### Requirement: Moderation of pending applications only
seller-service SHALL выполнять approve и reject только для заявки в статусе
PENDING. Для заявки в любом другом статусе SHALL возвращаться 409 без изменения
состояния, для несуществующего id SHALL возвращаться 404. Статусы APPROVED и
REJECTED SHALL быть конечными. Переход в статус CANCELED SHALL NOT выполняться
ни одной операцией.

#### Scenario: Повторное рассмотрение
- **WHEN** модератор одобряет заявку в статусе REJECTED
- **THEN** seller-service отвечает 409 и заявка остается REJECTED

#### Scenario: Отклонение уже одобренной заявки
- **WHEN** модератор отклоняет заявку в статусе APPROVED
- **THEN** seller-service отвечает 409

### Requirement: Application approval general rules
`POST /moderator/seller-applications/{id}/approve` SHALL выполнять действия,
соответствующие типу заявки, переводить заявку в статус APPROVED и записывать в
outbox событие SELLER_APPROVED, причем для заявок любого типа (REGISTRATION,
UPDATE, DELETION). Все изменения SHALL выполняться в одной транзакции
seller-service. Успешный ответ SHALL иметь код 204 без тела.

#### Scenario: Одобрение заявки на изменение
- **WHEN** модератор одобряет PENDING-заявку UPDATE
- **THEN** заявка становится APPROVED
- **AND** в outbox записывается SELLER_APPROVED

#### Scenario: Одобрение заявки на удаление
- **WHEN** модератор одобряет PENDING-заявку DELETION
- **THEN** в outbox записывается SELLER_APPROVED

**Observation**: OBS-02

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

### Requirement: Update application approval
При одобрении заявки UPDATE seller-service SHALL найти Seller по keycloakUserId
заявки (иначе 404) и SHALL перенести в него каждое из полей sellerName, inn,
phoneNumber, email, paymentAccount заявки, значение которого не равно null. При
этом SHALL NOT изменяться статус Seller и SHALL NOT вызываться user-service.
Статус Seller в момент одобрения SHALL NOT проверяться.

#### Scenario: Пустое значение не перезаписывает данные
- **WHEN** в одобряемой заявке UPDATE phoneNumber равен null (заявка подана до этого change)
- **THEN** phoneNumber Seller не меняется

#### Scenario: Обновление счета
- **WHEN** в одобряемой заявке UPDATE указан новый paymentAccount
- **THEN** Seller получает новый paymentAccount

#### Scenario: Обновление телефона
- **WHEN** в одобряемой заявке UPDATE phoneNumber равен "+79990001122"
- **THEN** Seller получает phoneNumber "+79990001122"

#### Scenario: Письмо об одобрении изменения
- **WHEN** одобряется заявка UPDATE, поданная без email
- **THEN** событие SELLER_APPROVED содержит email Seller, а не null

### Requirement: Deletion application approval
При одобрении заявки DELETION seller-service SHALL найти Seller по
keycloakUserId заявки (иначе 404), SHALL установить ему статус DELETED без
физического удаления записи и без изменения связанных магазинов и SHALL
синхронно запросить у user-service снятие роли SELLER.

#### Scenario: Успешное одобрение удаления
- **WHEN** модератор одобряет PENDING-заявку DELETION и user-service успешно снимает роль
- **THEN** Seller получает статус DELETED и остается в БД вместе с магазинами
- **AND** у пользователя в Keycloak снята realm-роль SELLER
- **AND** заявка APPROVED

**Observation**: OBS-04

### Requirement: Application rejection
seller-service SHALL принимать `POST /moderator/seller-applications/{id}/reject`
с телом JSON `{moderatorComment}`, где moderatorComment (причина отказа)
обязателен, не пуст и не длиннее 500 символов, иначе 400 без изменения
заявки. Поле status в теле SHALL NOT входить в контракт: если оно передано,
оно игнорируется. seller-service SHALL перевести заявку в REJECTED, сохранить
moderatorComment и записать в outbox SELLER_REJECTED. Успешный ответ SHALL
иметь код 200 без тела.

#### Scenario: Отклонение с комментарием
- **WHEN** модератор отклоняет PENDING-заявку с moderatorComment
- **THEN** заявка REJECTED, комментарий сохранен, в outbox записано SELLER_REJECTED с этим moderatorComment

#### Scenario: Причина не указана
- **WHEN** тело запроса не содержит moderatorComment или он пуст или состоит из пробелов
- **THEN** seller-service отвечает 400 с message, содержащим `Rejection reason is required.`
- **AND** заявка остается PENDING и событие не записывается

#### Scenario: Поле status не передано
- **WHEN** тело запроса содержит только moderatorComment
- **THEN** заявка становится REJECTED, ответ 200

#### Scenario: Передан status APPROVED
- **WHEN** тело содержит status = APPROVED (или любое другое значение) и moderatorComment
- **THEN** поле status игнорируется, заявка становится REJECTED, ответ 200

**Observation**: OBS-16

### Requirement: Seller role sync gRPC contract
seller-service SHALL вызывать сервис `user.UserService` методами
`AssignSellerRole` и `RemoveSellerRole` с сообщением `{keycloakUserId: string}`
и пустым ответом через blocking stub по адресу `user-service:9090` без TLS и
без аутентификации вызова, с дедлайном 5 секунд на вызов. Адрес SHALL NOT
определяться через service discovery.

#### Scenario: Назначение роли при одобрении регистрации
- **WHEN** одобряется заявка REGISTRATION
- **THEN** seller-service вызывает AssignSellerRole с keycloakUserId заявки

#### Scenario: Превышен дедлайн
- **WHEN** user-service не ответил за 5 секунд
- **THEN** вызов завершается ошибкой DEADLINE_EXCEEDED

**Assumption**: ASM-05

### Requirement: Seller role change in Keycloak
user-service SHALL обрабатывать AssignSellerRole и RemoveSellerRole,
соответственно добавляя или удаляя realm-роль `SELLER` пользователя с id
keycloakUserId в realm из `application.realm` через Keycloak Admin API от имени
клиента, аутентифицированного по grant client_credentials. При любой ошибке
user-service SHALL отвечать gRPC-статусом INTERNAL.

#### Scenario: Успешное назначение
- **WHEN** user-service получает AssignSellerRole для существующего пользователя Keycloak
- **THEN** пользователю добавлена realm-роль SELLER и возвращен пустой ответ

#### Scenario: Ошибка Keycloak
- **WHEN** Keycloak недоступен или пользователь не найден
- **THEN** user-service отвечает INTERNAL

**Assumption**: ASM-01, ASM-06

### Requirement: Approval rollback on role sync failure
Если gRPC-вызов AssignSellerRole или RemoveSellerRole при одобрении завершился
ошибкой, seller-service SHALL откатить транзакцию одобрения (изменения Seller,
заявки и outbox) и SHALL ответить кодом 5xx. Заявка SHALL остаться в статусе
PENDING. Компенсирующее изменение роли в Keycloak SHALL NOT выполняться.

#### Scenario: user-service недоступен при одобрении регистрации
- **WHEN** AssignSellerRole завершился ошибкой
- **THEN** Seller не создан, заявка остается PENDING, событие не записано, ответ 500

#### Scenario: Ошибка после успешного назначения роли
- **WHEN** роль назначена, но фиксация транзакции завершилась ошибкой
- **THEN** изменения в БД откатываются, а роль SELLER в Keycloak остается назначенной

**Observation**: OBS-05

### Requirement: Outbox event recording
seller-service SHALL записывать событие в таблицу `outbox_events` в той же
транзакции, что и изменение заявки, с aggregateType `SELLER_APPLICATION`,
aggregateId равным id заявки, eventType, payload в JSON, createdAt текущего
времени и processed = false. События SHALL записываться только при подаче
заявки любого типа (REGISTRATION, UPDATE, DELETION), при одобрении и при
отклонении заявки. Payload SELLER_APPROVED SHALL содержать applicationType
одобренной заявки.

#### Scenario: Payload событий
- **WHEN** записывается SELLER_APPLICATION_CREATED или SELLER_APPROVED
- **THEN** payload содержит applicationId, keycloakUserId, sellerName, email из заявки

#### Scenario: Одинаковое событие для всех типов заявок
- **WHEN** подается заявка REGISTRATION, UPDATE или DELETION
- **THEN** записывается событие с eventType SELLER_APPLICATION_CREATED и одним и тем же набором полей payload
- **AND** payload не содержит тип заявки

#### Scenario: Тип заявки в одобрении
- **WHEN** одобряется заявка типа REGISTRATION, UPDATE или DELETION
- **THEN** payload SELLER_APPROVED дополнительно содержит applicationType с этим типом

#### Scenario: Payload отклонения
- **WHEN** записывается SELLER_REJECTED
- **THEN** payload дополнительно содержит moderatorComment и не содержит applicationType

#### Scenario: Откат транзакции
- **WHEN** транзакция подачи или модерации откатывается
- **THEN** событие в outbox не сохраняется

### Requirement: Outbox publishing to seller-events-topic
seller-service SHALL каждые 3 секунды (fixed rate) в одной транзакции выбирать
до 100 событий с processed = false по возрастанию createdAt и публиковать их по
одному с ключом aggregateId в топик по aggregateType, ожидая подтверждения не
более 10 секунд. Событие SHALL помечаться processed только после
подтверждения. При ошибке отправки обработка пачки SHALL прекращаться.
Обработанные события SHALL NOT удаляться.

#### Scenario: Kafka недоступна
- **WHEN** отправка события завершилась ошибкой или таймаутом
- **THEN** событие и следующие за ним остаются processed = false до следующего цикла
- **AND** события, отправленные ранее в этом цикле, помечаются processed

#### Scenario: Неизвестный тип события
- **WHEN** eventType записи не является значением SellerEventType для SELLER_APPLICATION или StoreEventType для STORE
- **THEN** запись помечается processed без публикации

#### Scenario: Событие заявки продавца
- **WHEN** в outbox есть запись SELLER_APPLICATION
- **THEN** она публикуется в `seller-events-topic`

#### Scenario: Событие магазина
- **WHEN** в outbox есть запись STORE / STORE_DELETED
- **THEN** она публикуется в `store-events-topic`, а не в `seller-events-topic`

#### Scenario: Неизвестный aggregateType
- **WHEN** aggregateType записи не SELLER_APPLICATION и не STORE
- **THEN** запись помечается processed без публикации

**Assumption**: ASM-02, ASM-03

### Requirement: Seller events message format
Значение сообщения в `seller-events-topic` SHALL быть JSON-конвертом
`{aggregateType, eventType, payload}`, где eventType — одно из
SELLER_APPLICATION_CREATED, SELLER_APPROVED, SELLER_REJECTED, а payload — JSON
события, сериализованный в строку. Ключ сообщения SHALL быть строковым id заявки.

#### Scenario: Сообщение об отклонении
- **WHEN** публикуется SELLER_REJECTED для заявки 42
- **THEN** ключ сообщения "42", eventType "SELLER_REJECTED", payload — строка с JSON, включающим moderatorComment

### Requirement: Seller event email notifications
notification-service SHALL потреблять `seller-events-topic` (consumer group
`notification-group`) и отправлять email от `spring.mail.from`: на
SELLER_APPLICATION_CREATED — на `notification.moderator.email`, на
SELLER_APPROVED и SELLER_REJECTED — на email из события. Письмо на
SELLER_APPROVED SHALL выбираться по applicationType. Сообщение без eventType
или payload SHALL пропускаться с предупреждением в логе.

#### Scenario: Новая заявка
- **WHEN** получено SELLER_APPLICATION_CREATED
- **THEN** модератору отправляется письмо "New application!" с sellerName и email заявителя

#### Scenario: Одобрение
- **WHEN** получено SELLER_APPROVED с applicationType REGISTRATION
- **THEN** на email из события отправляется письмо "Seller application approved!" о том, что магазин можно использовать

#### Scenario: Одобрение изменения данных
- **WHEN** получено SELLER_APPROVED с applicationType UPDATE
- **THEN** на email из события отправляется письмо "Seller data change approved" о том, что новые данные продавца вступили в силу

#### Scenario: Одобрение удаления
- **WHEN** получено SELLER_APPROVED с applicationType DELETION
- **THEN** на email из события отправляется письмо "Seller account deletion approved" о том, что аккаунт продавца закрыт
- **AND** письмо не содержит фразы "Your shop can now be used as a seller shop"

#### Scenario: Одобрение без типа заявки
- **WHEN** получено SELLER_APPROVED без applicationType или с пустым значением (например, событие записано до этого change)
- **THEN** отправляется письмо "Seller application approved!", как для REGISTRATION

#### Scenario: Одобрение с неизвестным типом заявки
- **WHEN** получено SELLER_APPROVED с непустым applicationType, отличным от REGISTRATION, UPDATE и DELETION (например, "FOO" или "update")
- **THEN** письмо не отправляется, в лог пишется ошибка с applicationId и значением типа
- **AND** сообщение обрабатывается по правилам `Seller events consumer error handling` для неизвестного типа заявки

#### Scenario: Отклонение
- **WHEN** получено SELLER_REJECTED
- **THEN** на email из события отправляется письмо "Seller application rejected" с moderatorComment в качестве причины

#### Scenario: Telegram
- **WHEN** обрабатывается любое событие seller-events-topic
- **THEN** уведомление в Telegram не отправляется

### Requirement: Seller events consumer error handling
Ошибка отправки email (MailException) SHALL логироваться без повторной попытки,
и сообщение SHALL считаться обработанным. Ошибка обработки сообщения (например,
десериализации) SHALL повторяться 3 раза с интервалом 1 секунда, после чего
сообщение SHALL публиковаться в топик `seller-events-topic.DLT`. Сообщение
SELLER_APPROVED с неизвестным непустым applicationType SHALL публиковаться в
`seller-events-topic.DLT` сразу, без повторов. Потребителя DLT SHALL NOT
существовать.

#### Scenario: SMTP недоступен
- **WHEN** отправка письма завершилась MailException
- **THEN** ошибка логируется, повторной отправки нет

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения не является валидным конвертом
- **THEN** после 3 повторов сообщение публикуется в seller-events-topic.DLT

#### Scenario: Неизвестный тип заявки
- **WHEN** получено SELLER_APPROVED с applicationType "FOO"
- **THEN** сообщение после первой попытки публикуется в seller-events-topic.DLT с исходными ключом и значением, повторов нет, письмо не отправлено

**Assumption**: ASM-03, ASM-04, ASM-07

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
- **WHEN** тело запроса не читается: некорректный JSON или неизвестное значение поля-перечисления (в теле reject таких полей после удаления status нет, поэтому пример — обрезанный JSON `{"moderatorComment": `)
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

### Requirement: Seller phone normalization
seller-service SHALL принимать phoneNumber в заявках REGISTRATION и UPDATE
только в формах `+7XXXXXXXXXX` и `8XXXXXXXXXX`, где X — цифра, и SHALL
сохранять его в заявке в формате `+7XXXXXXXXXX`: у формы `8XXXXXXXXXX` первая
цифра заменяется на `+7`. Форма `7XXXXXXXXXX` без плюса, пробелы, скобки,
дефисы и номера других стран SHALL отклоняться с кодом 400.

#### Scenario: Регистрация с восьмеркой
- **WHEN** пользователь подает заявку REGISTRATION с phoneNumber "89991234567"
- **THEN** заявка сохраняется с phoneNumber "+79991234567", и ответ содержит "+79991234567"

#### Scenario: Изменение с восьмеркой
- **WHEN** ACTIVE-продавец подает заявку UPDATE с phoneNumber "89990001122"
- **THEN** заявка сохраняется с phoneNumber "+79990001122"

#### Scenario: Форма без плюса
- **WHEN** phoneNumber равен "79991234567"
- **THEN** seller-service отвечает 400 и заявку не создает

#### Scenario: Номер с разделителями
- **WHEN** phoneNumber равен "+7 999 123-45-67"
- **THEN** seller-service отвечает 400

### Requirement: Self-moderation prohibition
seller-service SHALL отклонять с кодом 403 и телом `{message, time}`
(message `Moderator can't review own application.`) approve и reject заявки,
у которой keycloakUserId совпадает с claim `sub` модератора. Заявка, Seller,
роль в Keycloak и outbox SHALL NOT меняться. Проверка SHALL выполняться после
поиска заявки (иначе 404) и до проверки статуса PENDING (иначе 409). Просмотр
своей заявки модератором SHALL NOT ограничиваться.

#### Scenario: Одобрение своей заявки
- **WHEN** модератор с `sub` "m-1" одобряет PENDING-заявку пользователя "m-1"
- **THEN** seller-service отвечает 403 с message `Moderator can't review own application.`
- **AND** заявка остается PENDING, Seller не создается, gRPC-вызов не выполняется, событие не записывается

#### Scenario: Отклонение своей заявки
- **WHEN** модератор с `sub` "m-1" отклоняет заявку пользователя "m-1" с причиной
- **THEN** seller-service отвечает 403, заявка не меняется и событие не записывается

#### Scenario: Своя уже рассмотренная заявка
- **WHEN** модератор одобряет свою заявку в статусе REJECTED
- **THEN** seller-service отвечает 403

#### Scenario: Просмотр своей заявки
- **WHEN** модератор запрашивает `GET /moderator/seller-applications/{id}` своей заявки
- **THEN** seller-service отвечает 200 с заявкой

#### Scenario: Чужая заявка
- **WHEN** модератор с `sub` "m-1" одобряет PENDING-заявку пользователя "u-2"
- **THEN** одобрение выполняется как обычно

## State Model (informative)

```
Заявка (SellerApplication.status)

  [подача REGISTRATION | UPDATE | DELETION]
                 |
                 v
             PENDING --approve--> APPROVED   (конечный)
                 |
                 +-----reject---> REJECTED   (конечный)

  CANCELED - значение enum, переходов нет.

Продавец (Seller.status)

  approve REGISTRATION --> ACTIVE --approve DELETION--> DELETED (конечный)
                            ^   |
                            +---+ approve UPDATE (статус не меняется)

  BLOCKED - значение enum, переходов нет.

Роль SELLER (Keycloak realm role)
  назначается: approve REGISTRATION (gRPC AssignSellerRole)
  снимается:   approve DELETION     (gRPC RemoveSellerRole)
```

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. При UPDATE телефон не переносится.** _Устранено change
  `seller-application-contact-data`: параметр формы переименован в
  `phoneNumber`, телефон нормализуется и переносится в заявку._ Было: в
  `SellerUpdateRequest` поле называлось `phone`, а явного `@Mapping` не было,
  поэтому через заявку UPDATE телефон изменить было нельзя.
- **OBS-02. SELLER_APPROVED отправляется для UPDATE и DELETION.** _Устранено
  change `seller-approval-notification-types`: payload SELLER_APPROVED содержит
  applicationType, и notification-service отправляет отдельные письма об
  одобрении изменения и удаления; неизвестный тип уходит в DLT без письма._
  Было: продавец получал письмо "Your shop can now be used as a seller shop", в
  том числе при одобрении удаления (`ModeratorServiceImpl.approve`).
- **OBS-03. UPDATE и DELETION не создают SELLER_APPLICATION_CREATED.** _Устранено
  change `notify-moderator-on-update-deletion-applications`: подача заявки
  любого типа записывает SELLER_APPLICATION_CREATED._ Было: модератор не
  получал уведомления о заявках UPDATE и DELETION.
- **OBS-04. Удаление необратимо.** После одобрения DELETION запись Seller
  остается, поэтому повторная регистрация того же пользователя и повторное
  использование того же ИНН невозможны.
- **OBS-05. gRPC-вызов внутри транзакции.** Если вызов прошел успешно, а commit
  нет (outbox и заявка пишутся при flush), роль SELLER в Keycloak расходится с
  данными seller-service. Компенсации нет.
- **OBS-06. Поле status в reject.** _Устранено change `seller-moderation-rules`:
  поле удалено из тела reject, статус заявки выставляет сервер (REJECTED)._
  Было: поле было обязательно, но игнорировалось.
- **OBS-07. Необязательный email.** _Устранено change
  `seller-application-contact-data`: регистрация без email дает 400._ Было:
  email при регистрации был необязателен при `Seller.email nullable=false`, и
  одобрение такой заявки завершалось ошибкой БД (5xx). Заявки, поданные без
  email до change, при одобрении по-прежнему дают 500.
- **OBS-08. Уникальность sellerName.** _Устранено change
  `seller-service-error-handling`: имя уникально без учета регистра и пробелов
  по краям, проверяется при подаче и при одобрении (409), гарантируется
  индексом `uq_sellers_seller_name`._ Было: проверялось только ограничением БД
  при одобрении, поэтому ответ 5xx вместо 409.
- **OBS-09. Тексты ошибок.** _Устранено change `seller-service-error-handling`:
  тексты `MessageKeys` исправлены._ Было: у `SELLER_ALREADY_EXIST` текст
  "User not found…", `APPLICATION_ALREADY_EXISTS` формировался без аргумента и
  содержал литерал `[%s]`.
- **OBS-10. Самомодерация.** _Устранено change `seller-moderation-rules`:
  approve и reject своей заявки дают 403._ Было: модератор мог одобрить
  собственную заявку.
- **OBS-11. Пустой комментарий.** _Устранено change `seller-moderation-rules`:
  причина отказа обязательна, иначе 400._ Было: moderatorComment при reject
  был необязателен, и письмо содержало "Reason: null".
- **OBS-12. Email = null в событии UPDATE.** _Устранено change
  `seller-application-contact-data`: без email в заявку UPDATE записывается
  email Seller._ Было: события SELLER_APPLICATION_CREATED и SELLER_APPROVED для
  такой заявки уходили с email = null.
- **OBS-13. Обработчика ошибок нет.** _Устранено change
  `seller-service-error-handling`: `GlobalExceptionHandler` возвращает
  `{message, time}` для всех эндпоинтов._ Было: `@ControllerAdvice` не было,
  ответы 404 и 409 формировались через `@ResponseStatus` со стандартным телом,
  ошибки gRPC и БД давали 500. Ошибки gRPC и БД, кроме занятого sellerName,
  по-прежнему дают 500, теперь с message `Internal server error`.
- **OBS-14. Заготовки не используются.** Значения `CANCELED` (заявка) и
  `BLOCKED` (Seller) объявлены, но не используются. Отмена заявки
  закомментирована. DTO `ModeratorRequest`, `SellerRequest`,
  `SellerVerificationRequest` не используются.
- **OBS-15. Скудный ответ `GET /sellers/me`.** `SellerResponse` содержит поля
  `type` и `moderatorComment`, которых нет у `Seller`, поэтому они всегда null
  (подтверждено в `SellerMapperImpl.toDTO`). Данные продавца (sellerName, inn,
  phoneNumber, email, paymentAccount) в ответ не входят.
- **OBS-16. Длина колонок причин.** Колонки `seller_applications.moderator_comment`
  и `request_reason` созданы `ddl-auto` как `varchar(255)` (проверено на
  стенде), а API допускает до 500 символов (`ReviewSellerApplicationRequest`,
  `SellerDeletionRequest`). Причина отказа или удаления длиной 256–500
  символов, вероятно, завершается ошибкой БД (500).

## Assumptions (unverified)
- **ASM-01. Задержка роли в токене.** Назначенная или снятая роль SELLER
  попадает в JWT только при следующем выпуске токена (access token lifespan
  300 с). До этого `/sellers/**` и `/stores/**` отвечают по старому набору ролей.
- **ASM-02. Гонки.** В БД нет ограничений, гарантирующих одну PENDING-заявку на
  пользователя или ИНН. Параллельные запросы могут обойти проверки. Несколько
  инстансов OutboxPublisher (без блокировок строк) могут опубликовать одно
  событие дважды.
- **ASM-03. At-least-once.** Если Kafka подтвердила отправку, а commit не прошел,
  событие будет опубликовано повторно. Consumer не идемпотентен, поэтому письмо
  уйдет повторно.
- **ASM-04. Письмо без адреса.** При email = null отправка письма, вероятно,
  завершается MailException, которое логируется, и уведомление теряется.
- **ASM-05. Порт gRPC.** Порт 9090 у user-service берется из значения по
  умолчанию grpc-server-spring-boot-starter. Ключ `spring.grpc.server.port` в
  `application.yml` этим стартером не читается.
- **ASM-06. Идемпотентность в Keycloak.** Повторное назначение уже назначенной
  роли и снятие неназначенной в Keycloak, вероятно, проходят без ошибки.
- **ASM-07. Создание DLT.** Топик `seller-events-topic.DLT` явно не создается.
  Предполагается автосоздание топиков брокером.
- **ASM-08. Тело ответа об ошибке.** _Неактуально после change
  `seller-service-error-handling`: тело ошибки — `{message, time}`._ Было:
  стандартное тело Spring Boot без текста сообщения исключения
  (`server.error.include-message` не задан).

## Open Questions
- **OQ-01. OBS-01–OBS-16.** Какие из них дефекты, а какие задуманы? Каждый
  подтвержденный дефект — кандидат в отдельный change. (OBS-01–OBS-03,
  OBS-06–OBS-13 устранены.)
- **OQ-02. Повторная регистрация.** Должен ли удаленный продавец (DELETED)
  иметь возможность зарегистрироваться снова?
- **OQ-03. Статусы-заготовки.** Планируются ли сценарии отмены заявки
  (CANCELED) и блокировки продавца (BLOCKED)? Кто их инициирует?
- **OQ-04. Магазины и товары.** Что должно происходить с магазинами и товарами
  продавца после удаления? Сейчас они остаются без изменений.
- **OQ-05. Адресаты писем.** Должны ли уведомления модератору идти на один
  адрес из конфигурации или всем пользователям с ролью MODERATOR? Должен ли
  продавцу использоваться email из заявки или email аккаунта Keycloak?
- **OQ-06. Защита gRPC.** Нужна ли аутентификация вызова `user.UserService`.
  Сейчас назначить роль SELLER может любой участник docker-сети.
