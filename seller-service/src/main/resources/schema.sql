-- Выполняется Spring Boot SQL-инициализацией после ddl-auto: update
-- (spring.sql.init.mode=always, spring.jpa.defer-datasource-initialization=true).
-- Команды идемпотентны: повторный старт сервиса их пропускает.

-- Имя магазина уникально в пределах продавца без учета регистра и пробелов по краям.
CREATE UNIQUE INDEX IF NOT EXISTS uq_stores_seller_store_name
    ON stores (seller_id, lower(btrim(store_name)));

-- Имя продавца уникально без учета регистра и пробелов по краям (change seller-service-error-handling).
-- Существующее ограничение Hibernate UNIQUE (seller_name) остается.
CREATE UNIQUE INDEX IF NOT EXISTS uq_sellers_seller_name
    ON sellers (lower(btrim(seller_name)));
