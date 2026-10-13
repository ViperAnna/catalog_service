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

-- Статус магазина (change seller-store-lifecycle): ddl-auto добавляет колонку nullable,
-- существующие магазины получают ACTIVE, затем DEFAULT и NOT NULL. Повторный запуск ничего не меняет.
UPDATE stores SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE stores ALTER COLUMN status SET DEFAULT 'ACTIVE';
ALTER TABLE stores ALTER COLUMN status SET NOT NULL;

-- Тип заявки UNBLOCK (change seller-store-lifecycle): Hibernate создал CHECK по значениям enum
-- при создании таблицы, ddl-auto его не обновляет. Пересоздается при каждом старте (идемпотентно).
ALTER TABLE seller_applications DROP CONSTRAINT IF EXISTS seller_applications_application_type_check;
ALTER TABLE seller_applications ADD CONSTRAINT seller_applications_application_type_check
    CHECK (application_type IN ('REGISTRATION', 'UPDATE', 'DELETION', 'UNBLOCK'));

-- Одна PENDING-заявка на изменение магазина (change store-change-moderation): последняя защита
-- при одновременной подаче. Таблицу store_change_requests перед этим создает ddl-auto: update.
CREATE UNIQUE INDEX IF NOT EXISTS uq_store_change_requests_pending
    ON store_change_requests (store_id) WHERE status = 'PENDING';
