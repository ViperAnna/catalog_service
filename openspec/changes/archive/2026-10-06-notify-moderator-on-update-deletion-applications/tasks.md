# Tasks

## 1. Тестовая основа seller-service

- [x] 1.1 Создать `seller-service/src/test/java/ru/seller_service/service/impl/SellerApplicationServiceImplTest.java`: unit-тест на JUnit 5 и Mockito (`spring-boot-starter-test` уже подключен) с моками репозиториев, `CurrentUserService`, `SellerApplicationMapper` и реальным `EventMapper`. Проверка: `mvn -pl seller-service test` запускает класс без ошибок
- [x] 1.2 Добавить регрессионный тест REGISTRATION: успешная подача сохраняет ровно одну outbox-запись с eventType `SELLER_APPLICATION_CREATED`, aggregateType `SELLER_APPLICATION`, aggregateId = id заявки и payload `{applicationId, keycloakUserId, sellerName, email}`. Проверка: тест проходит на текущем коде без изменений production-кода

## 2. Событие при подаче UPDATE

- [x] 2.1 Добавить тесты для UPDATE (до изменения кода они должны падать): (а) успешная подача записывает `SELLER_APPLICATION_CREATED` с applicationId, keycloakUserId, sellerName из Seller и email из запроса; (б) подача без email записывает событие с `email = null`; (в) подача, отклоненная с 404 (нет Seller) и с 409 (Seller не ACTIVE, есть PENDING-заявка), outbox-запись не создает. Проверка: (а) и (б) падают на текущем коде, (в) проходит
- [x] 2.2 В `SellerApplicationServiceImpl.createUpdateRequest` после сохранения заявки записывать `SELLER_APPLICATION_CREATED` через существующий `saveOutbox(...)` (design.md, D2). Проверка: все тесты из 1.2 и 2.1 проходят

## 3. Событие при подаче DELETION

- [x] 3.1 Добавить тесты для DELETION (до изменения кода они должны падать): (а) успешная подача записывает `SELLER_APPLICATION_CREATED` с applicationId, keycloakUserId, sellerName и email из снимка данных Seller; (б) подача, отклоненная с 404 и 409, outbox-запись не создает. Проверка: (а) падает на текущем коде, (б) проходит
- [x] 3.2 В `SellerApplicationServiceImpl.createDeletionRequest` после сохранения заявки записывать `SELLER_APPLICATION_CREATED` через `saveOutbox(...)`. Проверка: `mvn -pl seller-service test` зеленый, тесты из 1.2, 2.1 и 3.1 проходят
- [x] 3.3 Убедиться, что изменения затронули только seller-service, а notification-service, DTO, сущности и схема БД не менялись. Проверка: `git diff --stat` показывает только `SellerApplicationServiceImpl.java` и новые тестовые файлы в seller-service

## 4. Интеграционная проверка

- [ ] 4.1 Ручной e2e на `docker-compose.yml` с mailpit: (1) ACTIVE-продавец подает UPDATE; (2) после модерации подает DELETION. Проверка: в `seller-events-topic` (kafka-ui) появляются два сообщения `SELLER_APPLICATION_CREATED` с ключом = id заявки, в mailpit на `NOTIFICATION_MODERATOR_EMAIL` приходят два письма "New application!", в `seller-events-topic.DLT` новых сообщений нет
- [ ] 4.2 Регрессия REGISTRATION в той же среде: новая заявка на регистрацию по-прежнему дает одно сообщение и одно письмо модератору. Проверка: в kafka-ui и mailpit видно ровно по одному новому сообщению и письму

## 5. Spec после archive

- [x] 5.1 Проверить delta. Проверка: `openspec validate notify-moderator-on-update-deletion-applications --strict` проходит
- [x] 5.2 После `openspec archive` вручную обновить non-normative секции `openspec/specs/seller-onboarding/spec.md` (design.md, D3): пометить запись OBS-03 как устраненную change `notify-moderator-on-update-deletion-applications`, не удаляя идентификатор, и учесть это в OQ-01. Новые OBS не добавлять. Проверка: `openspec validate seller-onboarding --type spec --strict` проходит, а `openspec show seller-onboarding --type spec` показывает новые формулировки трех MODIFIED-requirements
