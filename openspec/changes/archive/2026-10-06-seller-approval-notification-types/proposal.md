# Proposal

## Why

seller-service записывает SELLER_APPROVED при одобрении заявки любого типа
(REGISTRATION, UPDATE, DELETION), но тип заявки в событие не попадает.
notification-service на каждое SELLER_APPROVED отправляет продавцу одно и то
же письмо "Seller application approved! ... Your shop can now be used as a
seller shop", в том числе когда одобрено удаление аккаунта продавца
(`seller-onboarding` OBS-02).

Решения пользователя: передавать тип заявки полем `applicationType` в payload
SELLER_APPROVED, типы событий не менять; учитывать тип только в письме об
одобрении. Это change G3 из разбора OBS `seller-onboarding`.

## What Changes

- **seller-service:** payload SELLER_APPROVED получает поле `applicationType`
  со значением `REGISTRATION`, `UPDATE` или `DELETION` (тип одобренной
  заявки). Остальные поля, eventType, ключ и конверт сообщения не меняются.
- **notification-service:** письмо продавцу на SELLER_APPROVED зависит от
  `applicationType`:
  - `REGISTRATION` — прежнее письмо "Seller application approved!";
  - `UPDATE` — "Seller data change approved": одобрено изменение данных
    продавца;
  - `DELETION` — "Seller account deletion approved": одобрено удаление
    аккаунта продавца;
  - поле отсутствует или пустое — прежнее письмо о регистрации (события,
    записанные до деплоя, тип не содержат);
  - неизвестное непустое значение — письмо не отправляется, ошибка
    логируется, сообщение сразу уходит в `seller-events-topic.DLT` без
    повторов (`UnknownApplicationTypeException` помечено в
    `DefaultErrorHandler` как неповторяемое). Неверное письмо пользователю
    так не уйдет, а сообщение можно будет переотправить из DLT после
    обновления notification-service.
- **Совместимость:** изменение аддитивное. Старая версия notification-service
  игнорирует новое поле (в Spring Boot `FAIL_ON_UNKNOWN_PROPERTIES=false`), а
  новая версия обрабатывает старые события без поля. Сервисы можно деплоить в
  любом порядке.

### Out of scope

- Тип заявки в SELLER_APPLICATION_CREATED (письмо модератору) и в
  SELLER_REJECTED (письмо об отказе) — тексты этих писем не меняются.
- Новые типы событий, шаблоны писем (HTML, локализация), Telegram.
- Публикация отложенных событий (seller-service и Kafka на стенде).
- Инструмент переотправки сообщений из DLT (ручная операция).

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-onboarding`: изменяются требования `Outbox event recording`,
  `Seller event email notifications` и `Seller events consumer error handling`.
- `integration-events`: изменяются требования `seller-events-topic message contract`
  и `Consumer error handling and DLT`.

## Impact

- **seller-service:** `event/SellerApprovedEvent` (поле `applicationType`),
  `ModeratorServiceImpl.approve` (заполнение поля), тесты.
- **notification-service:** `event/SellerApprovedEvent` (поле
  `applicationType`), новое `UnknownApplicationTypeException`, проверка
  типа в `SellerEventConsumer`, `KafkaConfig` (неповторяемое исключение),
  `EmailService.sendSellerApprovedEmail` (выбор письма), unit-тесты.
- **Kafka-контракт:** новое необязательное поле в payload SELLER_APPROVED.
  Другие consumer этого топика не существуют.
- **frontend, Keycloak, БД:** без изменений.
