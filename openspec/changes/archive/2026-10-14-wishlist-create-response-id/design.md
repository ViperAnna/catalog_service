# Design

## Context

`WishlistServiceImpl.createWishlist` уже возвращает `WishlistResponse` с id,
`WishlistController.createWishList` игнорирует результат и отдает
`Response {message, time}`. `Response` используется всеми эндпоинтами
user-service.

## Goals / Non-Goals

**Goals:** id в ответе создания без изменения прежних полей.

**Non-Goals:** изменение общего `Response`, ответ с вишлистом целиком,
`Location`.

## Decisions

- **D1. Отдельный DTO.** `WishlistCreatedResponse {message, time, id}` с тем
  же форматом `time` (`yyyy-MM-dd HH:mm:ss`), что у `Response`.
  *Альтернатива:* добавить `id` в общий `Response` — поле появилось бы (как
  `null`) во всех ответах сервиса; отклонено.

## Risks / Trade-offs

- [Клиент строго проверяет набор полей] → в репозитории единственный клиент —
  frontend, тело ответа создания он не читает.
