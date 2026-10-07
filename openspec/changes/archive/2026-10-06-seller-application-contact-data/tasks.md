# Tasks

## 1. Проверка данных до применения

- [x] 1.1 На локальной БД `seller_service` проверить заявки и Seller без email и с телефоном не в формате `+7XXXXXXXXXX`. Проверка: 2 заявки REGISTRATION и 1 Seller, у всех email заполнен, телефоны `+7XXXXXXXXXX` (design.md, Context). При деплое на production шаг нужно повторить, PENDING-заявки без email разрешить до деплоя

## 2. Тесты (до изменения кода)

- [x] 2.1 Добавить `SellerRequestValidationTest`: регистрация — `+79991234567` и `89991234567` проходят, `79991234567`, `+7 999 123-45-67`, `1234567` дают ошибку телефона, отсутствие и пустой email дают `Email is required.`; UPDATE — то же для phoneNumber, без email валидно. Проверка: тесты не компилируются или падают на текущем коде
- [x] 2.2 Дополнить `SellerApplicationServiceImplTest`: регистрация с `89991234567` сохраняет `+79991234567`; UPDATE переносит нормализованный phoneNumber; UPDATE без email и с пустым email сохраняет email Seller, и он же в SELLER_APPLICATION_CREATED (заменяет тест с `email = null`). Проверка: тесты падают на текущем коде
- [x] 2.3 Дополнить `ModeratorServiceImplTest`: одобрение UPDATE переносит phoneNumber в Seller, SELLER_APPROVED содержит email заявки. Перевести `GlobalExceptionHandlerTest` на `phoneNumber` и добавить 400 для формы с `phone` вместо `phoneNumber`. Проверка: тесты падают на текущем коде

## 3. Реализация

- [x] 3.1 `SellerUpdateRequest`: поле `phone` → `phoneNumber`; в обоих DTO телефон `@NotBlank` + `^(\+7|8)\d{10}$` с текстами из `StoreRequest`; в `SellerRegistrationRequest` email `@NotBlank(message = "Email is required.")` (D1–D3). Проверка: `mvn test -Dtest=SellerRequestValidationTest` проходит
- [x] 3.2 `SellerApplicationServiceImpl`: `PhoneNormalizer.normalize` для phoneNumber при регистрации и UPDATE; в UPDATE email Seller, если email не передан или пуст (D2, D3). Проверка: `mvn test -Dtest=SellerApplicationServiceImplTest,ModeratorServiceImplTest,GlobalExceptionHandlerTest` проходит

## 4. Проверка

- [x] 4.1 Полный прогон тестов seller-service: `mvn test` зеленый. Проверка: вывод `mvn test`
- [x] 4.2 Изменения ограничены DTO, `SellerApplicationServiceImpl` и тестами seller-service. Проверка: `git diff --stat`, `git status`
- [x] 4.3 `openspec validate seller-application-contact-data --strict` проходит. Archive — только после archive `notify-moderator-on-update-deletion-applications` (D4); если тот остается открытым, сначала убрать `Seller update application submission` из его delta. После archive в `seller-onboarding` отметить OBS-01, OBS-07 и OBS-12 устраненными и обновить OQ-01. Проверка: `openspec validate seller-onboarding --type spec --strict` проходит

## 5. Ручные проверки на стенде

Выполняются отдельно по запросу пользователя, в рамках apply не запускаются.

- [ ] 5.1 Пересобрать и перезапустить seller-service. Через api-gateway с токеном: регистрация без email → 400; регистрация с `89991234567` → 201 и `+79991234567` в ответе; UPDATE с `phoneNumber` и без email → 200, в заявке телефон и email Seller. Проверка: ответы записаны в отчет
- [ ] 5.2 После одобрения UPDATE в mailpit приходит письмо продавцу на email Seller, в `seller-events-topic.DLT` новых сообщений нет (нужны Kafka и notification-service). Проверка: скриншот или вывод kafka-ui и mailpit в отчете
