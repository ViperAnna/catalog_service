# Design

## Context

Мотивация описана в proposal.md, требования — в delta spec
`specs/seller-onboarding/spec.md`.

Текущее состояние:
- `SellerUpdateRequest(phone, email, paymentAccount)` привязывается через
  `@Valid @ModelAttribute`. `SellerApplicationMapper.toEntity(SellerUpdateRequest)`
  не маппит `phone` в `phoneNumber` (`unmappedTargetPolicy = IGNORE`), поэтому в
  заявке phoneNumber = null, и `updateFromApplication` его пропускает.
- `SellerRegistrationRequest.email` — только `@Email` и `@Size(max=100)`, при
  этом `Seller.email` `nullable=false`.
- Телефон: `@Pattern("\\+?[0-9]{7,15}")` в обоих DTO. У магазинов —
  `^(\+7|8)\d{10}$` и `util/PhoneNormalizer`.
- `SellerMapper.updateFromDTO(SellerUpdateRequest, Seller)` нигде не
  вызывается. После переименования поля он маппит phoneNumber автоматически,
  поведение не меняется.
- Данные стенда: 2 заявки REGISTRATION и 1 Seller, у всех email заполнен,
  телефоны в формате `+7XXXXXXXXXX`.
- Change `notify-moderator-on-update-deletion-applications` (код в рабочей
  копии, не заархивирован) меняет то же требование
  `Seller update application submission`.

## Goals / Non-Goals

**Goals:**
- Одинаковые имя поля и формат телефона в регистрации, UPDATE и магазинах.
- У каждой новой заявки есть email, поэтому события и письма получают адрес.

**Non-Goals:**
- Миграция старых данных, отдельные письма для UPDATE и DELETION.

## Decisions

### D1. Переименование `phone` → `phoneNumber` в `SellerUpdateRequest`
Имя поля record совпадает с полем заявки, поэтому MapStruct маппит его без
`@Mapping`. Параметр формы — `phoneNumber`. Сообщение `@NotBlank` —
`Phone is required.`, поэтому запрос со старым именем `phone` получает 400
с понятным текстом.

Альтернатива — `@Mapping(source = "phone", target = "phoneNumber")` без смены
API. Отклонена по решению пользователя: имена в регистрации и в UPDATE
разошлись бы.

### D2. Формат телефона как у магазинов
Оба DTO: `@NotBlank(message = "Phone is required.")` и
`@Pattern(regexp = "^(\\+7|8)\\d{10}$", message = "Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX.")`
— те же тексты, что в `StoreRequest`. `SellerApplicationServiceImpl`
применяет `PhoneNormalizer.normalize` к phoneNumber заявки при регистрации и
при UPDATE. Нормализация в сервисе, а не в маппере, — как в `StoreServiceImpl`.

### D3. Email
- Регистрация: `@NotBlank(message = "Email is required.")` плюс прежние
  `@Email` и `@Size`.
- UPDATE: `@Email` и `@Size` без `@NotBlank`. Пустая строка считается
  отсутствием email (`@Email` пропускает пустую строку, поэтому иначе в
  заявку попал бы `""`). В `createUpdateRequest`: если email null или пустой,
  `application.setEmail(seller.getEmail())`.

Отсюда SELLER_APPLICATION_CREATED и SELLER_APPROVED для UPDATE содержат
адрес, а `updateFromApplication` записывает в Seller тот же email (без
изменений).

### D4. Порядок архивации с notify-moderator
Delta `Seller update application submission` этого change содержит событие
SELLER_APPLICATION_CREATED из change notify-moderator и все его сценарии,
кроме измененного «Подача изменения без email». Правильный порядок:
1. archive `notify-moderator-on-update-deletion-applications`;
2. archive этого change. Его MODIFIED-блок заменит версию notify-moderator.

Если notify-moderator понадобится архивировать позже, перед его архивацией
нужно убрать `Seller update application submission` из его delta (tasks 4.3).

### D5. Тесты
- `SellerRequestValidationTest` (новый): валидатор Jakarta на обоих DTO —
  формы телефона, обязательный email при регистрации, необязательный в UPDATE.
- `SellerApplicationServiceImplTest`: нормализация телефона при регистрации и
  UPDATE; UPDATE без email и с пустым email берет email Seller, и он же в
  событии; phoneNumber переносится в заявку.
- `ModeratorServiceImplTest`: одобрение UPDATE переносит телефон и дает
  SELLER_APPROVED с email Seller.
- `GlobalExceptionHandlerTest`: форма UPDATE с `phone` вместо `phoneNumber`
  дает 400; существующий тест переводится на `phoneNumber`.

## Risks / Trade-offs

- [Клиенты, отправляющие `phone` или телефоны в другом формате] → Известных
  нет. Изменение отмечено как BREAKING.
- [Старые PENDING-заявки без email] → На стенде их нет. На production перед
  деплоем проверить (tasks 1.1), при наличии отклонить или исправить вручную.
- [Порядок архивации] → D4 и задача в tasks.

## Migration Plan

1. Проверить на production отсутствие PENDING-заявок без email.
2. Деплой seller-service. Схема БД не меняется.
3. Откат — прежний образ, данные совместимы.
