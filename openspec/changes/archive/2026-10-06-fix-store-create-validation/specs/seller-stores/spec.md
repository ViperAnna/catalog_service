## ADDED Requirements

### Requirement: Store phone normalization
seller-service SHALL принимать phoneNumber магазина только в формах
`+7XXXXXXXXXX` и `8XXXXXXXXXX`, где X — цифра, и SHALL сохранять его в
формате `+7XXXXXXXXXX`: у формы `8XXXXXXXXXX` первая цифра заменяется на
`+7`. Форма `7XXXXXXXXXX` без плюса, пробелы, скобки, дефисы и номера других
стран SHALL отклоняться с кодом 400. Правило SHALL действовать при создании и
при изменении магазина.

#### Scenario: Форма с восьмеркой
- **WHEN** продавец создает магазин с phoneNumber "89991234567"
- **THEN** магазин сохраняется с phoneNumber "+79991234567", и ответ содержит "+79991234567"

#### Scenario: Каноническая форма
- **WHEN** продавец передает phoneNumber "+79991234567"
- **THEN** сохраняется "+79991234567"

#### Scenario: Форма без плюса
- **WHEN** продавец передает phoneNumber "79991234567"
- **THEN** seller-service отвечает 400

#### Scenario: Номер с разделителями
- **WHEN** продавец передает phoneNumber "+7 999 123-45-67"
- **THEN** seller-service отвечает 400

#### Scenario: Изменение телефона в форме с восьмеркой
- **WHEN** продавец меняет phoneNumber магазина через `PUT /stores/{id}` на "89990001122"
- **THEN** сохраняется "+79990001122"

## MODIFIED Requirements

### Requirement: Store creation
seller-service SHALL принимать `POST /stores` с телом JSON
`{storeName, description, email, phoneNumber}`, где storeName, email и
phoneNumber обязательны, и SHALL создавать магазин, принадлежащий текущему
Seller, с переданными значениями полей и нормализованным телефоном. Ответ
SHALL иметь код 201 и содержать созданный магазин.

#### Scenario: Успешное создание
- **WHEN** продавец отправляет валидный запрос с уникальным для него storeName, email и phoneNumber
- **THEN** создается магазин текущего Seller
- **AND** ответ имеет код 201 и содержит id, storeName, description, email, createdAt, updatedAt

#### Scenario: Создание без email или телефона
- **WHEN** продавец не передает email или phoneNumber
- **THEN** seller-service отвечает 400, магазин не создается и обращения к БД на запись нет

### Requirement: Store field validation
seller-service SHALL отклонять с кодом 400 создание магазина, если storeName
пуст или длиннее 100 символов, email отсутствует или не является email,
phoneNumber отсутствует или не соответствует `^(\+7|8)\d{10}$`, или
description длиннее 255 символов. При изменении SHALL действовать те же
ограничения, но email, phoneNumber и description SHALL быть необязательными.
description SHALL быть необязательным и при создании.

#### Scenario: Пустое имя
- **WHEN** storeName пуст
- **THEN** seller-service отвечает 400

#### Scenario: Некорректный телефон
- **WHEN** phoneNumber равен "abc"
- **THEN** seller-service отвечает 400

#### Scenario: Длинное описание
- **WHEN** description содержит от 256 до 500 символов
- **THEN** seller-service отвечает 400 до обращения к БД

#### Scenario: Описание на границе
- **WHEN** description содержит ровно 255 символов
- **THEN** валидация проходит

### Requirement: Store update
seller-service SHALL принимать `PUT /stores/{id}` с телом JSON
`{storeName, description, email, phoneNumber}`, где обязателен только
storeName, и SHALL изменять только магазин текущего Seller (иначе 404).
Каждое из полей storeName, description, email, phoneNumber SHALL меняться
только при значении, отличном от null; phoneNumber SHALL сохраняться
нормализованным. Ответ SHALL иметь код 200 и содержать магазин.

#### Scenario: Изменение имени и описания
- **WHEN** продавец отправляет новое уникальное storeName и новый description
- **THEN** оба поля обновлены, ответ 200

#### Scenario: null не очищает поле
- **WHEN** продавец отправляет description = null
- **THEN** description магазина остается прежним

#### Scenario: Изменение без email и телефона
- **WHEN** продавец отправляет PUT без email и phoneNumber
- **THEN** валидация проходит, email и phoneNumber магазина остаются прежними

#### Scenario: Изменение чужого магазина
- **WHEN** продавец отправляет PUT для магазина другого продавца
- **THEN** seller-service отвечает 404

**Observation**: OBS-05, OBS-06
