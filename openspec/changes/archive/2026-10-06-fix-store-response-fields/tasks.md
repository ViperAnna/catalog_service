# Tasks

## 1. Тесты на текущее поведение маппинга

- [x] 1.1 Создать `seller-service/src/test/java/ru/seller_service/mapper/StoreMapperTest` (JUnit 5, реальный сгенерированный `StoreMapperImpl`) с тестами `Store response format`: `toDTO` для магазина с продавцом id 7 и телефоном "+79990000000" дает `sellerId = 7` и `phoneNumber = "+79990000000"`. Проверка: `mvn test -Dtest=StoreMapperTest` в seller-service падает на текущем коде (поле `phoneNumber` в DTO отсутствует, `sellerId` равен null)

## 2. Исправление ответа

- [x] 2.1 В `seller-service/src/main/java/ru/seller_service/dto/responce/StoreResponse.java` переименовать поле `phone` в `phoneNumber`. Проверка: `mvn -q compile` в seller-service проходит; `grep -n "phone;" StoreResponse.java` находит только `phoneNumber`
- [x] 2.2 В `StoreMapper.toDTO` добавить `@Mapping(source = "seller.id", target = "sellerId")`. Проверка: в пересобранном `target/generated-sources/.../StoreMapperImpl.toDTO` есть заполнение `sellerId` и `phoneNumber`; `mvn test -Dtest=StoreMapperTest` проходит
- [x] 2.3 Добавить тест: магазин без продавца (`seller = null`) дает `sellerId = null` без исключения, а остальные поля маппятся. Проверка: `mvn test -Dtest=StoreMapperTest` проходит

## 3. Проверка

- [x] 3.1 Полный прогон тестов seller-service. Проверка: `mvn test` в seller-service зеленый, тесты OBS-03-change (`SellerApplicationServiceImplTest`) тоже проходят
- [x] 3.2 Убедиться, что изменились только `StoreResponse`, `StoreMapper` и тесты в seller-service, а catalog-service и frontend не тронуты. Проверка: `git diff --stat` и `git status` по этим файлам
- [x] 3.3 Проверка change: `openspec validate fix-store-response-fields --strict` проходит. После archive в `openspec/specs/seller-stores/spec.md` отметить OBS-01 и OBS-02 как устраненные этим change, сохранив идентификаторы, и убрать их из строки `**Observation**` requirement `Store response format`. Проверка: `openspec validate seller-stores --type spec --strict` проходит
