# Tasks

`skipTests=true` в catalog-service и user-service не меняется. Тесты без
Docker (Mockito и заглушки), запуск выборочно `-DskipTests=false -Dtest=...`.

## 1. catalog-service: outbox и запись событий (D1, D2)

- [x] 1.1 Тесты (до кода): удаление владельцем пишет outbox-запись `PRODUCT_DELETED:<id>` до `productRepo.delete`; несуществующий товар и чужой магазин — без записи; `StoreProductsPurger` пишет записи для всех товаров магазина после удаления изображений и до `remove`; повторная запись того же id не создает второй документ; архивация записей не пишет. Проверка: тесты падают на текущем коде
- [x] 1.2 Документ outbox, репозиторий, запись в `deleteProductById` и `StoreProductsPurger`, `NewTopic` `product-events-topic`, producer-настройки Kafka. Проверка: тесты 1.1 проходят; `mvn -q -f catalog-service/pom.xml package`

## 2. catalog-service: публикатор (D3)

- [x] 2.1 Тесты (до кода): PENDING и товара нет → отправка с ключом productId и конвертом D1, затем SENT; товар есть и запись моложе 1 часа → без отправки; старше → DISCARDED и ERROR; запись DISCARDED при повторном удалении становится PENDING (D2); ошибка отправки → цикл прекращается, запись PENDING. Проверка: тесты падают на текущем коде
- [x] 2.2 Публикатор `@Scheduled(fixedRate = 3000)`. Проверка: тесты 2.1 проходят

## 3. user-service: потребитель (D4)

- [x] 3.1 Тесты (до кода): PRODUCT_DELETED вызывает удаление id из всех вишлистов; сборка вишлиста (`WishlistResponseBuilder`) не показывает товар, которого нет в ответе by-ids, и не удаляет его id (D5, закрепление текущего поведения); повтор без ошибки; неизвестный eventType — без удаления; невалидный JSON — исключение (для DLT). Проверка: тесты падают на текущем коде
- [x] 3.2 Listener, запрос удаления в `WishlistRepository`, consumer-настройки и `DefaultErrorHandler` с DLT. Проверка: тесты 3.1 и существующие тесты user-service без Docker проходят; `mvn -q -f user-service/pom.xml package`

## 4. Проверка change

- [x] 4.1 Границы: catalog-service (outbox, публикатор, удаление, purge, конфигурация) и user-service (consumer, репозиторий, конфигурация); `pom.xml` без новых зависимостей (spring-kafka уже есть в обоих). Проверка: `git diff --stat`
- [x] 4.2 `openspec validate product-deletion-sync --strict` проходит; при archive свести `integration-events` с `add-order-service`, если тот архивирован раньше (R3); после archive — OBS-05 и OQ-02 в `wishlists` устранены/решены, Event Catalog `integration-events` — строка `product-events-topic`, OBS-13 обновить. Проверка: strict validate затронутых specs

## 5. Ручные операции (только по запросу пользователя)

- [ ] 5.1 Стенд: удалить товар владельцем и через `STORE_PURGED` → сообщения в `product-events-topic`, id исчезает из вишлистов; при остановленной Kafka событие уходит после запуска; группа `user-service-group` получила партиции
- [ ] 5.2 Процедура D6: сверка id из `wishlist_products` с MongoDB и удаление отсутствующих (id, удаленные до change, и остатки гонки)
