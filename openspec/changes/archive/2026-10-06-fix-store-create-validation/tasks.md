# Tasks

## 1. Тесты валидации (до изменения кода)

- [x] 1.1 Создать `seller-service/src/test/java/ru/seller_service/dto/request/store/StoreRequestValidationTest` (JUnit 5, Jakarta `Validator` из `Validation.buildDefaultValidatorFactory()`): для `StoreRequest` нарушения есть, если `email` или `phoneNumber` отсутствуют, phoneNumber равен `79991234567`, `+7 999 123-45-67`, `abc`, description длиной 256; нарушений нет для `+79991234567`, `89991234567` и description длиной 255. Проверка: `mvn test -Dtest=StoreRequestValidationTest` падает на текущем коде (сейчас допускаются отсутствующие поля, `79991234567` и 256 символов)

## 2. DTO и валидация

- [x] 2.1 В `StoreRequest` добавить `@NotBlank` с `Email is required.` и `Phone is required.`, заменить шаблон телефона на `^(\+7|8)\d{10}$` с `Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX.` и лимит description на `@Size(max = 255, message = "Description can't exceed 255 characters.")` (design.md, D1, D3). Проверка: `mvn test -Dtest=StoreRequestValidationTest` проходит
- [x] 2.2 Добавить record `dto/request/store/StoreUpdateRequest` с теми же полями и ограничениями формата, но без `@NotBlank` на email и phoneNumber (`storeName` остается `@NotBlank @Size(max=100)`); перевести на него `StoreController.updateStore`, `StoreService.updateStore` и `StoreMapper.updateFromDTO`. Добавить в `StoreRequestValidationTest` проверки `StoreUpdateRequest`: без email и phoneNumber нарушений нет, `79991234567` и description длиной 256 дают нарушения. Проверка: `mvn test -Dtest=StoreRequestValidationTest` проходит

## 3. Нормализация телефона

- [x] 3.1 Добавить статическую утилиту нормализации телефона в seller-service (design.md, D2): `8XXXXXXXXXX` в `+7XXXXXXXXXX`, остальное без изменений, null — null. Добавить unit-тест утилиты. Проверка: тест проходит
- [x] 3.2 В `StoreServiceImpl` нормализовать `store.phoneNumber` после `toEntity` в `createStore` и после `updateFromDTO` в `updateStore`, если телефон передан. Добавить `StoreServiceImplTest` (Mockito, реальный `StoreMapperImpl`): создание с `89991234567` сохраняет `+79991234567`; изменение с `89990001122` сохраняет `+79990001122`; изменение без телефона и email оставляет прежние значения. Проверка: `mvn test -Dtest=StoreServiceImplTest` проходит

## 4. Проверка

- [x] 4.1 Полный прогон тестов seller-service: `mvn test` зеленый, включая `SellerApplicationServiceImplTest` и `StoreMapperTest`. Проверка: вывод `mvn test`
- [x] 4.2 Проверка данных на текущей (локальной) БД: в `stores` нет телефонов, не соответствующих `^(\+7|8)[0-9]{10}$`, и description длиннее 255. Проверка: результат запроса записан в отчет к задаче. При деплое на production шаг нужно повторить для production БД
- [x] 4.3 Убедиться, что изменения ограничены seller-service (`dto/request/store`, `StoreController`, `StoreService`, `StoreServiceImpl`, `StoreMapper`, утилита и тесты), схема БД, catalog-service и frontend не тронуты. Проверка: `git diff --stat` и `git status`
- [x] 4.4 Проверка change: `openspec validate fix-store-create-validation --strict` проходит. После archive в `openspec/specs/seller-stores/spec.md` отметить OBS-03 и OBS-04 как устраненные этим change, сохранив идентификаторы, и обновить OQ-01. Проверка: `openspec validate seller-stores --type spec --strict` проходит
