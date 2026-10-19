# Design

## Context

- Роли (`access-control`): USER выдается всем через
  `default-roles-user-service-realm`, SELLER добавляется при одобрении заявки.
  Значит, у продавца в JWT одновременно USER и SELLER. Роль меняется в правах
  только после выпуска нового access token (`Role change propagation`).
- `SpringSecurityConfig` user-service: `/wishlists/**` — `hasAnyRole("USER",
  "ADMIN")`.
- Вишлисты привязаны к `keycloak_user_id`, строки пользователя может не быть
  (профиль не требуется), поэтому заблокировать «пользователя» строкой нельзя.
- `WishlistServiceImpl` транзакционный; уникальность имени и товара уже
  защищена индексами (`user-service-db-constraints`), лимитов нет.
- `removeProduct` после удаления id собирает ответ через `by-ids`; ошибка
  каталога откатывает удаление (ASM-04).

## Goals / Non-Goals

**Goals:** запрет SELLER, лимиты 20/200 без превышения при гонке, удаление
товара без зависимости от каталога, чистка OBS-09/ASM-03.

**Non-Goals:** frontend, удаление вишлистов продавцов, лимиты в БД (CHECK
по количеству строк в PostgreSQL невозможен без триггеров).

## Decisions

- **D1. Правило доступа.** `/wishlists/**` —
  `access((auth, ctx) -> USER или ADMIN && !SELLER)` в `SpringSecurityConfig`.
  *Альтернатива:* `@PreAuthorize` на контроллере — проверки доступа в проекте
  описаны в `SecurityFilterChain`; отклонено.
- **D2. Лимит вишлистов.** В транзакции создания перед подсчетом —
  `pg_advisory_xact_lock(hashtext('wishlists:' || :userId))`, затем
  `countByKeycloakUserId`, при 20 и более — 409. Блокировка сериализует
  создание вишлистов одного пользователя и снимается при завершении
  транзакции. *Альтернатива:* блокировка строки `users` — строки может не
  быть.
- **D3. Лимит товаров.** `addProduct` загружает вишлист с
  `PESSIMISTIC_WRITE` (`SELECT ... FOR UPDATE`), проверяет `productIds.size()`
  (200 и более — 409), добавляет. Одновременные добавления в один вишлист
  выполняются по очереди. Вызов каталога остается до блокировки, чтобы не
  держать блокировку на время сетевого запроса: порядок — каталог, затем
  блокировка и проверки.
- **D4. Удаление без зависимости от каталога.** `WishlistResponseBuilder`
  получает вариант сборки, который при `CatalogServiceException` возвращает
  вишлист с `products = []`; `removeProduct` использует его. Удаление
  сохраняется (`saveAndFlush` до сборки ответа).
- **D5. Сообщения.** `WISHLIST_LIMIT_REACHED` = "Wishlist limit reached: at
  most %d wishlists per user.", `WISHLIST_PRODUCT_LIMIT_REACHED` = "Wishlist
  product limit reached: at most %d products per wishlist."; лимиты —
  константы в сервисе.

## Risks / Trade-offs

- [Продавец видит в frontend вишлисты и получает 403] → frontend не входит;
  нужен отдельный change frontend (скрыть вишлисты при роли SELLER).
- [Роль SELLER действует только после нового токена] → по
  `Role change propagation` до истечения токена (300 с) доступ сохраняется.
- [Advisory lock по hashtext — коллизии хеша] → коллизия лишь сериализует
  запросы двух разных пользователей, корректность не нарушается.
- [Существующие данные сверх лимитов] → не меняются; на dev — 1 вишлист.
- [Гонка проверяется только на реальном PostgreSQL] → unit-тесты проверяют
  вызов блокировки и порядок; поведение под нагрузкой — ручная проверка на
  стенде (tasks).
