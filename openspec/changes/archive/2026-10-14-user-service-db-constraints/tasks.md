# Tasks

`skipTests=true` в user-service не меняется. Тесты без Docker:
`mvn -f user-service/pom.xml test -DskipTests=false -Dtest='Wishlist*Test,*ViolationTranslatorTest'`.
Проверка индексов на реальной БД — ручная (раздел 4), тесты репозиториев с
Testcontainers в этом change не запускаются.

## 1. schema.sql (D1, D2, D5)

- [x] 1.1 `user-service/src/main/resources/schema.sql` с командами D2 и комментариями в стиле seller-service; в `application.yml` — `spring.jpa.defer-datasource-initialization: true` и `spring.sql.init.mode: always`. Проверка: `mvn -q -f user-service/pom.xml package`; `schema.sql` в jar; каждая команда идемпотентна (`IF NOT EXISTS` или повторяемая `SET NOT NULL`), блоков `DO` нет

## 2. Имя вишлиста без регистра и пробелов (D3)

- [x] 2.1 Тесты (до кода) в `WishlistServiceImplTest`: создание "  Books  " сохраняет "Books"; создание при существующем имени в другом регистре → 409 с обрезанным именем; переименование своего вишлиста со сменой регистра проходит (проверка исключает свой id); переименование в чужое имя другого регистра → 409. Проверка: тесты падают на текущем коде
- [x] 2.2 Обрезка имени и проверки `...NameIgnoreCase...` в `WishlistServiceImpl` и `WishlistRepository`; прежние `existsByKeycloakUserIdAndName...` удаляются, если больше не используются. Проверка: тесты 2.1 и существующие `Wishlist*Test` проходят

## 3. Гонка → 409 (D4)

- [x] 3.1 Тесты (до кода): unit-тест утилиты перевода (`DataIntegrityViolationException` с `SQLException` 23505 и текстом `uq_wishlists_user_name` → 409 имени, `uq_wishlist_products` → 409 товара, другое нарушение или другой SQLState → пусто); в `WishlistServiceImplTest` — `saveAndFlush`, бросающий такое исключение при создании, переименовании и добавлении товара, дает `ResourceConflictException` с нужным сообщением. Проверка: тесты падают на текущем коде
- [x] 3.2 Утилита и `saveAndFlush` с переводом в `createWishlist`, `updateWishlist`, `addProduct`. Проверка: тесты 3.1 и все тесты из заголовка проходят

## 4. Связь с C3 и проверка change

- [x] 4.1 В `user-contact-uniqueness`: D4 и задача 5.1 ссылаются на этот change (индекс `uq_users_phone` из `schema.sql` вместо `ddl-auto`); требования C3 не меняются. Проверка: `openspec validate user-contact-uniqueness --strict`
- [x] 4.2 Границы: изменены `application.yml`, `schema.sql`, `WishlistServiceImpl`, `WishlistRepository`, новая утилита и тесты user-service, артефакты C3; `pom.xml` и `User` не изменены. Проверка: `git diff --stat`; все тесты без Docker (`UserSecurityWebMvcTest,UserServiceImplTest,OutboxPublisherTest,CatalogClientTest,Wishlist*Test,*ViolationTranslatorTest`) проходят
- [x] 4.3 `openspec validate user-service-db-constraints --strict` проходит; после archive отметить OBS-02 устраненным и OBS-08 частично (ключ есть, лимитов нет) в spec `wishlists`. Проверка: strict validate spec `wishlists`

## 5. Ручные операции (только по запросу пользователя)

- [x] 5.1 Локальный стенд: пересобрать user-service, проверить старт без ошибок `schema.sql`, `pg_indexes` содержит `uq_users_phone`, `uq_wishlists_user_name`, `uq_wishlist_products`; повторный рестарт без ошибок; через gateway: "gifts" при "Gifts" → 409, "  Books  " сохраняется как "Books". Закрывает и задачу 5.1 C3 для локальной БД
- [ ] 5.2 Production: проверочные запросы Migration Plan (п. 1) до деплоя, проверка индексов после
