# Design

## Context

- `WebClientConf` создает `@LoadBalanced WebClient.Builder` без настроек
  HttpClient. Единственный потребитель — `CatalogClient`.
- `CatalogClient` обрабатывает `onStatus` 4xx (404) и 5xx (503) и вызывает
  `block()` без ограничения времени. Ошибки соединения и чтения приходят как
  `WebClientRequestException` и не обрабатываются.
- Отсутствие экземпляров catalog-service в Eureka балансировщик возвращает
  как ответ 503 — уже обрабатывается веткой 5xx.

## Goals / Non-Goals

**Goals:** ограниченное время ожидания каталога и единый 503 для любых сбоев
связи.

**Non-Goals:** повторы, circuit breaker, разные сообщения для таймаута и
отказа соединения.

## Decisions

- **D1. Таймауты на уровне HttpClient** в `WebClientConf`, как в
  catalog-service и order-service: `CONNECT_TIMEOUT_MILLIS` 2000,
  `responseTimeout` 5 с. *Альтернатива:* `Mono.timeout` в каждом вызове —
  дублирование и не покрывает подключение.
- **D2. Сетевые ошибки в 503.** В обоих методах `CatalogClient` —
  `onErrorMap(WebClientRequestException.class, ...)` в
  `CatalogServiceException(CATALOG_SERVICE_UNAVAILABLE)`. Таймаут ответа
  Reactor Netty приходит как `WebClientRequestException` (причина —
  `ReadTimeoutException`) и попадает в ту же ветку.

## Risks / Trade-offs

- [Медленный каталог > 5 с при нормальной работе] → те же значения уже
  работают в catalog-service и order-service; `by-ids` для вишлиста — один
  запрос.
- [Таймаут при уже выполненном изменении вишлиста] → при добавлении товара
  каталог вызывается до изменения; сборка ответа после изменения выполняется
  в той же транзакции, и `CatalogServiceException` (RuntimeException) ее
  откатывает — поведение ASM-04 не меняется.
