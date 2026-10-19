# Proposal

## Why

Вызовы catalog-service из user-service (`CatalogClient`, вишлисты) не
ограничены по времени и не обрабатывают сетевые ошибки (spec `wishlists`
ASM-02): обрабатываются только ответы 4xx и 5xx. Если каталог не отвечает,
запрос пользователя ждет без ограничения; при отказе соединения ответ —
необработанная ошибка, а не 503 `{message, time}`, как при ответе 5xx.
catalog-service и order-service уже используют таймауты 2 с / 5 с.

## What Changes

- Таймауты вызовов catalog-service из user-service: подключение 2 с, ответ
  5 с — как в catalog-service и order-service.
- Таймаут, отказ соединения и другие сетевые ошибки при вызове каталога дают
  503 с message "Catalog service is unavailable." и телом `{message, time}` —
  так же, как ответ 5xx. Затрагивает добавление товара и чтение вишлиста
  (а также ответы добавления и удаления товара, которые собирают вишлист).
- Не входят: повторы запросов, circuit breaker, проверка issuer (ASM-01).

## Capabilities

### New Capabilities
<!-- нет -->

### Modified Capabilities
- `wishlists`: недоступность catalog-service включает таймаут и сетевые
  ошибки.

## Impact

- Код user-service: `WebClientConf` (таймауты; единственный потребитель —
  `CatalogClient`), `CatalogClient`. БД, другие сервисы и `skipTests` не
  меняются.
- После archive: ASM-02 в spec `wishlists` — снять (поведение стало
  нормативным).
