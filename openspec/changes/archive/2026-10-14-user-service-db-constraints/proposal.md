# Proposal

## Why

В user-service схему ведет только `ddl-auto: update`, а он не добавляет
ограничения к существующим таблицам. Поэтому уникальность держится только на
проверках в коде и не защищена от гонки:
- `users.phone` — `@Column(unique = true)` объявлен, но `UNIQUE(phone)` в БД
  нет (change `user-contact-uniqueness`, задача 5.1, решение D4 не сработало);
- имя вишлиста уникально только в коде, с учетом регистра и без обрезки
  пробелов (spec `wishlists` OBS-02);
- у `wishlist_products` нет первичного или уникального ключа (OBS-08).

Решение пользователя: ограничения в БД — идемпотентный `schema.sql` по образцу
seller-service; имя вишлиста уникально без учета регистра и с обрезкой
пробелов по краям.

## What Changes

- user-service получает `schema.sql`, выполняемый после `ddl-auto: update` при
  каждом старте (`spring.sql.init.mode: always`,
  `spring.jpa.defer-datasource-initialization: true`), как в seller-service.
- Уникальный индекс `users (phone)` — реализация ограничения из change
  `user-contact-uniqueness` (его требования не дублируются здесь).
- Имя вишлиста: обрезка пробелов по краям при создании и переименовании,
  уникальность в пределах пользователя без учета регистра; уникальный индекс
  `wishlists (keycloak_user_id, lower(btrim(name)))`. **Изменение
  поведения:** "gifts" при существующем "Gifts" теперь 409.
- `wishlist_products`: уникальный индекс `(wishlist_id, product_id)` и
  `product_id NOT NULL`.
- Нарушение новых индексов при гонке — 409 с теми же сообщениями, что у
  проверок в коде, а не 500.
- Не входят: лимиты числа вишлистов и товаров (OBS-08, бизнес-решение),
  Flyway/Liquibase, изменение существующих данных.

## Capabilities

### New Capabilities
<!-- нет -->

### Modified Capabilities
- `wishlists`: правила уникальности имени при создании и переименовании,
  гонка при добавлении одного товара.

## Impact

- user-service: `application.yml`, новый `schema.sql`, `WishlistServiceImpl`,
  `WishlistRepository`, утилита перевода нарушений в 409, тесты.
- БД `user_service`: три новых индекса и `NOT NULL`. На существующей БД с
  дублями старт user-service упадет — перед деплоем нужна проверка данных
  (design.md, Migration Plan; выполняется только по запросу пользователя).
- `user-contact-uniqueness`: задача 5.1 и D4 указывают на этот change.
- `skipTests`, `pom.xml`, другие сервисы и frontend не меняются.
