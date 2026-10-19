# Proposal

## Why

`POST /wishlists` отвечает только `{message, time}`: чтобы узнать id нового
вишлиста, клиенту нужен дополнительный `GET /wishlists` (spec `wishlists`
OBS-01). Сервис id уже знает — контроллер его отбрасывает.

## What Changes

- Ответ `POST /wishlists` (201): `{message, time, id}` — к прежнему телу
  добавляется id созданного вишлиста. Поля `message` и `time` и код ответа не
  меняются (обратно совместимо: frontend тело ответа не читает, после создания
  перезапрашивает список).
- Не входят: возврат вишлиста целиком, заголовок `Location`, изменения других
  эндпоинтов.

## Capabilities

### New Capabilities
<!-- нет -->

### Modified Capabilities
- `wishlists`: тело успешного ответа на создание вишлиста.

## Impact

- Код user-service: `WishlistController.createWishList`, новый DTO ответа
  создания. Сервис, БД, другие сервисы и `skipTests` не меняются.
- После archive: OBS-01 в spec `wishlists` — устранено.
