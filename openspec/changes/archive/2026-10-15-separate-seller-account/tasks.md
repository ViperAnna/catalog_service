# Tasks

`skipTests=true` во всех модулях не меняется. Тесты без Docker (Mockito,
`@WebMvcTest`), запуск выборочно `-DskipTests=false -Dtest=...`. Keycloak в
тестах — моки Admin API.

## 1. Контракт gRPC (D2, D3, D6, D7)

- [x] 1.1 `user.proto` в user-service и seller-service: `CreateSellerAccount`, `DeleteSellerAccount`, `IsEmailAvailable`; прежние методы сохраняются. Проверка: `mvn -q -f user-service/pom.xml package` и то же для seller-service генерируют стабы

## 2. user-service: аккаунты продавцов (D2, D3, D7)

- [x] 2.1 Тесты (до кода): создание — username/email в нижнем регистре, атрибут `account_type=seller`, составная роль по умолчанию удалена, добавлен SELLER, вызван `executeActionsEmail`; повтор для незавершенного аккаунта продавца возвращает его id; email обычного аккаунта → ALREADY_EXISTS; удаление аккаунта без атрибута → FAILED_PRECONDITION; `IsEmailAvailable`; ошибка Keycloak → INTERNAL. Проверка: тесты падают на текущем коде
- [x] 2.2 Реализация в `KeycloakRoleService`/gRPC-сервисе. Проверка: тесты 2.1 и существующие тесты user-service без Docker проходят

## 3. seller-service: модель и доступ (D1, D4)

- [x] 3.1 Тесты (до кода): `@WebMvcTest` правил D4 (USER/SELLER по путям и методам); проверка «есть Seller» по `keycloakUserId` или `applicantKeycloakUserId`. Проверка: тесты падают на текущем коде
- [x] 3.2 `Seller.applicantKeycloakUserId`, индекс в `schema.sql`, репозиторий, правила доступа. Проверка: тесты 3.1 и существующие тесты seller-service проходят

## 4. seller-service: подача, одобрение, удаление (D5, D6, D7)

- [x] 4.1 Тесты (до кода): подача REGISTRATION с занятым email → 409; одобрение — `CreateSellerAccount`, `keycloakUserId` = id аккаунта, `applicant` = заявитель, `AssignSellerRole` не вызывается; ALREADY_EXISTS → 409; ошибка gRPC → откат; откат после успешного создания → `DeleteSellerAccount`; удаление отдельного продавца → `DeleteSellerAccount` и удаление REGISTRATION-заявок заявителя; существующего → `RemoveSellerRole`. Проверка: тесты падают на текущем коде
- [x] 4.2 Реализация в `SellerApplicationServiceImpl` и `ModeratorServiceImpl`, gRPC-клиент. Проверка: тесты 4.1 и все тесты seller-service проходят

## 5. frontend (D8)

- [x] 5.1 Ссылки и маршруты профиля и вишлистов только при роли USER; для аккаунта без USER — сообщение вместо страницы. Проверка: `npm run build` во frontend проходит; ревью условий по `hasRole('USER')`

## 6. Проверка change

- [x] 6.1 Границы: user-service (gRPC, Keycloak), seller-service (модель, доступ, сервисы, gRPC-клиент, `schema.sql`), frontend (навигация, маршруты); `pom.xml` без новых зависимостей, конфигурация Keycloak не меняется. Проверка: `git diff --stat`
- [x] 6.2 `openspec validate separate-seller-account --strict` проходит; при archive свести `access-control` с другими change, если они архивированы раньше. Проверка: strict validate затронутых specs

## 7. Ручные операции (только по запросу пользователя)

- [ ] 7.1 Стенд: заявка покупателя с отдельным email (адрес с доступом к почте, R4) → одобрение → письмо установки пароля → вход продавцом: магазины и товары доступны, `/users/me`, вишлисты, корзина — 403; покупатель не меняется; удаление продавца удаляет аккаунт; существующий продавец работает как раньше
