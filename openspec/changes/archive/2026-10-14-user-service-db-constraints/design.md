# Design

## Context

- user-service: `ddl-auto: update`, SQL-инициализации нет. Hibernate не
  добавляет ограничения к существующим таблицам: `@Column(unique = true)` на
  `User.phone` не создал `UNIQUE(phone)` (`user-contact-uniqueness`).
- seller-service решает это идемпотентным `schema.sql`
  (`spring.sql.init.mode: always`, `defer-datasource-initialization: true`,
  `CREATE UNIQUE INDEX IF NOT EXISTS ... (seller_id, lower(btrim(store_name)))`).
- `ContactUniqueViolationTranslator` (C3) определяет поле по SQLState 23505 и
  тексту `(phone)=` / `(email)=` — индекс на `users (phone)` дает такой же
  текст, код C3 не меняется.
- `WishlistServiceImpl` помечен `@Transactional` на классе; сохранение через
  `save` без `flush`, поэтому нарушение ограничения проявилось бы при commit
  вне метода — как 500.
- dev-БД (проверено 2026-10-14, только чтение): 1 вишлист, 1 строка
  `wishlist_products`, 0 пользователей; дублей имен (без регистра и пробелов),
  пар `(wishlist_id, product_id)`, телефонов и `product_id IS NULL` нет.

## Goals / Non-Goals

**Goals:** ограничения в БД для phone, имени вишлиста и состава вишлиста;
409 вместо 500 при гонке; обрезка и сравнение имени без регистра.

**Non-Goals:** лимиты, Flyway/Liquibase, правка существующих данных,
изменение требований `user-profile` (они в `user-contact-uniqueness`).

## Decisions

- **D1. `schema.sql` как в seller-service.** В `application.yml`:
  `spring.jpa.defer-datasource-initialization: true`,
  `spring.sql.init.mode: always`. Скрипт выполняется после `ddl-auto` при
  каждом старте; все команды идемпотентны. Spring делит скрипт по `;`, поэтому
  блоки `DO $$ ... $$` не используются.
- **D2. Состав `schema.sql`:**
  ```sql
  CREATE UNIQUE INDEX IF NOT EXISTS uq_users_phone ON users (phone);
  CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlists_user_name
      ON wishlists (keycloak_user_id, lower(btrim(name)));
  ALTER TABLE wishlist_products ALTER COLUMN product_id SET NOT NULL;
  CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_products
      ON wishlist_products (wishlist_id, product_id);
  ```
  NULL в `phone` допускаются многократно (семантика UNIQUE в PostgreSQL).
  Уникальный индекс + `NOT NULL` для `wishlist_products` дает гарантии
  первичного ключа без `DO`-блока. *Альтернатива:* `ADD PRIMARY KEY` —
  неидемпотентно без `DO`.
- **D3. Имя вишлиста в коде.** Сервис обрезает `name` (`strip()`) до проверки
  и сохранения; проверки — `existsByKeycloakUserIdAndNameIgnoreCase` и
  `existsByKeycloakUserIdAndNameIgnoreCaseAndIdNot`. Индекс D2 — последняя
  защита. Валидация `@NotBlank`/`@Size(max = 100)` не меняется (пустое после
  обрезки имя уже отклоняется `@NotBlank`). Сообщение 409 содержит обрезанное
  имя из запроса.
- **D4. Гонка → 409.** Создание, переименование и добавление товара сохраняют
  через `saveAndFlush`; `DataIntegrityViolationException` с SQLState 23505 и
  именем индекса `uq_wishlists_user_name` → 409 `WISHLIST_NAME_ALREADY_EXIST`,
  `uq_wishlist_products` → 409 `PRODUCT_ALREADY_IN_WISHLIST`; иначе исключение
  пробрасывается. Утилита — статическая, по образцу
  `ContactUniqueViolationTranslator` (определение по тексту ошибки; драйвер
  подключен со scope `runtime`).
- **D5. phone.** Только индекс в `schema.sql`; `@Column(unique = true)`,
  проверки и перевод в 409 остаются из C3. В C3 задача 5.1 и D4 ссылаются на
  этот change.

## Risks / Trade-offs

- [Дубли в существующих данных → `CREATE UNIQUE INDEX` падает, user-service не
  стартует] → перед деплоем проверочные запросы (Migration Plan); в dev
  дублей нет.
- [На новой БД Hibernate создаст и свой `UNIQUE(phone)` по
  `@Column(unique = true)` → два одинаковых ограничения] → безвредно;
  аннотацию не трогаем (решение C3).
- [Тесты с Testcontainers используют ту же SQL-инициализацию] → `schema.sql`
  идемпотентен и выполняется после создания таблиц; тесты репозиториев
  запускаются только с флагом Docker API (C1 приостановлен) и в этом change не
  запускаются.

## Migration Plan

1. Перед деплоем на существующую БД (по запросу пользователя, только чтение):
   дубли `(keycloak_user_id, lower(btrim(name)))` в `wishlists`, пары
   `(wishlist_id, product_id)` и `product_id IS NULL` в `wishlist_products`,
   дубли `phone` в `users`. Если есть — решение пользователя до деплоя.
2. Деплой user-service: индексы создаются при старте.
3. Проверка: `pg_indexes` содержит `uq_users_phone`, `uq_wishlists_user_name`,
   `uq_wishlist_products`.
4. Откат: прежний образ работает с новыми индексами (проверки в коде
   строже или совпадают); индексы можно удалить вручную.
