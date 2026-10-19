## ADDED Requirements

### Requirement: Seller blocking by moderator
seller-service SHALL принимать `POST /moderator/sellers/{sellerId}/block` и
`POST /moderator/sellers/{sellerId}/unblock` с ролью MODERATOR: block
переводит Seller из ACTIVE в BLOCKED, unblock — из BLOCKED в ACTIVE, ответ
204; нет Seller — 404; другой исходный статус — 409. Блокировка SHALL NOT
менять статусы магазинов и товаров, роль SELLER и SHALL NOT удалять данные.

#### Scenario: Блокировка продавца
- **WHEN** модератор блокирует ACTIVE-продавца с магазинами 3 и 4
- **THEN** Seller получает статус BLOCKED, ответ 204
- **AND** статусы магазинов и товаров не меняются, данные не удаляются, роль SELLER сохраняется
- **AND** записываются SELLER_BLOCKED для магазинов 3 и 4, STORE_PURGED не записывается

#### Scenario: Разблокировка продавца
- **WHEN** модератор разблокирует BLOCKED-продавца, у которого магазин 3 заблокирован отдельно
- **THEN** Seller получает статус ACTIVE, записываются SELLER_UNBLOCKED, магазин 3 остается BLOCKED

#### Scenario: Недопустимый переход
- **WHEN** модератор блокирует продавца в статусе BLOCKED
- **THEN** seller-service отвечает 409, событий нет

#### Scenario: Продавец не найден
- **WHEN** sellerId не существует
- **THEN** seller-service отвечает 404

### Requirement: Blocked seller restrictions
Продавец в статусе BLOCKED SHALL иметь доступ только к просмотру своих
данных, магазинов, товаров и заявок и к подаче заявок DELETION и UNBLOCK.
Создание, изменение и удаление магазинов, создание, изменение и удаление
товаров и подача заявки UPDATE SHALL отклоняться с 409. Проверки SHALL
выполняться на сервере, роль SELLER SHALL NOT сниматься.

#### Scenario: Создание магазина
- **WHEN** заблокированный продавец вызывает `POST /stores`
- **THEN** seller-service отвечает 409, магазин не создается

#### Scenario: Просмотр своих данных
- **WHEN** заблокированный продавец вызывает `GET /stores` и `GET /sellers/me`
- **THEN** ответ 200 с его данными

#### Scenario: Заявка на изменение
- **WHEN** заблокированный продавец подает заявку UPDATE
- **THEN** seller-service отвечает 409

### Requirement: Seller unblock application
seller-service SHALL принимать `POST /seller-applications/unblock` с телом
`{reason}` (обязательно, не длиннее 500) от продавца в статусе BLOCKED без
PENDING-заявки и SHALL создавать заявку UNBLOCK/PENDING со снимком данных
Seller и событием SELLER_APPLICATION_CREATED. Иначе SHALL возвращаться 409.
Одобрение SHALL переводить Seller в ACTIVE с SELLER_UNBLOCKED для каждого
магазина; если Seller уже не BLOCKED — 409.

#### Scenario: Подача заявки на разблокировку
- **WHEN** заблокированный продавец без PENDING-заявок подает UNBLOCK с причиной
- **THEN** создается заявка UNBLOCK/PENDING и записывается SELLER_APPLICATION_CREATED

#### Scenario: Заявка от активного продавца
- **WHEN** ACTIVE-продавец подает UNBLOCK
- **THEN** seller-service отвечает 409

#### Scenario: Одобрение разблокировки
- **WHEN** модератор одобряет PENDING-заявку UNBLOCK заблокированного продавца
- **THEN** Seller получает ACTIVE, записываются SELLER_UNBLOCKED для его магазинов и SELLER_APPROVED с applicationType UNBLOCK
- **AND** отдельно заблокированные магазины остаются BLOCKED

#### Scenario: Продавец уже разблокирован
- **WHEN** модератор одобряет UNBLOCK, а продавец уже разблокирован модератором
- **THEN** seller-service отвечает 409

## MODIFIED Requirements

### Requirement: Deletion application approval
При одобрении заявки DELETION seller-service SHALL найти Seller по
keycloakUserId заявки (иначе 404) и в одной транзакции SHALL записать
STORE_PURGED на каждый магазин продавца и SELLER_APPROVED, физически удалить
магазины, запись Seller и все заявки пользователя и запросить у
user-service снятие роли SELLER. Ошибка SHALL откатывать все. Статус Seller
(ACTIVE или BLOCKED) SHALL NOT влиять на одобрение.

#### Scenario: Успешное одобрение удаления
- **WHEN** модератор одобряет PENDING-заявку DELETION продавца с магазинами 3 и 4, и user-service успешно снимает роль
- **THEN** записи Seller, магазинов 3 и 4 и все заявки пользователя удалены из БД
- **AND** в outbox записаны STORE_PURGED для магазинов 3 и 4 и SELLER_APPROVED с applicationType DELETION
- **AND** у пользователя в Keycloak снята realm-роль SELLER, ответ 204

#### Scenario: Удаление заблокированного продавца
- **WHEN** модератор одобряет DELETION продавца в статусе BLOCKED
- **THEN** выполняется то же окончательное удаление

#### Scenario: Персональные данные в outbox
- **WHEN** одобряется DELETION продавца, у заявок которого есть опубликованные записи outbox
- **THEN** в той же транзакции их payload обезличивается, а SELLER_APPROVED этой заявки сохраняет payload до публикации

#### Scenario: Ошибка снятия роли
- **WHEN** gRPC-вызов снятия роли завершается ошибкой
- **THEN** Seller, магазины и заявки остаются в БД, событий нет, заявка остается PENDING

### Requirement: Seller deletion application submission
seller-service SHALL принимать `DELETE /seller-applications/deletion` с телом
JSON `{reason}` (обязательно, не длиннее 500 символов) и SHALL создавать заявку
DELETION/PENDING со снимком sellerName, inn, phoneNumber, email, paymentAccount
текущего Seller и requestReason = reason, только если Seller существует в
статусе ACTIVE или BLOCKED. Ответ SHALL иметь код 200. В той же транзакции
SHALL записываться событие SELLER_APPLICATION_CREATED.

#### Scenario: Успешная подача удаления
- **WHEN** ACTIVE- или BLOCKED-продавец без PENDING-заявок отправляет непустую причину
- **THEN** создается заявка DELETION/PENDING с данными Seller и requestReason
- **AND** в outbox в той же транзакции записывается событие SELLER_APPLICATION_CREATED с applicationId, keycloakUserId, sellerName и email созданной заявки

#### Scenario: Отклоненная подача удаления не создает событие
- **WHEN** подача DELETION отклоняется с 400, 404 или 409
- **THEN** заявка не создается и событие в outbox не записывается

#### Scenario: Причина не указана
- **WHEN** reason пуст
- **THEN** seller-service отвечает 400

### Requirement: Registration application constraints
seller-service SHALL отклонять заявку на регистрацию с кодом 409, если для
пользователя существует Seller, если inn принадлежит существующему Seller или
указан в любой заявке в статусе PENDING, а также если sellerName после
удаления пробелов по краям и без учета регистра совпадает с именем
существующего Seller или с sellerName любой заявки в статусе PENDING. Данные
окончательно удаленного продавца SHALL NOT препятствовать новой заявке.

#### Scenario: Пользователь уже продавец
- **WHEN** для keycloakUserId пользователя существует Seller со статусом ACTIVE или BLOCKED
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

### Requirement: Application approval general rules
`POST /moderator/seller-applications/{id}/approve` SHALL выполнять действия,
соответствующие типу заявки, переводить заявку в статус APPROVED (кроме
DELETION, где заявки удаляются) и записывать в outbox событие SELLER_APPROVED
для заявок любого типа (REGISTRATION, UPDATE, DELETION, UNBLOCK). Все
изменения SHALL выполняться в одной транзакции seller-service. Успешный ответ
SHALL иметь код 204 без тела.

#### Scenario: Одобрение заявки на изменение
- **WHEN** модератор одобряет PENDING-заявку UPDATE
- **THEN** заявка становится APPROVED
- **AND** в outbox записывается SELLER_APPROVED

#### Scenario: Одобрение заявки на удаление
- **WHEN** модератор одобряет PENDING-заявку DELETION
- **THEN** в outbox записывается SELLER_APPROVED, а заявки пользователя удаляются

#### Scenario: Одобрение заявки на разблокировку
- **WHEN** модератор одобряет PENDING-заявку UNBLOCK
- **THEN** заявка становится APPROVED и в outbox записывается SELLER_APPROVED

### Requirement: Outbox event recording
seller-service SHALL записывать событие в таблицу `outbox_events` в той же
транзакции, что и изменение заявки, с aggregateType `SELLER_APPLICATION`,
aggregateId равным id заявки, eventType, payload в JSON, createdAt текущего
времени и processed = false. События SHALL записываться только при подаче
заявки любого типа (REGISTRATION, UPDATE, DELETION, UNBLOCK), при одобрении и
при отклонении. Payload SELLER_APPROVED SHALL содержать applicationType.

#### Scenario: Payload событий
- **WHEN** записывается SELLER_APPLICATION_CREATED или SELLER_APPROVED
- **THEN** payload содержит applicationId, keycloakUserId, sellerName, email из заявки

#### Scenario: Одинаковое событие для всех типов заявок
- **WHEN** подается заявка REGISTRATION, UPDATE, DELETION или UNBLOCK
- **THEN** записывается событие с eventType SELLER_APPLICATION_CREATED и одним и тем же набором полей payload
- **AND** payload не содержит тип заявки

#### Scenario: Тип заявки в одобрении
- **WHEN** одобряется заявка типа REGISTRATION, UPDATE, DELETION или UNBLOCK
- **THEN** payload SELLER_APPROVED дополнительно содержит applicationType с этим типом

#### Scenario: Payload отклонения
- **WHEN** записывается SELLER_REJECTED
- **THEN** payload дополнительно содержит moderatorComment и не содержит applicationType

#### Scenario: Откат транзакции
- **WHEN** транзакция подачи или модерации откатывается
- **THEN** событие в outbox не сохраняется

#### Scenario: Обезличивание после публикации
- **WHEN** событие SELLER_APPLICATION подтверждено Kafka и запись помечена processed
- **THEN** payload записи заменен на `{"applicationId": <id>, "redacted": true}` (`integration-events` `Transactional outbox recording`)

### Requirement: Seller event email notifications
notification-service SHALL потреблять `seller-events-topic` (consumer group
`notification-group`) и отправлять email от `spring.mail.from`: на
SELLER_APPLICATION_CREATED — на `notification.moderator.email`, на
SELLER_APPROVED и SELLER_REJECTED — на email из события. Письмо на
SELLER_APPROVED SHALL выбираться по applicationType (REGISTRATION, UPDATE,
DELETION, UNBLOCK). Сообщение без eventType или payload SHALL пропускаться с
предупреждением в логе.

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

#### Scenario: Одобрение разблокировки
- **WHEN** получено SELLER_APPROVED с applicationType UNBLOCK
- **THEN** на email из события отправляется письмо "Seller account unblocked" о том, что продавец снова может управлять магазинами и товарами, а отдельно заблокированные магазины остаются заблокированными
- **AND** письмо содержит только sellerName из персональных данных

#### Scenario: Одобрение без типа заявки
- **WHEN** получено SELLER_APPROVED без applicationType или с пустым значением (например, событие записано до этого change)
- **THEN** отправляется письмо "Seller application approved!", как для REGISTRATION

#### Scenario: Одобрение с неизвестным типом заявки
- **WHEN** получено SELLER_APPROVED с непустым applicationType, отличным от REGISTRATION, UPDATE, DELETION и UNBLOCK (например, "FOO" или "update")
- **THEN** письмо не отправляется, в лог пишется ошибка с applicationId и значением типа
- **AND** сообщение обрабатывается по правилам `Seller events consumer error handling` для неизвестного типа заявки

#### Scenario: Отклонение
- **WHEN** получено SELLER_REJECTED
- **THEN** на email из события отправляется письмо "Seller application rejected" с moderatorComment в качестве причины

#### Scenario: Telegram
- **WHEN** обрабатывается любое событие seller-events-topic
- **THEN** уведомление в Telegram не отправляется

### Requirement: Single pending application per user
seller-service SHALL отклонять с кодом 409 подачу заявки любого типа
(REGISTRATION, UPDATE, DELETION, UNBLOCK), если у пользователя уже есть заявка
любого типа в статусе PENDING. Проверка SHALL выполняться на уровне приложения
перед сохранением заявки.

#### Scenario: Повторная подача при PENDING
- **WHEN** у пользователя есть PENDING-заявка типа REGISTRATION и он подает еще одну заявку
- **THEN** seller-service отвечает 409 и новую заявку не создает

#### Scenario: Подача после рассмотрения
- **WHEN** все заявки пользователя в статусах APPROVED или REJECTED
- **THEN** ограничение на одну PENDING-заявку не препятствует подаче

#### Scenario: Разблокировка при PENDING-заявке на удаление
- **WHEN** у заблокированного продавца есть PENDING-заявка DELETION и он подает UNBLOCK
- **THEN** seller-service отвечает 409

#### Scenario: Заблокированный продавец с PENDING UPDATE
- **WHEN** у продавца, заблокированного после подачи UPDATE, эта заявка в PENDING, и он подает UNBLOCK или DELETION
- **THEN** seller-service отвечает 409; подать заявку можно после отклонения UPDATE модератором или разблокировки

**Assumption**: ASM-02

### Requirement: Update application approval
При одобрении заявки UPDATE seller-service SHALL найти Seller по keycloakUserId
заявки (иначе 404) и SHALL перенести в него каждое из полей sellerName, inn,
phoneNumber, email, paymentAccount заявки, значение которого не равно null, не
меняя статус Seller и не вызывая user-service. Если текущий статус Seller
BLOCKED, одобрение SHALL отклоняться с 409, заявка SHALL оставаться PENDING и
может быть отклонена; после разблокировки она SHALL NOT применяться
автоматически.

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

#### Scenario: Продавец заблокирован после подачи
- **WHEN** модератор одобряет UPDATE, поданную до блокировки продавца
- **THEN** seller-service отвечает 409, данные Seller не меняются, заявка остается PENDING

#### Scenario: Отклонение при блокировке
- **WHEN** модератор отклоняет такую заявку
- **THEN** заявка становится REJECTED, как обычно

#### Scenario: Разблокировка не применяет заявку
- **WHEN** продавца разблокировали, а его UPDATE в PENDING
- **THEN** данные Seller не меняются до явного одобрения модератором

### Requirement: Seller events consumer error handling
Ошибка отправки email (MailException) SHALL логироваться без повторной попытки
и без адреса получателя (только applicationId и eventType), и сообщение SHALL
считаться обработанным. Ошибка обработки сообщения SHALL повторяться 3 раза с
интервалом 1 секунда, затем публиковаться в `seller-events-topic.DLT`.
SELLER_APPROVED с неизвестным непустым applicationType SHALL публиковаться в
DLT сразу, без повторов. Потребителя DLT SHALL NOT существовать.

#### Scenario: SMTP недоступен
- **WHEN** отправка письма продавцу завершилась MailException
- **THEN** ошибка логируется с applicationId и eventType без email, повторной отправки нет

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения не является валидным конвертом
- **THEN** после 3 повторов сообщение публикуется в seller-events-topic.DLT

#### Scenario: Неизвестный тип заявки
- **WHEN** получено SELLER_APPROVED с applicationType "FOO"
- **THEN** сообщение после первой попытки публикуется в seller-events-topic.DLT с исходными ключом и значением, повторов нет, письмо не отправлено

**Assumption**: ASM-03, ASM-04, ASM-07
