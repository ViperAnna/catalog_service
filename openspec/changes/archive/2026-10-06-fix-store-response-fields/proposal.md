# Proposal

## Why

Ответы API магазинов seller-service всегда возвращают `sellerId = null` и
`phone = null` (`seller-stores` OBS-01, OBS-02). Сгенерированный
`StoreMapperImpl` эти поля не заполняет: у `Store` есть только связь `seller`
без маппинга `seller.id`, а телефон в сущности называется `phoneNumber`, а в
DTO — `phone`. Из-за этого клиент не видит телефон магазина и id владельца.
Кроме того, имя поля в ответе расходится с запросом (`phoneNumber`) и с копией
`StoreResponse` в catalog-service (`phoneNumber`).

Change S1 из разбора OBS `seller-stores`.

## What Changes

- Поле ответа `phone` переименовывается в `phoneNumber`, как в запросе
  `StoreRequest`, в сущности `Store` и в копии `StoreResponse` catalog-service.
  Поле заполняется телефоном магазина.
- Поле `sellerId` заполняется id продавца-владельца магазина.
- **BREAKING (API):** в ответах `POST /stores`, `GET /stores`,
  `GET /stores/{id}`, `PUT /stores/{id}` больше нет поля `phone`, вместо него
  `phoneNumber`. Сейчас поле `phone` всегда было `null`, а frontend `/stores`
  не вызывает.
- Запросы, коды ответов, правила доступа и остальные поля ответа не меняются.

### Out of scope

- Остальные OBS `seller-stores`: S2 (уникальность имени, OBS-05, OBS-10),
  S3 (API против схемы БД, OBS-03, OBS-04), S4 (обработка ошибок, OBS-11),
  A1, A2 и D1.
- Изменения catalog-service. Он берет из ответа только `id`, его копия
  `StoreResponse` уже содержит `phoneNumber` и `sellerId`.
- Формат телефона магазина.

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-stores`: меняется requirement `Store response format`. `sellerId`
  и `phoneNumber` теперь содержат реальные значения, поле `phone` заменено на
  `phoneNumber`.

## Impact

- **seller-service:** `dto/responce/StoreResponse` (переименование поля),
  `mapper/StoreMapper` (маппинг `seller.id` в `sellerId`).
- **catalog-service:** без изменений. Его копия `StoreResponse` начнет
  получать заполненные `sellerId` и `phoneNumber`.
- **frontend, Kafka, Keycloak, БД:** без изменений.
- **Тесты:** в seller-service тестовая инфраструктура уже работает
  (JUnit 5, Mockito, `skipTests` не задан), тесты пишутся в рамках этого
  change.
- **design.md:** не требуется. Изменение в одном сервисе, без новых
  зависимостей, миграций и неоднозначных технических решений. `seller.id`
  читается у lazy-прокси Hibernate без загрузки продавца.
