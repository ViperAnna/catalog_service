## MODIFIED Requirements

### Requirement: Role-based access to onboarding API
seller-service SHALL разрешать `POST /seller-applications/registration` только
с JWT с realm-ролью USER; `GET /seller-applications/me` и
`GET /seller-applications/history` — с ролью USER или SELLER;
`PUT /seller-applications/update`, `DELETE /seller-applications/deletion` и
`POST /seller-applications/unblock` — с ролью SELLER;
`/moderator/seller-applications/**` — с ролью MODERATOR. Роли SHALL
извлекаться из claim `realm_access.roles` и сопоставляться без учета
регистра. Идентификатором пользователя SHALL служить claim `sub`.

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

#### Scenario: Аккаунт продавца подает заявку на изменение
- **WHEN** аккаунт только с ролью SELLER вызывает `PUT /seller-applications/update`
- **THEN** заявка принимается по правилам `Seller update application submission`

#### Scenario: Аккаунт продавца не подает регистрацию
- **WHEN** аккаунт только с ролью SELLER вызывает `POST /seller-applications/registration`
- **THEN** seller-service отвечает 403

#### Scenario: Покупатель не подает заявку на изменение
- **WHEN** аккаунт только с ролью USER вызывает `PUT /seller-applications/update`
- **THEN** seller-service отвечает 403

**Observation**: OBS-10

### Requirement: Registration application constraints
seller-service SHALL отклонять заявку на регистрацию с кодом 409, если у
пользователя уже есть Seller (Seller, созданный по его заявке, или Seller на
его собственном аккаунте), если email заявки уже принадлежит аккаунту
Keycloak, если inn принадлежит существующему Seller или указан в любой заявке в
статусе PENDING, а также если sellerName после удаления пробелов по краям и без
учета регистра совпадает с именем существующего Seller или с sellerName любой
заявки в статусе PENDING. Данные окончательно удаленного продавца SHALL NOT
препятствовать новой заявке.

#### Scenario: Пользователь уже продавец
- **WHEN** у пользователя есть Seller со статусом ACTIVE или BLOCKED — созданный по его заявке или на его аккаунте
- **THEN** заявка REGISTRATION отклоняется с 409

#### Scenario: Бывший продавец
- **WHEN** продавец пользователя окончательно удален, и пользователь подает REGISTRATION
- **THEN** заявка принимается как новая, создается новая заявка REGISTRATION/PENDING

#### Scenario: ИНН на рассмотрении у другого пользователя
- **WHEN** inn указан в PENDING-заявке другого пользователя
- **THEN** заявка REGISTRATION отклоняется с 409

#### Scenario: ИНН удаленного продавца
- **WHEN** inn принадлежал окончательно удаленному продавцу
- **THEN** ограничение ИНН не препятствует подаче

#### Scenario: Имя занято продавцом
- **WHEN** существует Seller с sellerName "Shop" и пользователь подает заявку с sellerName "  SHOP "
- **THEN** заявка REGISTRATION отклоняется с 409 и message `Seller with name [SHOP] already exists.`

#### Scenario: Имя на рассмотрении
- **WHEN** sellerName "shop" указан в PENDING-заявке другого пользователя и пользователь подает заявку с sellerName "Shop"
- **THEN** заявка REGISTRATION отклоняется с 409 и message `Seller name [Shop] is already in review.`

#### Scenario: Имя из рассмотренной заявки
- **WHEN** sellerName "Shop" указан только в REJECTED-заявке, а Seller с таким именем нет
- **THEN** ограничение имени не препятствует подаче

#### Scenario: Email занят аккаунтом
- **WHEN** email заявки совпадает с email любого аккаунта Keycloak, в том числе аккаунта самого пользователя
- **THEN** заявка REGISTRATION отклоняется с 409 и message `Email [<email>] is already used by another account.`

### Requirement: Registration application approval
При одобрении заявки REGISTRATION seller-service SHALL проверить, что у
заявителя нет Seller (иначе 409), что sellerName не занят по правилам
`Seller name uniqueness` (иначе 409), и SHALL создать Seller(ACTIVE) с данными
заявки и `applicantKeycloakUserId` = keycloakUserId заявки, записав его в БД до
gRPC-вызова. Затем SHALL синхронно запросить у user-service создание аккаунта
продавца с email заявки и SHALL записать id созданного аккаунта в
`Seller.keycloakUserId`. Аккаунт заявителя SHALL NOT меняться.

#### Scenario: Успешное одобрение регистрации
- **WHEN** модератор одобряет PENDING-заявку REGISTRATION покупателя B с email E, и user-service создает аккаунт S
- **THEN** создается Seller(ACTIVE) с keycloakUserId S и applicantKeycloakUserId B
- **AND** аккаунт S имеет логин и email E и только realm-роль SELLER; аккаунт B не меняется
- **AND** заявка APPROVED, в outbox записано SELLER_APPROVED

#### Scenario: Seller уже существует
- **WHEN** на момент одобрения у заявителя уже есть Seller
- **THEN** seller-service отвечает 409, gRPC-вызов не выполняется

#### Scenario: Имя занято
- **WHEN** на момент одобрения sellerName заявки совпадает (без учета регистра и пробелов по краям) с именем существующего Seller
- **THEN** seller-service отвечает 409 с message `Seller with name [<sellerName>] already exists.`
- **AND** Seller не создается, заявка остается PENDING, gRPC-вызов не выполняется

#### Scenario: Нарушено ограничение БД на Seller
- **WHEN** inn уже занят другим Seller, либо email заявки равен null
- **THEN** сохранение Seller завершается ошибкой БД до gRPC-вызова
- **AND** транзакция откатывается, заявка остается PENDING, ответ 500 с message `Internal server error`

#### Scenario: Email занят к моменту одобрения
- **WHEN** после подачи заявки email E занял другой аккаунт Keycloak
- **THEN** seller-service отвечает 409, Seller не создается, заявка остается PENDING

**Observation**: OBS-05, OBS-07

### Requirement: Deletion application approval
При одобрении заявки DELETION seller-service SHALL найти Seller по
keycloakUserId заявки (иначе 404) и в одной транзакции SHALL записать
STORE_PURGED на каждый магазин продавца и SELLER_APPROVED, физически удалить
магазины, запись Seller, все заявки аккаунта продавца и регистрационные заявки
его заявителя. Затем SHALL запросить у user-service удаление аккаунта
продавца, если он отдельный (keycloakUserId ≠ applicantKeycloakUserId), или
снятие роли SELLER, если продавец существующий и работает на аккаунте
покупателя. Ошибка SHALL откатывать все. Статус Seller (ACTIVE или BLOCKED)
SHALL NOT влиять на одобрение.

#### Scenario: Успешное одобрение удаления
- **WHEN** модератор одобряет PENDING-заявку DELETION продавца с отдельным аккаунтом S, магазинами 3 и 4, и user-service удаляет S
- **THEN** записи Seller, магазинов 3 и 4, заявки аккаунта S и регистрационная заявка заявителя удалены из БД
- **AND** в outbox записаны STORE_PURGED для магазинов 3 и 4 и SELLER_APPROVED с applicationType DELETION
- **AND** аккаунт S удален из Keycloak, аккаунт заявителя не меняется, ответ 204

#### Scenario: Удаление заблокированного продавца
- **WHEN** модератор одобряет DELETION продавца в статусе BLOCKED
- **THEN** выполняется то же окончательное удаление

#### Scenario: Персональные данные в outbox
- **WHEN** одобряется DELETION продавца, у заявок которого есть опубликованные записи outbox
- **THEN** в той же транзакции их payload обезличивается, а SELLER_APPROVED этой заявки сохраняет payload до публикации

#### Scenario: Ошибка снятия роли
- **WHEN** gRPC-вызов удаления аккаунта или снятия роли завершается ошибкой
- **THEN** Seller, магазины и заявки остаются в БД, событий нет, заявка остается PENDING

#### Scenario: Существующий продавец на аккаунте покупателя
- **WHEN** одобряется DELETION продавца, у которого keycloakUserId равен applicantKeycloakUserId или applicantKeycloakUserId не задан
- **THEN** у аккаунта снимается роль SELLER, аккаунт не удаляется

### Requirement: Seller role sync gRPC contract
seller-service SHALL вызывать сервис `user.UserService` методами
`CreateSellerAccount` (`{email, sellerName}` → `{keycloakUserId}`),
`DeleteSellerAccount` (`{keycloakUserId}`), `IsEmailAvailable` (`{email}` →
`{available}`), а также `AssignSellerRole` и `RemoveSellerRole`
(`{keycloakUserId}`) — только для существующих продавцов на аккаунте
покупателя. Вызовы SHALL выполняться через blocking stub по адресу
`user-service:9090` без TLS и без аутентификации вызова, с дедлайном 5 секунд.
Адрес SHALL NOT определяться через service discovery.

#### Scenario: Назначение роли при одобрении регистрации
- **WHEN** одобряется заявка REGISTRATION
- **THEN** seller-service вызывает CreateSellerAccount с email и sellerName заявки, AssignSellerRole не вызывается

#### Scenario: Превышен дедлайн
- **WHEN** user-service не ответил за 5 секунд
- **THEN** вызов завершается ошибкой DEADLINE_EXCEEDED

**Assumption**: ASM-05

### Requirement: Seller role change in Keycloak
user-service SHALL обрабатывать вызовы через Keycloak Admin API в realm из
`application.realm` от имени клиента с grant client_credentials:
`CreateSellerAccount` — создать включенного пользователя с логином и email из
запроса (без учета регистра), без составной роли
`default-roles-user-service-realm`, только с realm-ролью SELLER, с атрибутом
аккаунта продавца, и отправить письмо с действиями UPDATE_PASSWORD и
VERIFY_EMAIL; `DeleteSellerAccount` — удалить пользователя, если у него есть
атрибут аккаунта продавца (иначе FAILED_PRECONDITION); `IsEmailAvailable` —
проверить отсутствие пользователя с таким email; `AssignSellerRole` и
`RemoveSellerRole` — как прежде. При ошибке Keycloak SHALL отвечать INTERNAL.

#### Scenario: Успешное назначение
- **WHEN** user-service получает CreateSellerAccount для свободного email E
- **THEN** в Keycloak создан пользователь E с единственной realm-ролью SELLER, отправлено письмо установки пароля, возвращен его id

#### Scenario: Ошибка Keycloak
- **WHEN** Keycloak недоступен
- **THEN** user-service отвечает INTERNAL

#### Scenario: Повтор после незавершенного одобрения
- **WHEN** CreateSellerAccount вызван для E, и в Keycloak уже есть пользователь E с атрибутом аккаунта продавца, который ни разу не входил
- **THEN** возвращается id существующего пользователя, второй не создается

#### Scenario: Email занят обычным аккаунтом
- **WHEN** CreateSellerAccount вызван для E, принадлежащего аккаунту без атрибута продавца
- **THEN** user-service отвечает ALREADY_EXISTS

#### Scenario: Защита аккаунта покупателя
- **WHEN** DeleteSellerAccount вызван для аккаунта без атрибута продавца
- **THEN** user-service отвечает FAILED_PRECONDITION, аккаунт не удаляется

**Assumption**: ASM-01, ASM-06

### Requirement: Approval rollback on role sync failure
Если gRPC-вызов при одобрении завершился ошибкой, seller-service SHALL
откатить транзакцию одобрения (изменения Seller, заявки и outbox) и SHALL
ответить кодом 5xx (ALREADY_EXISTS при создании аккаунта — 409). Заявка SHALL
остаться в статусе PENDING. Если аккаунт продавца создан, а транзакция
одобрения не зафиксирована, seller-service SHALL запросить его удаление
(компенсация); неудачная компенсация SHALL записываться в лог как ошибка и
устраняется повтором одобрения (`Seller role change in Keycloak`, повтор).

#### Scenario: user-service недоступен при одобрении регистрации
- **WHEN** CreateSellerAccount завершился ошибкой
- **THEN** Seller не создан, заявка остается PENDING, событие не записано, ответ 500

#### Scenario: Ошибка после успешного назначения роли
- **WHEN** аккаунт продавца создан, но фиксация транзакции завершилась ошибкой
- **THEN** изменения в БД откатываются, и seller-service запрашивает удаление созданного аккаунта

**Observation**: OBS-05
