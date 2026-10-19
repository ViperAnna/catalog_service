# Tasks

`skipTests=true` в user-service не меняется. Тесты без Docker:
`mvn -f user-service/pom.xml test -DskipTests=false -Dtest='Wishlist*Test,CatalogClientTest,UserSecurityWebMvcTest'`.

## 1. Запрет SELLER (OBS-07, D1)

- [x] 1.1 Тесты (до кода) в `WishlistWebMvcTest`: JWT с ролями USER+SELLER и ADMIN+SELLER → 403 на `GET /wishlists` и `POST /wishlists`; только USER и только ADMIN → доступ сохраняется. Проверка: тесты SELLER падают на текущем коде
- [x] 1.2 Правило доступа в `SpringSecurityConfig` (D1). Проверка: тесты 1.1 и `UserSecurityWebMvcTest` проходят

## 2. Лимиты (OBS-08, D2, D3, D5)

- [x] 2.1 Тесты (до кода) в `WishlistServiceImplTest`: 20 вишлистов → 409 лимита, вызов advisory lock до подсчета; 200 товаров → 409 лимита товаров; вишлист для добавления загружается методом с блокировкой; каталог вызывается до блокировки. Проверка: тесты падают на текущем коде
- [x] 2.2 Advisory lock и подсчет в `createWishlist`, загрузка с `PESSIMISTIC_WRITE` и проверка размера в `addProduct`, сообщения в `MessageKeys`. Проверка: тесты 2.1 и все тесты из заголовка проходят

## 3. Удаление товара без каталога (ASM-04, D4)

- [x] 3.1 Тест (до кода): `removeProduct` при `CatalogServiceException` из сборки ответа сохраняет вишлист без удаленного id, возвращает ответ с пустыми products, остальные id остаются. Проверка: тест падает на текущем коде
- [x] 3.2 Вариант сборки ответа в `WishlistResponseBuilder` и его использование в `removeProduct`. Проверка: тест 3.1 и все тесты из заголовка проходят

## 4. Чистка и проверка change

- [x] 4.1 Удалить `findByName` и `findByKeycloakUserIdAndNameContainingIgnoreCase` (OBS-09). Проверка: `git grep` не находит использований; `mvn -q -f user-service/pom.xml package`
- [x] 4.2 Границы: изменены только файлы из Impact proposal и тесты; `pom.xml` и БД не меняются. Проверка: `git diff --stat`
- [x] 4.3 `openspec validate wishlist-access-and-limits --strict` проходит; после archive в spec `wishlists` — OBS-07, OBS-08, OBS-09 устранены, ASM-03 и ASM-04 сняты; в `access-control` Access Matrix строка wishlists — «USER или ADMIN без SELLER». Проверка: strict validate specs `wishlists` и `access-control`

## 5. Ручные проверки (только по запросу пользователя)

- [ ] 5.1 Стенд: токен с SELLER → 403; лимит 20 и 200 (включая одновременные запросы на границе); удаление товара при остановленном catalog-service → 200, `products = []`, после запуска каталога — товара нет
