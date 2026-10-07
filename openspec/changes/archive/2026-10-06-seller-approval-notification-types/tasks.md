# Tasks

## 1. Тесты (до изменения кода)

- [x] 1.1 seller-service `ModeratorServiceImplTest`: payload SELLER_APPROVED содержит `applicationType` = `REGISTRATION` при одобрении регистрации и `UPDATE` при одобрении изменения (design.md, D5). Проверка: тесты падают на текущем коде
- [x] 1.2 notification-service `EmailServiceTest`: тема и текст письма на SELLER_APPROVED для REGISTRATION, UPDATE, DELETION, null и пустого значения (таблица D3); письмо DELETION не содержит "Your shop can now be used"; адресат — email из события. Неизвестное значение из fallback-теста убрать. Проверка: `mvn test -DskipTests=false -Dtest=EmailServiceTest` проходит после 2.2
- [x] 1.3 notification-service `SellerApprovedEventTest`: payload без `applicationType` и с дополнительным неизвестным полем разбирается Spring Boot `ObjectMapper` без исключения; с `"applicationType":"DELETION"` поле заполняется. Проверка: тест не компилируется на текущем коде
- [x] 1.4 notification-service `SellerEventConsumerTest`: SELLER_APPROVED с `"FOO"`, `"update"` и `"DELETION "` дает `UnknownApplicationTypeException`, `SellerNotificationService` не вызывается; null, пустое, пробелы и три известных типа передаются в `notifySellerApproved` (D2). Проверка: тест не компилируется на текущем коде
- [x] 1.5 notification-service `KafkaConfigTest`: `DefaultErrorHandler` из `KafkaConfig` с моком recoverer на `ListenerExecutionFailedException` с причиной `UnknownApplicationTypeException` восстанавливает сообщение с первой попытки, а на `RuntimeException` — нет (D4). Проверка: тест не компилируется или падает на текущем коде

## 2. Реализация

- [x] 2.1 seller-service: поле `SellerApplicationType applicationType` в `SellerApprovedEvent`, заполнение в `ModeratorServiceImpl.approve` (D1). Проверка: `mvn test -Dtest=ModeratorServiceImplTest` проходит
- [x] 2.2 notification-service: поле `String applicationType` в `SellerApprovedEvent`, выбор письма в `EmailService.sendSellerApprovedEmail` для известных типов, null и пустого значения (D2, D3). Проверка: `mvn test -DskipTests=false -Dtest=EmailServiceTest,SellerApprovedEventTest` проходит
- [x] 2.3 notification-service: `UnknownApplicationTypeException`, проверка типа в `SellerEventConsumer` (D2), `addNotRetryableExceptions` в `KafkaConfig` (D4). Проверка: `mvn test -DskipTests=false -Dtest=SellerEventConsumerTest,KafkaConfigTest` проходит

## 3. Проверка

- [x] 3.1 Полный прогон тестов seller-service (`mvn test`) зеленый; в notification-service проходят все новые тесты (`contextLoads` требует Kafka и БД и не запускается). Проверка: вывод `mvn`
- [x] 3.2 Изменения ограничены `SellerApprovedEvent` и `ModeratorServiceImpl` в seller-service, `SellerApprovedEvent`, `UnknownApplicationTypeException`, `SellerEventConsumer`, `KafkaConfig` и `EmailService` в notification-service и тестами; `skipTests` в `notification-service/pom.xml` не меняется. Проверка: `git diff --stat`, `git status`
- [x] 3.3 `openspec validate seller-approval-notification-types --strict` проходит. После archive в `seller-onboarding` отметить OBS-02 устраненным и обновить OQ-01; в `integration-events` в Event Catalog добавить `applicationType` в payload SELLER_APPROVED и неизвестный тип → DLT без повторов. Проверка: `openspec validate seller-onboarding --type spec --strict` и `openspec validate integration-events --type spec --strict` проходят

## 4. Ручные проверки на стенде

Выполняются отдельно по запросу пользователя, в рамках apply не запускаются.

- [ ] 4.1 Пересобрать seller-service и notification-service, запустить Kafka. Одобрить заявки UPDATE и DELETION. Проверка: в kafka-ui payload SELLER_APPROVED содержит `applicationType`, в mailpit письма "Seller data change approved" и "Seller account deletion approved", в `seller-events-topic.DLT` новых сообщений нет
- [ ] 4.2 Опубликовать в `seller-events-topic` через kafka-ui SELLER_APPROVED с `"applicationType":"FOO"`. Проверка: в mailpit письма нет, в логе notification-service ошибка с applicationId и `FOO`, сообщение одно (без повторов) появилось в `seller-events-topic.DLT`
