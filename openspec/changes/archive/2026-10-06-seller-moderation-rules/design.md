# Design

## Context

Мотивация описана в proposal.md, требования — в delta spec
`specs/seller-onboarding/spec.md`.

Текущее состояние:
- `ModeratorController` защищен `@PreAuthorize("hasRole('MODERATOR')")`, но
  `ModeratorServiceImpl` не знает, кто модератор: `CurrentUserService` в него
  не внедрен. `CurrentUserServiceImpl.getUserId()` возвращает `sub` JWT.
- `ReviewSellerApplicationRequest(status @NotNull, moderatorComment @Size(max=500))`.
  `ModeratorServiceImpl.reject` не читает `status`: статус заявки всегда
  становится REJECTED.
- `GlobalExceptionHandler` (change `seller-service-error-handling`)
  пробрасывает `AccessDeniedException` в Spring Security, поэтому 403 от
  него приходит без тела `{message, time}`.
- Колонка `seller_applications.moderator_comment` — `varchar(255)` при лимите
  API 500 (проверено на стенде). Не исправляется этим change (OBS-16).

## Goals / Non-Goals

**Goals:**
- Модератор не может рассмотреть свою заявку, ответ 403 с телом `{message, time}`.
- Тело reject содержит только то, что влияет на результат.
- Причина отказа всегда заполнена.

**Non-Goals:**
- Справочник причин, единый эндпоинт рассмотрения, длина колонок (OBS-16).

## Decisions

### D1. Проверка в сервисе по `sub`
`ModeratorServiceImpl` получает `CurrentUserService`. В `approve` и `reject`
после `getApplicationById` (404) вызывается
`validateNotOwnApplication(application)`: если
`application.getKeycloakUserId().equals(currentUserService.getUserId())`,
выбрасывается `ResourceForbiddenException(MessageKeys.OWN_APPLICATION_REVIEW_FORBIDDEN)`.
Затем `validatePending` (409). Так своя заявка дает 403 в любом статусе, и
модератор не узнает лишнего из 409.

Альтернатива — проверка в контроллере или `@PreAuthorize` с SpEL. Отклонена:
нужен уже загруженный объект заявки, а бизнес-правила в сервисе здесь общие
для всех проверок.

### D2. `ResourceForbiddenException` вместо `AccessDeniedException`
Новое исключение `exception/ResourceForbiddenException extends ClientException`
с `@ResponseStatus(FORBIDDEN)` по образцу двух существующих.
`GlobalExceptionHandler` отвечает на него 403 с `{message, time}`.
`AccessDeniedException` не используется, потому что обработчик намеренно
пробрасывает его в Spring Security, и тело было бы пустым.

Текст: `MessageKeys.OWN_APPLICATION_REVIEW_FORBIDDEN = "Moderator can't review own application."`

### D3. Обязательная причина
`ReviewSellerApplicationRequest.moderatorComment`:
`@NotBlank(message = "Rejection reason is required.")` и прежний
`@Size(max = 500)`. Поле и его имя не меняются, поэтому будущий выбор из
списка причин сможет передавать выбранный текст в то же поле без изменения
API.

### D4. Удаление status из тела reject
Поле `status` удаляется из `ReviewSellerApplicationRequest`, record остается с
одним полем `moderatorComment`. `ModeratorServiceImpl.reject` и раньше не
читал `request.status()`, поэтому логика сервиса не меняется: статус заявки
выставляется сервером (REJECTED), а `validatePending` по-прежнему дает 409
для уже рассмотренной заявки.

Совместимость: в seller-service нет своей настройки Jackson, поэтому действует
значение Spring Boot `FAIL_ON_UNKNOWN_PROPERTIES=false`. Тело со старым полем
`status` (с любым значением, включая неизвестное) принимается, поле
игнорируется. Сценарий «Неизвестное значение enum» требования
`Seller service error response format` опирался на `{"status": "UNKNOWN"}`,
поэтому его пример меняется на обрезанный JSON. Название сценария сохраняется,
потому что archive не позволяет удалить сценарий из MODIFIED-блока.

Альтернатива — оставить status необязательным и игнорируемым. Отклонена:
поле осталось бы в контракте, хотя ни на что не влияет.

### D5. Тесты
- `ModeratorServiceImplTest`: approve и reject своей заявки дают
  `ResourceForbiddenException`, заявка, Seller, gRPC и outbox не затронуты;
  своя REJECTED-заявка дает 403, а не 409; чужая заявка одобряется; reject
  сохраняет moderatorComment и статус REJECTED. Существующие тесты получают
  мок `CurrentUserService` с другим `sub`.
- `ReviewSellerApplicationRequestValidationTest` (новый): null, пустая строка
  и пробелы дают `Rejection reason is required.`; 500 символов проходят, 501 —
  нет.
- `GlobalExceptionHandlerTest`: `ResourceForbiddenException` → 403 с
  `{message, time}`; reject без moderatorComment → 400 без вызова сервиса;
  reject с `{"status":"APPROVED","moderatorComment":"..."}` и с телом без
  status → 200, сервис вызван; тест нечитаемого тела переводится на обрезанный JSON.

## Risks / Trade-offs

- [Клиенты, отклоняющие без комментария] → Известных нет, frontend модерацию
  не вызывает.
- [Модератор — единственный, кто может рассмотреть заявку] → Нужен второй
  модератор. Это и есть цель запрета.
- [Комментарий 256–500 символов] → По-прежнему 500 из-за колонки (OBS-16),
  не меняется этим change.

## Migration Plan

Деплой seller-service. Схема БД не меняется. Откат — прежний образ.
