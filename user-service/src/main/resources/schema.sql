-- Выполняется Spring Boot SQL-инициализацией после ddl-auto: update
-- (spring.sql.init.mode=always, spring.jpa.defer-datasource-initialization=true).
-- Команды идемпотентны: повторный старт сервиса их пропускает.
-- Spring делит скрипт на команды по точке с запятой, поэтому блоки DO не используются.

-- Телефон уникален (change user-contact-uniqueness). ddl-auto не добавил UNIQUE(phone)
-- к существующей таблице, хотя @Column(unique = true) объявлен. NULL допускаются многократно.
CREATE UNIQUE INDEX IF NOT EXISTS uq_users_phone
    ON users (phone);

-- Имя вишлиста уникально в пределах пользователя без учета регистра и пробелов по краям
-- (change user-service-db-constraints). Последняя защита при одновременных запросах.
CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlists_user_name
    ON wishlists (keycloak_user_id, lower(btrim(name)));

-- Товар входит в вишлист не больше одного раза: уникальный индекс и NOT NULL
-- вместо первичного ключа (ADD PRIMARY KEY неидемпотентен без блока DO).
ALTER TABLE wishlist_products ALTER COLUMN product_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_wishlist_products
    ON wishlist_products (wishlist_id, product_id);
