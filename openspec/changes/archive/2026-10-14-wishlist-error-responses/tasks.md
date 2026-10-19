# Tasks

`skipTests=true` в user-service не меняется. Тесты этого change не требуют
Docker и запускаются выборочно: `mvn -f user-service/pom.xml test
-DskipTests=false -Dtest='Wishlist*Test'`.

## 1. Ошибки каталога (OBS-03)

- [x] 1.1 Тесты (до кода): `@WebMvcTest` `WishlistController` с моком `WishlistService` — `ProductNotFoundException` → 404, `CatalogServiceException` → 503, оба с телом `{message, time}`; unit-тест `CatalogClient` не нужен, сообщения проверяются в 1.3. Проверка: тесты падают на текущем коде
- [x] 1.2 Обработчики `ProductNotFoundException` и `CatalogServiceException` в `GlobalExceptionHandler` (D1). Проверка: тесты 1.1 проходят
- [x] 1.3 `CatalogClient`: productId в `PRODUCT_NOT_FOUND` для `getProductById`, новое сообщение без шаблона для `getProductsByIds` (D2). Проверка: `mvn -q -f user-service/pom.xml package`; `git grep -n "PRODUCT_NOT_FOUND" user-service/src/main` — каждое использование с аргументом

## 2. Товар не в вишлисте и порядок проверок (OBS-04, OBS-06)

- [x] 2.1 Тесты (до кода): unit-тесты `WishlistServiceImpl` (Mockito) — удаление отсутствующего товара → `ResourceNotFoundException` с message "Product with id [p-1] not found in wishlist." и без `save`; переименование несуществующего вишлиста при занятом имени → `ResourceNotFoundException`, проверка имени не вызывается; переименование в имя другого своего вишлиста → `ResourceConflictException`. Проверка: тесты падают на текущем коде
- [x] 2.2 `removeProduct` (D3) и порядок проверок в `updateWishlist` (D4). Проверка: тесты 2.1 и 1.1 проходят выборочной командой из заголовка

## 3. Проверка change

- [x] 3.1 Границы: изменены только `GlobalExceptionHandler`, `CatalogClient`, `WishlistServiceImpl`, `MessageKeys` и новые тесты user-service; `user-service/pom.xml` не изменен. Проверка: `git diff --stat`
- [x] 3.2 `openspec validate wishlist-error-responses --strict` проходит. После archive обновить non-normative секцию spec `wishlists`: OBS-03, OBS-04, OBS-06 — устранены; OBS-07 — уточнить остаток (продавец-владелец). Проверка: strict validate spec `wishlists`

## 4. Ручные проверки (только по запросу пользователя)

- [ ] 4.1 Стенд через gateway: добавление несуществующего товара (404 с телом), удаление отсутствующего товара (404), переименование чужого вишлиста в занятое имя (404)
