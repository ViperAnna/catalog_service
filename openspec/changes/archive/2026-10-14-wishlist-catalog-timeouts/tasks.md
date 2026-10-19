# Tasks

`skipTests=true` в user-service не меняется. Тесты без Docker:
`mvn -f user-service/pom.xml test -DskipTests=false -Dtest='CatalogClientTest,Wishlist*Test'`.

## 1. Сетевые ошибки каталога (ASM-02)

- [x] 1.1 Тесты (до кода) `CatalogClientTest`: `CatalogClient` с `WebClient.Builder`, собранным с заглушкой `ExchangeFunction`, которая возвращает `WebClientRequestException` (отказ соединения и таймаут чтения) для `getProductById` и `getProductsByIds` → `CatalogServiceException` с message "Catalog service is unavailable."; ответы 404 и 500 по-прежнему дают `ProductNotFoundException` и `CatalogServiceException`. Проверка: тесты сетевых ошибок падают на текущем коде
- [x] 1.2 `onErrorMap` для `WebClientRequestException` в обоих методах `CatalogClient` (D2). Проверка: тесты 1.1 проходят

## 2. Таймауты (ASM-02)

- [x] 2.1 `WebClientConf`: HttpClient с подключением 2 с и `responseTimeout` 5 с (D1). Проверка: `mvn -q -f user-service/pom.xml package`; ревью совпадения с `catalog-service` `WebClientConfig`

## 3. Проверка change

- [x] 3.1 Границы: изменены только `WebClientConf`, `CatalogClient` и новый тест; `user-service/pom.xml` не изменен. Проверка: `git diff --stat`; тесты из заголовка проходят
- [x] 3.2 `openspec validate wishlist-catalog-timeouts --strict` проходит; после archive снять ASM-02 в spec `wishlists`. Проверка: strict validate spec `wishlists`

## 4. Ручные проверки (только по запросу пользователя)

- [ ] 4.1 Стенд: при остановленном catalog-service `GET /wishlists/{id}` с товарами и добавление товара отвечают 503 с телом `{message, time}` не дольше чем примерно через 5 с
