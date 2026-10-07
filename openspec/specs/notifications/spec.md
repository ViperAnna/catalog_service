# notifications Specification

## Purpose
Baseline (as-is) поведения notification-service: обработка события создания
профиля пользователя (приветственное письмо, ссылка для привязки Telegram и
сообщение в Telegram), привязка Telegram-чата к пользователю через бота,
отправка email и обработка ошибок при потреблении событий Kafka. Спецификация
описывает фактическое текущее поведение кода, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Учитывается
  незакоммиченное изменение `application.yaml`: `smtp.auth` и `smtp.ssl.enable`
  читаются из `MAIL_SMTP_AUTH` и `MAIL_SMTP_SSL` (по умолчанию true). В dev
  письма уходят в mailpit (`mailpit:1025`, без auth и TLS).
- Метод: статический анализ кода и конфигурации, схема таблицы
  `telegram_users` в dev-БД. Поведение при старте без Kafka наблюдалось на
  dev-стенде. Тестов, кроме `contextLoads`, нет.
- Хранение: PostgreSQL, база `notification_service`, таблица `telegram_users`.
- Источники событий: `user-created-events-topic` (`user-profile`) и
  `seller-events-topic`. Уведомления по `seller-events-topic` (письма
  модератору и продавцу) описаны в `seller-onboarding` (requirements
  `Seller event email notifications` и
  `Seller events consumer error handling`) и здесь не повторяются.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код сейчас. Изменять его
  нужно отдельным change.

## Requirements

### Requirement: No HTTP API
notification-service SHALL NOT предоставлять HTTP-эндпоинтов и SHALL NOT
выполнять аутентификацию запросов. Маршрут api-gateway `/notifications/**`
SHALL NOT вести ни к одному обработчику.

#### Scenario: Запрос через gateway
- **WHEN** клиент вызывает `GET /notifications/anything` через api-gateway
- **THEN** запрос не обрабатывается никакой бизнес-логикой

**Observation**: OBS-01
**Assumption**: ASM-01

### Requirement: User created event consumption
notification-service SHALL потреблять `user-created-events-topic` в consumer
group `notification-group` и SHALL разбирать значение сообщения как JSON
`{userId, keycloakUserId, email, username}` без конверта. Для каждого события
SHALL по порядку выполняться: подготовка ссылки привязки Telegram, сообщение в
Telegram и приветственное письмо.

#### Scenario: Новое событие
- **WHEN** в топик приходит USER_CREATED для нового userId
- **THEN** создается запись привязки Telegram и отправляется приветственное письмо

### Requirement: Telegram link token issuance
При обработке события notification-service SHALL искать запись Telegram по
userId. Для привязанной записи ссылка SHALL быть null. Для записи с
сохраненным токеном SHALL использоваться этот токен без проверки срока.
Иначе SHALL создаваться токен-UUID со сроком действия 1 сутки. Ссылка SHALL
иметь вид `https://t.me/myNotificServiceJavaAppBot?start=<token>`.

#### Scenario: Новый пользователь
- **WHEN** записи для userId нет
- **THEN** создается запись linked = false с новым токеном и сроком now + 1 сутки

#### Scenario: Повторное событие до привязки
- **WHEN** запись с токеном уже есть
- **THEN** ссылка строится с тем же токеном, даже если срок истек

#### Scenario: Уже привязан
- **WHEN** запись для userId привязана
- **THEN** ссылка равна null

**Observation**: OBS-02, OBS-03

### Requirement: Welcome email
notification-service SHALL отправлять на email из события письмо от адреса
`spring.mail.from` с темой "Welcome!" и текстом, содержащим username и ссылку
привязки Telegram. Ошибка отправки (MailException) SHALL логироваться без
повторной попытки.

#### Scenario: Приветственное письмо
- **WHEN** обрабатывается USER_CREATED с email "a@b.c" и username "jdoe"
- **THEN** на "a@b.c" уходит письмо "Welcome!" с "Welcome, jdoe!" и ссылкой на бота

#### Scenario: Привязанный пользователь
- **WHEN** ссылка равна null
- **THEN** письмо отправляется со строкой "null" на месте ссылки

**Observation**: OBS-04

### Requirement: Welcome Telegram message
Если запись Telegram для userId события привязана, notification-service SHALL
отправлять в ее chatId сообщение «Добро пожаловать! Ваш аккаунт успешно
зарегистрирован.». Ошибка Telegram API SHALL логироваться без повторной
попытки.

#### Scenario: Новый пользователь
- **WHEN** обрабатывается событие для userId без привязки
- **THEN** сообщение в Telegram не отправляется

**Observation**: OBS-05

### Requirement: Telegram account linking via bot
Бот SHALL получать обновления через long polling и обрабатывать только
текстовые сообщения, начинающиеся с `/start`. Для `/start <token>` с
действующим токеном непривязанной записи он SHALL сохранять chatId и имя
отправителя, ставить linked = true, удалять токен и срок и отвечать
«Telegram connected successfully!».

#### Scenario: Успешная привязка
- **WHEN** пользователь отправляет боту `/start <token>` с неистекшим токеном
- **THEN** запись привязана к чату и пользователь получает подтверждение

#### Scenario: Прочие сообщения
- **WHEN** пользователь отправляет боту текст без `/start`
- **THEN** бот не отвечает

**Observation**: OBS-06, OBS-07

### Requirement: Telegram linking error replies
На `/start` без ровно одного аргумента бот SHALL отвечать
"Usage: /start <token>", на неизвестный токен — "Invalid token.", для уже
привязанной записи — "Telegram is already connected.", при отсутствии срока —
"Token is invalid.", при истекшем сроке — "Link has expired.". В этих случаях
запись SHALL NOT изменяться.

#### Scenario: Истекший токен
- **WHEN** пользователь отправляет `/start <token>` после истечения суток
- **THEN** бот отвечает "Link has expired." и запись не меняется

#### Scenario: Без токена
- **WHEN** пользователь отправляет `/start`
- **THEN** бот отвечает "Usage: /start <token>"

**Observation**: OBS-02

### Requirement: Event consumer error handling
Исключение при обработке сообщения из `user-created-events-topic` или
`seller-events-topic` SHALL повторяться 3 раза с интервалом 1 секунда, после
чего сообщение SHALL публиковаться в `<topic>.DLT`. Ошибки отправки email и
Telegram SHALL NOT считаться ошибками обработки. Потребителей DLT SHALL NOT
существовать.

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения в `user-created-events-topic` не является JSON события
- **THEN** после 3 повторов сообщение публикуется в `user-created-events-topic.DLT`

#### Scenario: SMTP недоступен
- **WHEN** отправка приветственного письма завершилась ошибкой
- **THEN** ошибка логируется, сообщение считается обработанным

**Assumption**: ASM-02

### Requirement: Email transport configuration
notification-service SHALL отправлять письма через SMTP-сервер
`MAIL_HOST:MAIL_PORT` с учетными данными `MAIL_USERNAME`/`MAIL_PASSWORD`, с
SMTP auth и SSL, включаемыми `MAIL_SMTP_AUTH` и `MAIL_SMTP_SSL` (по умолчанию
true), и с адресом отправителя `MAIL_FROM`.

#### Scenario: Dev-окружение
- **WHEN** сервис запущен с `.env.notification` dev-окружения
- **THEN** письма уходят в mailpit без auth и TLS

## Observations (non-normative)
Поведение подтверждено кодом или наблюдением на стенде и может быть дефектом. В
Requirements оно зафиксировано как есть. Исправлять его нужно отдельным change
после решения.

- **OBS-01. Мертвый маршрут.** api-gateway содержит маршрут `/notifications/**`,
  но у сервиса нет контроллеров и Spring Security.
- **OBS-02. Истекший токен переиспользуется.** Сохраненный токен
  переиспользуется без проверки срока. После истечения суток повторное
  USER_CREATED выдаст ту же просроченную ссылку, и бот ответит
  "Link has expired." без способа получить новую.
- **OBS-03. Имя бота захардкожено.** Имя бота зашито в ссылку
  (`myNotificServiceJavaAppBot`). Свойство `telegram.bot.username`
  (`TELEGRAM_BOT_USERNAME`) не используется.
- **OBS-04. "null" в письме.** Для привязанного пользователя в приветственном
  письме вместо ссылки печатается "null".
- **OBS-05. Telegram-приветствие почти не отправляется.** Привязка создается
  только после первого письма, а повторное создание профиля дает новый userId
  (`user-profile` OBS-05). Поэтому сообщение в Telegram отправляется только при
  повторной доставке события для уже привязанного userId.
- **OBS-06. Один чат на несколько пользователей.** Один Telegram-чат можно
  привязать к нескольким userId: уникальности chatId нет. В telegramUsername
  сохраняется имя (first name), а не username Telegram.
- **OBS-07. Логи с персональными данными.** Бот и консьюмер пишут в лог токены
  привязки, тексты сообщений, chatId и полный payload события с email.
- **OBS-08. Старт без Kafka (наблюдалось на dev-стенде).** Если при старте
  Kafka недоступна, Spring-контекст не поднимается ("No resolvable bootstrap
  urls"), консьюмеры и HTTP-порт не работают, сервис снимается с регистрации в
  Eureka, но процесс JVM остается жив. Контейнер в статусе Up не
  перезапускается политикой `restart: unless-stopped`.
- **OBS-09. Ссылка на Eureka через переопределение.** В `application.yaml`
  указан `eureka.client.service-url.defaultZone: ${DEFAULTZONE}`, но такой
  переменной в env нет. Регистрация работает за счет
  `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` из `.env.kafka`.
- **OBS-10. Идемпотентности нет.** Повторная доставка USER_CREATED приводит к
  повторному приветственному письму.

## Assumptions (unverified)
- **ASM-01. Ответ gateway на /notifications/.** Когда notification-service
  зарегистрирован в Eureka, запрос через gateway, вероятно, получает 404 от
  сервиса. Когда не зарегистрирован, gateway, вероятно, отвечает 503.
- **ASM-02. Ошибки БД при обработке.** Ошибки БД при создании привязки
  Telegram считаются ошибками обработки и ведут к повторам и DLT. Повторная
  обработка после частичного выполнения может повторно отправить письмо.
- **ASM-03. Невалидный токен бота.** При невалидном `TELEGRAM_BOT_TOKEN` бот,
  вероятно, не получает обновлений, а консьюмеры Kafka продолжают работать.
- **ASM-04. Причина живого процесса.** JVM остается жива после неудачного
  старта (OBS-08), вероятно, из-за не-daemon потоков Telegram long polling или
  OkHttp.

## Open Questions
- **OQ-01. OBS-01–OBS-10.** Какие из них дефекты?
- **OQ-02. Перевыпуск ссылки.** Как пользователь должен получать новую ссылку
  привязки Telegram после истечения срока?
- **OQ-03. Seller-уведомления в Telegram.** Нужно ли отправлять уведомления
  продавцам и модераторам в Telegram? Сейчас для seller-событий используется
  только email.
- **OQ-04. Маршрут gateway.** Нужен ли маршрут `/notifications/**` или он
  оставлен под будущий API?
