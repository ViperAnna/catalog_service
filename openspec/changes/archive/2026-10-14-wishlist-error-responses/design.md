# Design

## Context

Мотивация — proposal.md. Проверено по коду user-service:
- `CatalogClient` бросает `ProductNotFoundException(PRODUCT_NOT_FOUND)` без
  аргумента (и для `GET /products/{id}`, и для `POST /products/by-ids`) и
  `CatalogServiceException` для 5xx. Оба — `@ResponseStatus`-исключения,
  `GlobalExceptionHandler` их не обрабатывает, поэтому тело — стандартное
  Spring.
- `WishlistServiceImpl.removeProduct` бросает
  `ResourceConflictException(PRODUCT_NOT_FOUND, productId)` (409).
- `WishlistServiceImpl.updateWishlist` вызывает `validateUniqueName` до
  `getWishlist`.

## Goals / Non-Goals

**Goals:** единый формат `{message, time}` для всех ошибок вишлистов,
корректные коды для «нет товара в вишлисте» и «нет вишлиста» при
переименовании, сообщения без `[%s]`.

**Non-Goals:** таймауты и сетевые ошибки WebClient (ASM-02), смена кода при
отклонении JWT каталогом (ASM-01), отложенные OBS из proposal.

## Decisions

- **D1. Обработчики в `GlobalExceptionHandler`.** Добавить
  `@ExceptionHandler` для `ProductNotFoundException` (404) и
  `CatalogServiceException` (503) с телом `{message, time}` — тем же способом,
  что для `ResourceNotFoundException`. *Альтернатива:* общий обработчик
  `ClientException` по `@ResponseStatus` — шире, затрагивает остальные
  исключения сервиса; отклонено ради минимального объема.
- **D2. Сообщения каталога.** `getProductById` передает productId в
  `PRODUCT_NOT_FOUND`. Для `getProductsByIds` (несколько id) 4xx — новое
  сообщение без шаблона `Products not found in catalog.`; код 404
  сохраняется (поведение 4xx при чтении вишлиста spec не задает, и менять его
  здесь не нужно).
- **D3. Нет товара в вишлисте.** `ResourceNotFoundException` с
  `WISHLIST_ITEM_NOT_FOUND` (404), ничего не сохраняется.
- **D4. Порядок проверок при переименовании.** `getWishlist` (404) до
  `validateUniqueName` (409). Транзакция и запросы не меняются.

## Risks / Trade-offs

- [Клиент ожидает 409 при удалении отсутствующего товара] → frontend коды
  ответов вишлистов не разбирает (общий `catch`); других клиентов API
  вишлистов в репозитории нет.
- [Тесты user-service выполняются только с ручным флагом Docker API
  (`restore-user-service-tests` приостановлен)] → новые тесты этого change —
  Mockito и `@WebMvcTest` без Testcontainers, запускаются выборочно
  `-Dtest=...` без Docker.
