# Proposal

## Why

Контактные данные в заявках продавца теряются или ломают одобрение
(`seller-onboarding` OBS-01, OBS-07, OBS-12):
- в форме `PUT /seller-applications/update` параметр называется `phone`, а
  в заявке — `phoneNumber`, и явного маппинга нет. Телефон через заявку
  UPDATE изменить нельзя;
- email при регистрации необязателен, но `Seller.email` объявлен
  `nullable=false`. Одобрение заявки без email падает ошибкой БД (500);
- если в заявке UPDATE нет email, события SELLER_APPLICATION_CREATED и
  SELLER_APPROVED уходят с `email = null`, и продавец письмо не получает;
- формат телефона продавца (`\+?[0-9]{7,15}`) отличается от формата магазинов
  (`+7XXXXXXXXXX` / `8XXXXXXXXXX` с нормализацией).

Решение пользователя: переименовать параметр в `phoneNumber`, ввести для
продавца формат телефона как у магазинов, сделать email обязательным при
регистрации, а в UPDATE без email брать текущий email продавца. Это change G1
из разбора OBS `seller-onboarding`.

## What Changes

- **BREAKING (API формы UPDATE):** параметр `phone` в
  `PUT /seller-applications/update` переименован в `phoneNumber`. Запрос с
  `phone` и без `phoneNumber` получает 400. Известных клиентов нет: frontend
  к seller-service не обращается.
- **Телефон в UPDATE переносится:** phoneNumber из формы попадает в заявку и
  после одобрения — в Seller.
- **Формат телефона продавца:** при регистрации и в UPDATE принимаются только
  `+7XXXXXXXXXX` и `8XXXXXXXXXX`, сохраняется `+7XXXXXXXXXX` (существующий
  `PhoneNormalizer`). Прочие формы дают 400.
- **Email при регистрации обязателен:** регистрация без email — 400 вместо 500
  при одобрении.
- **Email в UPDATE:** необязателен. Если не передан, в заявку записывается
  текущий email Seller, поэтому события и письма получают адрес.

### Out of scope

- Заявки, уже сохраненные без email или с телефоном в старом формате.
  На стенде таких нет (проверено). Одобрение старой PENDING-заявки без email
  по-прежнему даст 500 (`Registration application approval`).
- Телефоны существующих Seller в старом формате не нормализуются.
- Разные письма для одобрения UPDATE и DELETION (OBS-02, G3).
- Изменение sellerName и inn через UPDATE (их по-прежнему копирует сервис).

## Capabilities

### New Capabilities

_Нет._

### Modified Capabilities

- `seller-onboarding`: добавляется требование `Seller phone normalization`;
  изменяются `Seller registration application submission`,
  `Registration application field validation`,
  `Seller update application submission` и `Update application approval`.

## Impact

- **seller-service:** `SellerRegistrationRequest`, `SellerUpdateRequest`
  (переименование поля, шаблон телефона, обязательный email при регистрации),
  `SellerApplicationServiceImpl` (нормализация телефона, email по умолчанию),
  тесты.
- **Зависимость от change `notify-moderator-on-update-deletion-applications`:**
  он тоже меняет `Seller update application submission`. Delta этого change
  построена поверх его версии (событие SELLER_APPLICATION_CREATED при UPDATE),
  поэтому архивировать нужно сначала его, а потом этот change. Иначе архив
  notify-moderator перезапишет требование версией с `phone`.
- **Клиенты:** форма UPDATE (BREAKING, см. выше). Kafka, notification-service,
  catalog-service, frontend и Keycloak не меняются.
- **БД:** схема не меняется.
