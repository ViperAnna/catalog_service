## MODIFIED Requirements

### Requirement: Store response format
Ответы API магазинов SHALL содержать объект с полями id, sellerId, storeName,
description, email, phoneNumber, createdAt, updatedAt. Поле sellerId SHALL
содержать id продавца-владельца магазина, а phoneNumber — телефон магазина.
Поле phone SHALL NOT возвращаться.

#### Scenario: Ответ на создание
- **WHEN** создается магазин с phoneNumber "+79990000000" продавцом с id 7
- **THEN** в ответе phoneNumber = "+79990000000" и sellerId = 7
- **AND** поля phone в ответе нет

#### Scenario: Список магазинов
- **WHEN** продавец с id 7 запрашивает `GET /stores`
- **THEN** у каждого магазина в ответе sellerId = 7 и phoneNumber равен сохраненному телефону магазина

#### Scenario: Изменение телефона
- **WHEN** продавец меняет phoneNumber магазина через `PUT /stores/{id}`
- **THEN** ответ содержит новое значение phoneNumber
