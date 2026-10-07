## ADDED Requirements

### Requirement: Store API error responses
Ответы `/stores/**` об ошибках SHALL иметь формат из требования
`Seller service error response format` capability `seller-onboarding`: тело
JSON `{message, time}`. Коды ответов SHALL NOT меняться. 409 из-за занятого
имени магазина SHALL содержать message `Store with name [<storeName>] already exists.`,
а 404 из-за отсутствия магазина — `Store not found with id: [<id>].`

#### Scenario: Дубликат имени магазина
- **WHEN** у продавца есть магазин "Shop" и он создает магазин "shop"
- **THEN** seller-service отвечает 409 с телом `{message, time}` и message `Store with name [shop] already exists.`

#### Scenario: Чужой или несуществующий магазин
- **WHEN** продавец запрашивает `GET /stores/999999`, а такого магазина у него нет
- **THEN** seller-service отвечает 404 с message `Store not found with id: [999999].`

#### Scenario: Ошибка валидации магазина
- **WHEN** продавец создает магазин без phoneNumber
- **THEN** seller-service отвечает 400 с телом `{message, time}`, где message начинается с `Validation failed: `
