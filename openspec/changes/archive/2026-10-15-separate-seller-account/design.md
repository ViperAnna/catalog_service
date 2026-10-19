# Design

## Context

- Роли: USER входит в составную роль `default-roles-user-service-realm`
  (вместе с `offline_access`, `uma_authorization` и клиентскими ролями
  `account`), Keycloak выдает ее каждому новому пользователю. SELLER сейчас
  добавляется аккаунту заявителя (`AssignSellerRole`), снимается при удалении.
- Владение в seller-service — по `sub`: `Seller.keycloakUserId`, заявки,
  магазины. catalog-service проверяет владение через seller-service.
- Realm: `duplicateEmailsAllowed: false`, `loginWithEmailAllowed: true`,
  `resetPasswordAllowed: true`, SMTP — `smtp.mail.ru`. Клиент
  `user-service-admin` имеет `manage-users`, `view-users`, `query-users`.
- Зависимости от USER у аккаунта продавца: `/users/me` (профиль) и
  `/seller-applications/**` — первая закрывается намеренно, вторая меняется
  (D4). Корзина, заказы, вишлисты требуют USER — закрываются автоматически.
- frontend: разделов продавца нет; профиль и вишлисты показываются всем
  вошедшим.

## Goals / Non-Goals

**Goals:** аккаунт продавца только с SELLER; покупатель остается покупателем;
надежное одобрение при внешнем вызове Keycloak; удаление аккаунта продавца.

**Non-Goals:** кабинет продавца во frontend, перенос существующих продавцов,
смена email входа через UPDATE, изменения конфигурации realm.

## Decisions

- **D1. Модель Seller.** Новая колонка `applicant_keycloak_user_id`
  (nullable — у существующих продавцов не задана). «У пользователя есть
  Seller» = Seller с `keycloak_user_id = sub` или `applicant_keycloak_user_id =
  sub`. Уникальный частичный индекс по `applicant_keycloak_user_id` в
  `schema.sql` seller-service — один продавец на покупателя и защита от гонки.
- **D2. Аккаунт продавца в Keycloak.** `UserRepresentation`: username и email
  = E в нижнем регистре, `enabled = true`, `emailVerified = false`, атрибут
  `account_type = seller`. После создания: удалить составную роль
  `default-roles-user-service-realm`, добавить SELLER,
  `executeActionsEmail([UPDATE_PASSWORD, VERIFY_EMAIL])`. Если ссылка из
  письма истекла — «Забыли пароль» на странице входа (realm это разрешает).
  Без `offline_access` и ролей `account` — продавцу они не нужны.
- **D3. Идемпотентность создания.** Если пользователь с E уже есть и у него
  `account_type = seller` и нет входов (нет учетных данных пароля) —
  возвращается его id (остаток после сбоя). Если E занят другим аккаунтом —
  ALREADY_EXISTS → 409.
- **D4. Доступ к заявкам.** В `SpringSecurityConfig` seller-service по
  методам и путям: `POST /seller-applications/registration` — USER;
  `GET /me`, `GET /history` — USER или SELLER; `PUT /update`,
  `DELETE /deletion`, `POST /unblock` — SELLER. Существующие продавцы имеют
  обе роли — для них ничего не меняется.
- **D5. Одобрение и компенсация.** Порядок в транзакции: проверки →
  сохранение Seller (flush, `keycloakUserId` временно = заявитель) →
  `CreateSellerAccount` → `keycloakUserId` = id аккаунта → заявка APPROVED,
  outbox. После успешного gRPC регистрируется
  `TransactionSynchronization.afterCompletion`: при откате —
  `DeleteSellerAccount` (ошибка компенсации — ERROR в логе; повтор одобрения
  переиспользует аккаунт по D3).
- **D6. Проверка email.** `IsEmailAvailable` при подаче REGISTRATION (409) и
  `CreateSellerAccount` при одобрении (ALREADY_EXISTS → 409). Email заявителя
  занят его аккаунтом — поэтому совпадение с собственным email тоже 409.
- **D7. Удаление.** Отдельный аккаунт (`applicant ≠ null` и `keycloakUserId ≠
  applicant`) → `DeleteSellerAccount`; иначе (существующий продавец) →
  `RemoveSellerRole`. Удаляются заявки по `keycloakUserId` продавца и
  REGISTRATION-заявки по `applicant`. `DeleteSellerAccount` удаляет только
  пользователя с `account_type = seller` — защита аккаунта покупателя.
- **D8. frontend.** `useAuthStore` знает роли; ссылки и маршруты «Профиль» и
  «Списки желаний» показываются только при роли USER, для аккаунта без USER —
  скрыты (маршрут → сообщение «недоступно для аккаунта продавца»).

## Risks / Trade-offs

- [R1] Сбой компенсации → «осиротевший» аккаунт продавца в Keycloak без
  Seller. Повтор одобрения его переиспользует (D3); иначе удаление вручную
  через Admin Console.
- [R2] Аккаунт создан, письмо не отправлено (SMTP) → создание считается
  ошибкой, откат и компенсация; продавец может также использовать «Забыли
  пароль».
- [R3] Вход через Google для аккаунта продавца — только если email Google = E
  (связывание аккаунтов при первом входе).
- [R4] В dev SMTP realm — `smtp.mail.ru`: тестовые одобрения отправят реальные
  письма на адреса из заявок. Для стенда использовать адреса, к которым есть
  доступ, или переключить SMTP realm на mailpit (изменение Keycloak — по
  решению пользователя, вне этого change).
- [R5] Токен покупателя-заявителя не меняется — роль SELLER он не получает;
  продавец входит под E отдельно.

## Migration Plan

1. seller-service: колонка `applicant_keycloak_user_id` (ddl-auto) и индекс
   (`schema.sql`); существующие Seller — `NULL`.
2. Деплой user-service (новые gRPC-методы) до seller-service.
3. Деплой seller-service и frontend.
4. Откат: прежние образы; аккаунты продавцов, созданные новым кодом, остаются
   в Keycloak, Seller ссылаются на них — прежний код с ними работает (владение
   по `sub`), но удаление снимет роль вместо удаления аккаунта.

## Open Questions

- Смена контактного email через заявку UPDATE: сейчас меняет только
  `Seller.email`, email входа аккаунта продавца остается прежним. Синхронизация
  email входа — отдельное решение пользователя (не меняет этот дизайн).
