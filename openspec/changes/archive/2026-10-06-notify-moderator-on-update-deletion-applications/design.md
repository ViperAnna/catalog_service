# Design

## Context

Мотивация описана в proposal.md, раздел Why. Целевое поведение задано в
specs/seller-onboarding/spec.md.

Цепочка уведомления модератора уже работает для REGISTRATION:

```
seller-service                                   notification-service
  подача REGISTRATION
    save(application)
    saveOutbox(SELLER_APPLICATION_CREATED) --+
                                             |
  OutboxPublisher (3s) --> seller-events-topic --> SellerEventConsumer
                                             switch(eventType)
                                               SELLER_APPLICATION_CREATED
                                                 -> письмо на NOTIFICATION_MODERATOR_EMAIL
```

Подача UPDATE и DELETION сохраняет заявку, но outbox-запись не создает, поэтому
цепочка для этих типов не запускается. Consumer, конверт сообщения и формат
payload от типа заявки не зависят.

## Goals / Non-Goals

**Goals:**
- Подача UPDATE и DELETION создает ту же outbox-запись, что и REGISTRATION, в
  той же транзакции, что и сохранение заявки.
- notification-service не меняется и не требует согласованного деплоя.

**Non-Goals:**
- Менять контракт `seller-events-topic` и текст писем (см. proposal.md,
  Out of scope).
- Менять порядок проверок, коды ответов и данные, которые сохраняются в заявке.
- Исправлять OBS-01 и OBS-12, даже если они влияют на содержимое события.

## Decisions

### D1. Переиспользовать `SELLER_APPLICATION_CREATED` с текущим payload

UPDATE и DELETION публикуют то же событие и тот же набор полей
`{applicationId, keycloakUserId, sellerName, email}`, что и REGISTRATION.

Альтернативы:
- *Отдельные типы событий для UPDATE и DELETION.* Отклонено. notification-service
  десериализует `eventType` в свой enum, поэтому новое значение у старой версии
  consumer не распознается и уйдет в DLT после ретраев. Потребовался бы
  согласованный порядок деплоя и изменение consumer. Это вне scope.
- *Добавить `applicationType` в payload сейчас.* Отложено, см. Future
  consideration. Без изменения notification-service это поле ничего не дает.

### D2. Записывать событие в тех же методах подачи, через существующий outbox-хелпер

Событие записывается сразу после сохранения заявки в методах подачи UPDATE и
DELETION. Используется уже существующий в сервисе заявок метод записи outbox и
тот же билдер события, что и для REGISTRATION. Методы подачи уже выполняются в
одной транзакции (`@Transactional` на классе), поэтому заявка и событие
фиксируются атомарно.

Значения полей берутся из созданной заявки, а не из Seller, как и для
REGISTRATION:
- UPDATE: sellerName берется из Seller (его копирует подача), email берется из
  запроса и может быть null (OBS-12).
- DELETION: sellerName и email берутся из снимка данных Seller.

`applicationId` доступен сразу после `save`, потому что используется
`GenerationType.IDENTITY`.

Альтернатива: доменное событие и `@TransactionalEventListener`. Отклонено как
новый паттерн ради двух вызовов, не соответствующий текущему коду.

### D3. Обновить non-normative секции baseline вручную после archive

Archive переносит только блоки requirements. Запись OBS-03 в секции
Observations и упоминание в OQ-01 (`OBS-01–OBS-12`) он не трогает. Строки
`**Observation**:` внутри MODIFIED-блоков уже исправлены в delta. Запись OBS-03
нужно отметить как устраненную этим change, сохранив идентификатор, чтобы
ссылки не сдвинулись. Новый OBS не создается.

## Risks / Trade-offs

- [У модератора станет больше писем, и они неразличимы по типу] → Принято
  осознанно (proposal.md, Out of scope). Если это станет мешать, поможет
  будущее улучшение.
- [В письме модератору для UPDATE без email будет "Email: null"] → Принято:
  это следствие OBS-12, адресат письма корректен. Исправляется отдельным change.
- [Накопленные необработанные записи outbox или повторная доставка дадут
  дубликаты писем (ASM-03)] → Поведение не новое, просто распространяется на
  новые типы заявок.
- [Заявки UPDATE и DELETION, созданные до деплоя, уведомления не получат] →
  Ретроспективную генерацию событий не делаем: такие заявки видны в
  `GET /moderator/seller-applications/pending`.

## Migration Plan

1. Задеплоить seller-service. notification-service и схема БД не меняются.
2. Откат: вернуть предыдущий образ seller-service. События, которые уже
   записаны в outbox, допубликуются старой версией, а consumer их обработает:
   формат тот же.

## Future consideration

Добавить в payload `SELLER_APPLICATION_CREATED` поле `applicationType`
(`REGISTRATION` | `UPDATE` | `DELETION`). Тогда notification-service сможет
формировать для модератора разные письма по типу заявки. Наброски для будущего
change:
- изменение аддитивное. notification-service использует ObjectMapper Spring
  Boot, который неизвестные поля игнорирует, поэтому seller-service можно
  деплоить первым;
- потребуются изменения requirements `Outbox event recording`,
  `Seller events message format` и `Seller event email notifications`.

Это не входит в текущее исправление OBS-03.
