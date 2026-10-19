# Tasks

`skipTests=true` в user-service не меняется. Тесты без Docker:
`mvn -f user-service/pom.xml test -DskipTests=false -Dtest='Wishlist*Test'`.

## 1. id в ответе создания (OBS-01)

- [x] 1.1 Тест (до кода) в `WishlistWebMvcTest`: `POST /wishlists` с валидным именем → 201, тело содержит `message` "The wishlist was successfully created", `time` и `id`, равный id из результата `WishlistService.createWishlist`. Проверка: тест падает на текущем коде
- [x] 1.2 `WishlistCreatedResponse` и контроллер возвращает его с id из результата сервиса (D1). Проверка: тесты `Wishlist*Test` проходят

## 2. Проверка change

- [x] 2.1 Границы: изменены только `WishlistController`, новый DTO и тест; общий `Response` и `user-service/pom.xml` не изменены. Проверка: `git diff --stat`
- [x] 2.2 `openspec validate wishlist-create-response-id --strict` проходит; после archive отметить OBS-01 устраненным в spec `wishlists`. Проверка: strict validate spec `wishlists`

## 3. Ручные проверки (только по запросу пользователя)

- [ ] 3.1 Стенд через gateway: `POST /wishlists` возвращает id, `GET /wishlists/{id}` по нему возвращает вишлист; создание вишлиста во frontend работает
