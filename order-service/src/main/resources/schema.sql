-- Выполняется Spring Boot SQL-инициализацией после ddl-auto: update
-- (spring.sql.init.mode=always, spring.jpa.defer-datasource-initialization=true).
-- Команды идемпотентны: CHECK пересоздаются, индексы создаются IF NOT EXISTS.

-- Количество позиции корзины: 1..99.
ALTER TABLE cart_items DROP CONSTRAINT IF EXISTS ck_cart_items_quantity;
ALTER TABLE cart_items ADD CONSTRAINT ck_cart_items_quantity
    CHECK (quantity BETWEEN 1 AND 99);

-- Статус заказа. Имя совпадает с CHECK, который Hibernate создает для enum при создании таблицы,
-- ddl-auto его не обновляет: при добавлении статуса список меняется здесь.
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_status_check;
ALTER TABLE orders ADD CONSTRAINT orders_status_check
    CHECK (status IN ('CREATED', 'CANCELLED'));

ALTER TABLE orders DROP CONSTRAINT IF EXISTS ck_orders_total_amount;
ALTER TABLE orders ADD CONSTRAINT ck_orders_total_amount CHECK (total_amount > 0);

ALTER TABLE orders DROP CONSTRAINT IF EXISTS ck_orders_items_count;
ALTER TABLE orders ADD CONSTRAINT ck_orders_items_count CHECK (items_count > 0);

-- Снимок позиции заказа.
ALTER TABLE order_items DROP CONSTRAINT IF EXISTS ck_order_items_unit_price;
ALTER TABLE order_items ADD CONSTRAINT ck_order_items_unit_price CHECK (unit_price > 0);

ALTER TABLE order_items DROP CONSTRAINT IF EXISTS ck_order_items_quantity;
ALTER TABLE order_items ADD CONSTRAINT ck_order_items_quantity CHECK (quantity BETWEEN 1 AND 99);

ALTER TABLE order_items DROP CONSTRAINT IF EXISTS ck_order_items_line_total;
ALTER TABLE order_items ADD CONSTRAINT ck_order_items_line_total CHECK (line_total > 0);

-- История заказов пользователя (сортировка по умолчанию createdAt desc).
CREATE INDEX IF NOT EXISTS ix_orders_user_created_at ON orders (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS ix_order_items_order_id ON order_items (order_id);

-- Выборка публикатора outbox.
CREATE INDEX IF NOT EXISTS ix_outbox_events_processed_created_at ON outbox_events (processed, created_at);
