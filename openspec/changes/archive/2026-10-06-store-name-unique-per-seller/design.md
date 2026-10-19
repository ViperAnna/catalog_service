# Design

## Context

Мотивация описана в proposal.md, требования — в delta spec
`specs/seller-stores/spec.md`.

Текущее состояние:
- Проверка уникальности: `existsBySellerIdAndStoreNameIgnoreCase(sellerId,
  name)` вызывается и при создании, и при изменении, без исключения самого
  магазина (OBS-05). Пробелы по краям не учитываются.
- Таблица `stores` создается `ddl-auto: update`. Индексов, кроме PK, нет.
  `seller_id` — FK на `sellers`.
- Опыт C3 (`user-contact-uniqueness`): `ddl-auto: update` в этом стеке не
  добавил unique для существующей колонки. Функциональные индексы Hibernate
  не создает вообще.
- Проверка на локальной БД в транзакции с откатом подтвердила, что индекс
  `CREATE UNIQUE INDEX ... ON stores (seller_id, lower(btrim(store_name)))`
  создается, а вставка `'  SMOKE STORE '` для того же продавца отклоняется с
  `23505` и сообщением
  `duplicate key value violates unique constraint "uq_stores_seller_store_name"`.
  После `ROLLBACK` индекса нет. Дублей и имен с пробелами по краям в текущей
  БД нет.

## Goals / Non-Goals

**Goals:**
- Одно выражение нормализации — `lower(btrim(store_name))` — в проверке и в
  индексе.
- Индекс создается автоматически, идемпотентно, после схемы Hibernate. При
  дублях старт явно падает.
- Стабильное имя индекса для перевода гонки в 409.

**Non-Goals:**
- Отдельная колонка `normalized_store_name`, инструменты миграций, изменение
  остальных таблиц.

## Decisions

### D1. Хранение: одна колонка, `trim` при записи
`StoreServiceImpl` применяет `trim` к storeName перед проверкой и записью: при
создании после `toEntity`, при изменении, если storeName передан. Регистр
сохраняется для отображения. Нормализованная форма для сравнения не хранится,
ее вычисляет БД в индексе и в запросе. Так ключ не может разойтись с данными.

Альтернатива — колонка `normalized_store_name` с `@UniqueConstraint`.
Отклонена: старые строки пришлось бы заполнять вручную (миграция), а
создание unique через `ddl-auto` на существующей таблице ненадежно (C3).

### D2. Индекс через `schema.sql` и SQL-инициализацию Spring Boot
`seller-service/src/main/resources/schema.sql`:
```sql
CREATE UNIQUE INDEX IF NOT EXISTS uq_stores_seller_store_name
    ON stores (seller_id, lower(btrim(store_name)));
```
`application.yaml`:
```yaml
spring:
  sql:
    init:
      mode: always
  jpa:
    defer-datasource-initialization: true
```

**Порядок инициализации в Spring Boot 3.3.4** (проверено по байткоду
`spring-boot-3.3.4.jar`):
- `JpaDatabaseInitializerDetector` при
  `spring.jpa.defer-datasource-initialization=true` регистрирует
  `EntityManagerFactory` как инициализатор БД, а в `detectionComplete`
  (`configureOtherInitializersToDependOnJpaInitializers`) добавляет его в
  `dependsOn` остальным инициализаторам. Скриптовый инициализатор
  `SqlDataSourceScriptDatabaseInitializer` (`schema.sql`) поэтому создается
  после `EntityManagerFactory`, то есть после `ddl-auto: update`.
- `JpaDependsOnDatabaseInitializationDetector` при том же свойстве возвращает
  пустой набор: `EntityManagerFactory` больше не ждет скриптов, и цикла нет.
- Без этого свойства порядок обратный: скрипты выполняются до Hibernate, и на
  пустой БД `CREATE INDEX` упал бы, потому что таблицы `stores` еще нет.

Другие свойства:
- `spring.sql.init.mode=always` нужен, потому что по умолчанию (`embedded`)
  скрипты для PostgreSQL не выполняются.
- `spring.sql.init.continue-on-error` по умолчанию `false`: если в данных есть
  дубли, `CREATE UNIQUE INDEX` падает, и сервис не стартует.
- `IF NOT EXISTS` делает повторный старт безопасным. Ограничение: PostgreSQL
  сравнивает только имя индекса, поэтому если индекс с этим именем уже
  существует с другим определением, команда ничего не сделает. Риск
  смягчается ручной проверкой определения после запуска (tasks).
- `data.sql` в seller-service нет, поэтому `mode=always` больше ничего не
  выполняет.

Альтернатива — ручной `CREATE INDEX` на каждой среде. Отклонена: его легко
забыть при деплое, и тогда гонка снова не защищена. Альтернатива — Flyway.
Отклонена по решению не вводить новый инструмент.

### D3. Проверка в сервисе тем же выражением в БД
В `StoreRepository` добавляется JPQL-запрос:
```java
@Query("select count(s) > 0 from Store s where s.seller.id = :sellerId " +
       "and lower(trim(s.storeName)) = lower(trim(:name)) " +
       "and (:excludeId is null or s.id <> :excludeId)")
boolean existsSameName(Long sellerId, String name, Long excludeId);
```
Создание передает `excludeId = null`, изменение — id магазина. Сравнение в БД
совпадает по правилам регистра с индексом. `existsBySellerIdAndStoreNameIgnoreCase`
перестает использоваться и удаляется.

### D4. Перевод нарушения индекса в 409
`createStore` и `updateStore` используют `saveAndFlush`. При
`DataIntegrityViolationException` в цепочке причин ищется `SQLException` с
`SQLState = "23505"`, а в его тексте — имя `uq_stores_seller_store_name`. Тогда
выбрасывается `ResourceConflictException(STORE_ALREADY_EXISTS, storeName)`,
иначе исключение пробрасывается дальше. Используется `java.sql.SQLException`:
драйвер PostgreSQL подключен со scope `runtime`, как в C3. Здесь, в отличие от
C3, можно опираться на имя индекса, потому что его задаем мы сами.

### D5. Стратегия тестов
Unit-тесты (JUnit 5, Mockito, реальный `StoreMapperImpl`) покрывают `trim`,
передачу `excludeId`, 409 при положительной проверке и перевод нарушения
индекса в 409 на синтетическом `DataIntegrityViolationException` с
`SQLException("...uq_stores_seller_store_name...", "23505")`.

JPQL-запрос и индекс проверяются вручную на локальной БД. Testcontainers в
seller-service нет, а на этой машине Testcontainers 1.19.8 не работает с
Docker Desktop 24 без фиксации `api.version` (C1), которую фиксировать
нельзя.

## Risks / Trade-offs

- [Дубли в данных на production блокируют старт seller-service] → Это
  осознанно, лучше чем молчаливый пропуск. Перед деплоем выполняется проверка
  дублей (tasks). Если дубли найдены, их нужно разрешить до деплоя.
- [Индекс с тем же именем, но другим определением] → Проверка определения
  после запуска (tasks).
- [`schema.sql` становится местом для DDL вне `ddl-auto`] → Сейчас там одна
  идемпотентная команда. Если появятся другие, это стоит пересмотреть в пользу
  инструмента миграций.
- [`saveAndFlush` в транзакции] → Один INSERT или UPDATE, влияние
  пренебрежимо.

## Migration Plan

1. До запуска: проверить `stores` на дубли по
   `(seller_id, lower(btrim(store_name)))` и на имена с пробелами по краям
   (локальная БД; затем production).
2. Задеплоить seller-service. При старте Hibernate обновит схему, затем
   `schema.sql` создаст индекс.
3. После старта: проверить в `pg_indexes`, что `uq_stores_seller_store_name`
   существует и имеет нужное определение.
4. Откат: вернуть предыдущий образ. Индекс останется. Старый код с ним
   совместим, но при гонке получит 500, а не 409. При необходимости индекс
   удаляется вручную: `DROP INDEX uq_stores_seller_store_name`.
