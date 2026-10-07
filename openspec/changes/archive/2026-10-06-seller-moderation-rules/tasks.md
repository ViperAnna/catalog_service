# Tasks

## 1. Тесты (до изменения кода)

- [x] 1.1 Дополнить `ModeratorServiceImplTest` (design.md, D5): мок `CurrentUserService`; approve и reject своей заявки дают `ResourceForbiddenException` с `Moderator can't review own application.`, без `saveAndFlush`, gRPC и outbox; своя REJECTED-заявка дает 403, а не 409; reject сохраняет moderatorComment и статус REJECTED; существующие тесты проходят с чужим `sub`. Проверка: тесты не компилируются или падают на текущем коде
- [x] 1.2 Добавить `ReviewSellerApplicationRequestValidationTest`: moderatorComment null, `""` и `"   "` дают `Rejection reason is required.`; 500 символов проходят, 501 — нет. Проверка: тесты падают на текущем коде
- [x] 1.3 Дополнить `GlobalExceptionHandlerTest`: `ResourceForbiddenException` → 403 с `{message, time}`; reject без moderatorComment → 400 `Rejection reason is required.`, без вызова сервиса; reject с `{"status":"APPROVED","moderatorComment":"..."}` и без status → 200, сервис вызван; тест нечитаемого тела перевести с `{"status":"UNKNOWN"}` на обрезанный JSON. Проверка: тесты не компилируются или падают на текущем коде

## 2. Реализация

- [x] 2.1 Добавить `ResourceForbiddenException`, обработку 403 в `GlobalExceptionHandler` и ключ `OWN_APPLICATION_REVIEW_FORBIDDEN` в `MessageKeys` (D2). Проверка: `mvn test -Dtest=GlobalExceptionHandlerTest` проходит
- [x] 2.2 `ReviewSellerApplicationRequest`: удалить поле status (D4), `@NotBlank(message = "Rejection reason is required.")` на moderatorComment (D3). Проверка: `mvn test -Dtest=ReviewSellerApplicationRequestValidationTest` проходит
- [x] 2.3 `ModeratorServiceImpl`: внедрить `CurrentUserService`, проверка своей заявки в `approve` и `reject` после поиска и до `validatePending` (D1). Проверка: `mvn test -Dtest=ModeratorServiceImplTest` проходит

## 3. Проверка

- [x] 3.1 Полный прогон тестов seller-service: `mvn test` зеленый. Проверка: вывод `mvn test`
- [x] 3.2 Изменения ограничены seller-service (`ModeratorServiceImpl`, DTO, исключение, обработчик, `MessageKeys`, тесты). Проверка: `git diff --stat`, `git status`
- [x] 3.3 `openspec validate seller-moderation-rules --strict` проходит. После archive в `seller-onboarding` отметить OBS-06, OBS-10 и OBS-11 устраненными, добавить OBS-16 (колонки `moderator_comment` и `request_reason` `varchar(255)` при лимите API 500) и обновить OQ-01. Проверка: `openspec validate seller-onboarding --type spec --strict` проходит

## 4. Ручные проверки на стенде

Выполняются отдельно по запросу пользователя, в рамках apply не запускаются.

- [ ] 4.1 Пересобрать seller-service. Через api-gateway: модератор подает заявку на регистрацию от своего аккаунта и пытается ее одобрить и отклонить → 403 `{message, time}`; другой модератор отклоняет без причины → 400, с причиной и без status → 200 (заявка REJECTED), в mailpit письмо с причиной (нужны Kafka и notification-service). Проверка: ответы и письмо в отчете
