# Design

## Context

Мотивация описана в proposal.md, требования — в delta specs
`specs/seller-onboarding/spec.md` и `specs/integration-events/spec.md`.

Текущее состояние:
- seller-service: `ModeratorServiceImpl.approve` после действий по типу
  заявки строит `SellerApprovedEvent{applicationId, keycloakUserId,
  sellerName, email}` и пишет его в outbox. Тип заявки известен
  (`application.getSellerApplicationType()`), но в событие не попадает.
- notification-service: `SellerEventConsumer` разбирает конверт в
  `KafkaEvent` с `eventType` типа enum `SellerEventType`, payload — в
  `SellerApprovedEvent` через `EventMapper` на Spring Boot `ObjectMapper`.
  Своей настройки Jackson нет, поэтому неизвестные поля игнорируются.
  `EmailService.sendSellerApprovedEmail` отправляет одно письмо на все типы.
- Ошибка разбора сообщения ведет к 3 повторам и DLT: бин
  `DefaultErrorHandler(new DeadLetterPublishingRecoverer(template), new FixedBackOff(1000L, 3))`
  — единственный `CommonErrorHandler`, Spring Boot подключает его к фабрике
  listener-ов. Топик `.DLT` создается брокером при первой записи
  (`auto.create.topics.enable` не переопределен, ASM-07).
- `MailException` перехватывается в `EmailService` и в DLT не попадает.
- В notification-service тестов, кроме `contextLoads`, нет, а в `pom.xml`
  свойство `<skipTests>true</skipTests>`. Тесты запускаются с
  `-DskipTests=false -Dtest=...`, как в catalog-service.

## Goals / Non-Goals

**Goals:**
- Продавец получает письмо, соответствующее одобренному действию.
- Сервисы можно деплоить в любом порядке, старые события не уходят в DLT.

**Non-Goals:**
- Изменение писем о новой заявке и об отказе, новые eventType.

## Decisions

### D1. `applicationType` в payload SELLER_APPROVED
seller-service: в `SellerApprovedEvent` добавляется поле
`SellerApplicationType applicationType`, Jackson пишет его как имя enum
(`"REGISTRATION"`, `"UPDATE"`, `"DELETION"`). `ModeratorServiceImpl.approve`
заполняет его из заявки. Другие события не меняются.

### D2. notification-service: поле как строка, проверка в consumer
В `SellerApprovedEvent` notification-service поле объявляется как `String`,
а не enum, чтобы разбор payload не падал на любом значении и решение
принималось явно.

`SellerEventConsumer` в ветке SELLER_APPROVED перед вызовом
`SellerNotificationService` проверяет тип:
- null, пустая строка или только пробелы — допустимо (старые события);
- `REGISTRATION`, `UPDATE`, `DELETION` (точное совпадение, регистр важен:
  seller-service пишет имена enum) — допустимо;
- иное — `log.error` с applicationId и значением, затем
  `throw new UnknownApplicationTypeException(applicationId, applicationType)`.
  Письмо не отправляется.

Проверка стоит в consumer, а не в `EmailService`: «не отправлять и отдать в
DLT» — решение обработки сообщения, а `EmailService` только собирает и
отправляет письмо.

### D3. Выбор письма
`EmailService.sendSellerApprovedEmail` выбирает тему и текст по
`applicationType`:

| applicationType | Тема | Текст |
|---|---|---|
| `UPDATE` | `Seller data change approved` | `Hello, %s.` / `Your request to change seller data has been approved.` / `The new data is now in effect.` |
| `DELETION` | `Seller account deletion approved` | `Hello, %s.` / `Your request to delete the seller account has been approved.` / `Your seller account is now closed.` |
| `REGISTRATION`, null, пусто | `Seller application approved!` | прежний текст без изменений |

Окончание письма `Thank you for using our service!` сохраняется во всех
вариантах. Адресат, отправитель и обработка `MailException` не меняются.
Отдельные методы в `SellerNotificationService` не вводятся: выбор письма —
деталь `EmailService`.

null и пустое значение дают письмо о регистрации, потому что так работает
текущий код, а события UPDATE и DELETION, записанные до деплоя, отличить от
регистрации нельзя. Неизвестное значение до `EmailService` не доходит (D2),
ветка `default` в нем остается только для null и пустого значения.

### D4. Неизвестный тип — сразу в DLT
`KafkaConfig.kafkaErrorHandler`: после создания `DefaultErrorHandler`
вызывается `addNotRetryableExceptions(UnknownApplicationTypeException.class)`.
Spring Kafka проверяет классификацию по цепочке причин, поэтому обертка
`ListenerExecutionFailedException` не мешает. Сообщение публикуется
`DeadLetterPublishingRecoverer` в `seller-events-topic.DLT` (та же партиция,
исходные ключ и значение, причина в заголовках `kafka_dlt-exception-*`) после
первой попытки, без 3 повторов.

Остальные исключения, включая ошибки `user-created-events-topic`, по-прежнему
повторяются 3 раза: классифицируется только этот класс.

Восстановление: после деплоя notification-service, который знает новый тип,
сообщения из DLT переотправляются в `seller-events-topic` вручную (kafka-ui
или скрипт). Инструмент не входит в change.

Альтернативы: пропуск с логом (уведомление теряется без возможности
восстановления), существующие 3 повтора (бессмысленны для детерминированной
ошибки), нейтральное письмо (догадка о смысле события). Отклонены по решению
пользователя.

### D5. Тесты
- seller-service `ModeratorServiceImplTest`: payload SELLER_APPROVED содержит
  `applicationType` = `REGISTRATION` при одобрении регистрации и `UPDATE` при
  одобрении изменения.
- notification-service `EmailServiceTest` (новый, Mockito, мок
  `JavaMailSender`, `ReflectionTestUtils` для `from`): тема и текст для
  REGISTRATION, UPDATE, DELETION, null и пустого значения; письмо DELETION не
  содержит "Your shop can now be used".
- notification-service `SellerEventConsumerTest` (новый): SELLER_APPROVED с
  "FOO", "update" и "DELETION " дает `UnknownApplicationTypeException`, письмо
  не отправляется; null, пустое и пробелы, а также три известных типа
  передаются в `SellerNotificationService`.
- notification-service `KafkaConfigTest` (новый): `DefaultErrorHandler` из
  `KafkaConfig` с моком recoverer (через тот же код конфигурации) на
  `ListenerExecutionFailedException` с причиной
  `UnknownApplicationTypeException` вызывает recoverer с первой попытки
  (`handleOne` возвращает true), а на `RuntimeException` — нет.
- notification-service `SellerApprovedEventTest` (новый, `ObjectMapper` по
  умолчанию Spring Boot): payload без `applicationType` и с неизвестным
  полем разбирается без исключения.

## Risks / Trade-offs

- [События UPDATE и DELETION, записанные до деплоя, по-прежнему дадут письмо
  о регистрации] → Это прежнее поведение. На стенде seller-service на старом
  образе, Kafka остановлена, outbox таких событий не накапливает.
- [Тексты писем на английском, без шаблонов] → Как и остальные письма
  сервиса.
- [DLT без потребителя, сообщения копятся незаметно] → Ошибка пишется в лог
  notification-service; мониторинг DLT вне change.
- [Топик DLT не создан явно] → Автосоздание брокером (ASM-07), как для
  остальных ошибок.

## Migration Plan

1. Деплой seller-service и notification-service в любом порядке.
2. Откат любого из них безопасен: поле необязательное.
