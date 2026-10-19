# Proposal

## Why

Правила модерации заявок продавца неполные (`seller-onboarding` OBS-06, OBS-10, OBS-11):
- в теле `reject` поле status обязательно, но ни на что не влияет: статус
  заявки после отклонения всегда REJECTED, его выставляет сервер. Клиента
  заставляют передавать бессмысленное поле, а без него он получает 400 (OBS-06);
- модератор может одобрить или отклонить собственную заявку, то есть сам
  сделать себя продавцом (OBS-10);
- moderatorComment при отклонении необязателен. Продавец не узнает причину
  отказа, а письмо содержит "Reason: null" (OBS-11).

Решения пользователя:
- статус заявки (PENDING, APPROVED, REJECTED) остается и по-прежнему
  выставляется сервером по решению модератора: `/approve` → APPROVED,
  `/reject` → REJECTED. Поле status убирается из тела `reject`;
- рассмотрение своей заявки запрещено, ответ 403;
- причина отказа обязательна. Позже модератор будет выбирать причину из
  списка или писать свою, обе формы попадут в то же поле moderatorComment.

Это change G2 из разбора OBS `seller-onboarding`.

## What Changes

- **Тело `reject` — `{moderatorComment}`:** поле status удалено из
  `ReviewSellerApplicationRequest`. Если клиент его передает, Jackson его
  игнорирует (в Spring Boot `FAIL_ON_UNKNOWN_PROPERTIES=false`), и заявка
  все равно становится REJECTED. Статус заявки и правило «рассматривается
  только PENDING, иначе 409» не меняются.
- **Обязательная причина отказа:** moderatorComment обязателен и не может быть
  пустым, иначе 400 `Validation failed: Rejection reason is required.`
- **Запрет самомодерации:** `approve` и `reject` заявки, у которой
  keycloakUserId совпадает с `sub` модератора, отвечают 403 с телом
  `{message, time}` и message `Moderator can't review own application.`
  Заявка, Seller, роль в Keycloak и outbox не меняются. Просмотр своей заявки
  (`GET /moderator/seller-applications/{id}`, `/pending`) не ограничивается.
- **Порядок проверок:** тело невалидно → 400, заявка не найдена → 404, своя
  заявка → 403, заявка не PENDING → 409.
- Совместимость: запрос со status по-прежнему принимается. Запрос без status
  теперь дает 200 вместо 400. Запрос без moderatorComment теперь дает 400.
  Известных клиентов нет: frontend модерацию не вызывает.

### Out of scope

- Список предопределенных причин отказа (справочник, API, frontend). Поле
  moderatorComment остается свободным текстом, будущий выбор из списка будет
  заполнять его же.
- Единый эндпоинт рассмотрения, где решение передается в теле. approve и
  reject остаются отдельными эндпоинтами.
- Длина колонок `moderator_comment` и `request_reason` (`varchar(255)`) при
  лимите API 500 символов — новое наблюдение OBS-16, отдельный change.
- Запрет для администратора или другой ролевой модели модерации.

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-onboarding`: добавляется требование `Self-moderation prohibition`;
  изменяются `Application rejection` и `Seller service error response format`
  (пример нечитаемого тела больше не использует status).

## Impact

- **seller-service:** `ReviewSellerApplicationRequest` (удаление status,
  `@NotBlank` на moderatorComment), `ModeratorServiceImpl` (проверка `sub`,
  зависимость `CurrentUserService`), новое `ResourceForbiddenException` и его
  обработка в `GlobalExceptionHandler`, `MessageKeys`, тесты.
- **notification-service, Kafka, Keycloak, frontend, БД:** без изменений.
  Статус заявки в БД и событие SELLER_REJECTED не меняются.
