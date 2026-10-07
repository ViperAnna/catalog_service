## ADDED Requirements

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

## MODIFIED Requirements

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
