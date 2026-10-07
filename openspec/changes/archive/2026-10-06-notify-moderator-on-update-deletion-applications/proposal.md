# Proposal

## Why

Сейчас событие `SELLER_APPLICATION_CREATED` создается только при подаче заявки
REGISTRATION. Заявки UPDATE и DELETION попадают в очередь модерации, но модератор
о них не узнает: уведомление не приходит, и заявку можно найти, только если
самому открыть `GET /moderator/seller-applications/pending`. Это расхождение
зафиксировано в baseline `seller-onboarding` как OBS-03.

## What Changes

- При успешной подаче заявки UPDATE (`PUT /seller-applications/update`)
  seller-service в той же транзакции записывает в outbox событие
  `SELLER_APPLICATION_CREATED`.
- При успешной подаче заявки DELETION (`DELETE /seller-applications/deletion`)
  seller-service в той же транзакции записывает в outbox событие
  `SELLER_APPLICATION_CREATED`.
- Тип события, конверт сообщения в `seller-events-topic` и payload
  `{applicationId, keycloakUserId, sellerName, email}` остаются прежними.
  Значения берутся из созданной заявки. Для UPDATE email берется из запроса и
  может быть `null` (OBS-12).
- notification-service не меняется. Он уже отправляет письмо модератору на
  каждое `SELLER_APPLICATION_CREATED` и поэтому начнет уведомлять о заявках
  UPDATE и DELETION.
- Поведение для REGISTRATION не меняется.
- Не BREAKING: новых типов событий и полей нет, контракт топика тот же.

### Out of scope

- Тип Kafka-события и payload `SELLER_APPLICATION_CREATED` в этом исправлении
  OBS-03 не меняются.
- В `SELLER_APPLICATION_CREATED` не добавляется `applicationType`
  (`REGISTRATION`, `UPDATE`, `DELETION`), и отдельные типы событий для UPDATE и
  DELETION не вводятся.
- Поэтому письмо модератору одинаково для всех типов заявок
  ("New seller application."). Текст письма не меняется.
- Не исправляем другие observations baseline, включая OBS-01 (phone в UPDATE),
  OBS-02 (SELLER_APPROVED для UPDATE и DELETION) и OBS-12 (email = null в UPDATE).

### Future consideration

- Позже можно добавить `applicationType` в payload `SELLER_APPLICATION_CREATED`.
  Тогда notification-service сможет формировать разные письма для разных типов
  заявок. Это не часть текущего исправления OBS-03 и не расширяет его scope.
  Подробнее в design.md, раздел «Future consideration».

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-onboarding`: меняются требования
  - `Seller update application submission`: вместо «событие в outbox SHALL NOT
    записываться» — запись `SELLER_APPLICATION_CREATED`;
  - `Seller deletion application submission`: то же самое;
  - `Outbox event recording`: событие при подаче записывается для заявок всех
    типов, а не только REGISTRATION.

  Требования `Seller event email notifications` и `Seller events message format`
  не меняются: они уже не зависят от типа заявки.

## Impact

- **Код:** только `seller-service`, `SellerApplicationServiceImpl`, методы
  `createUpdateRequest` и `createDeletionRequest`. Используется уже
  существующий там `saveOutbox(...)`. Сущности, схема БД, DTO, REST API и gRPC
  не меняются.
- **Kafka:** в `seller-events-topic` станет больше сообщений
  `SELLER_APPLICATION_CREATED`. Формат прежний, поэтому порядок деплоя сервисов
  не важен.
- **notification-service:** код не меняется. На адрес
  `NOTIFICATION_MODERATOR_EMAIL` станет приходить больше писем.
- **Spec:** после archive обновятся три requirement в
  `openspec/specs/seller-onboarding/spec.md`. Секции Observations и Open
  Questions archive не обновляет, поэтому OBS-03 и связанные с ним строки
  `**Observation**:` нужно поправить отдельной задачей в tasks.
- **Тесты:** в seller-service тестов сейчас нет. Проверку записи события для
  UPDATE и DELETION нужно добавить в рамках этого change.
