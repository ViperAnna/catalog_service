## MODIFIED Requirements

### Requirement: Wishlist creation
user-service SHALL принимать `POST /wishlists` с телом JSON `{name}`, где name
не пуст и не длиннее 100 символов, и SHALL создавать пустой вишлист текущего
пользователя. Если у пользователя уже есть вишлист с точно таким же именем
(с учетом регистра), SHALL возвращаться 409. Ответ SHALL иметь код 201 и тело
`{message, time, id}`, где id — идентификатор созданного вишлиста.

#### Scenario: Успешное создание
- **WHEN** пользователь создает вишлист "Gifts"
- **THEN** ответ 201 с message "The wishlist was successfully created" и id созданного вишлиста; `GET /wishlists/{id}` с этим id возвращает вишлист "Gifts"

#### Scenario: Дубликат имени
- **WHEN** у пользователя уже есть вишлист "Gifts" и он создает "Gifts"
- **THEN** ответ 409 с message "The wishlist with name [Gifts] already exist"

#### Scenario: Имя в другом регистре
- **WHEN** у пользователя есть "Gifts" и он создает "gifts"
- **THEN** вишлист создается

**Observation**: OBS-02
