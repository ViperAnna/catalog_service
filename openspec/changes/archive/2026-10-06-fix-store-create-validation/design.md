# Design

## Context

Мотивация описана в proposal.md, требования — в delta spec
`specs/seller-stores/spec.md`.

Текущее состояние:
- `StoreRequest` — record с `@NotBlank @Size(max=100) storeName`,
  `@Size(max=500) description`, `@Email email` и
  `@Pattern("\+?[0-9]{7,15}") phoneNumber`. Он используется и в
  `POST /stores`, и в `PUT /stores/{id}` (`@Valid @RequestBody`).
- Таблица `stores`: `email` и `phone` объявлены `NOT NULL`, `description`
  имеет тип `varchar(255)`. Схема ведется через `ddl-auto: update`.
- `StoreMapper.updateFromDTO` пропускает null-поля (частичный `PUT`), и это
  поведение нужно сохранить.
- В seller-service нет `@ControllerAdvice`: ошибка валидации дает 400 со
  стандартным телом Spring. Это не меняется (S4).

## Goals / Non-Goals

**Goals:**
- Обязательность email и телефона только при создании, единый формат
  телефона и лимит описания для обоих методов.
- Сохранить частичный `PUT` и текущую обязательность `storeName` при `PUT`.

**Non-Goals:**
- Изменение схемы БД и данных.
- Общий с user-service код нормализации.

## Decisions

### D1. Отдельный DTO для `PUT`, а не группы валидации
`StoreRequest` остается DTO создания, получает `@NotBlank` на `email` и
`phoneNumber`, новый шаблон телефона и `@Size(max = 255)` на `description`.
Для `PUT` добавляется record `StoreUpdateRequest` с теми же полями и
ограничениями формата, но без `@NotBlank` на `email` и `phoneNumber`.
`storeName` в нем остается `@NotBlank @Size(max=100)`, как сейчас.
`StoreController.updateStore`, `StoreService.updateStore` и
`StoreMapper.updateFromDTO` переходят на `StoreUpdateRequest`.

Альтернатива — группы Bean Validation (`OnCreate`/`OnUpdate`) с `@Validated`.
Отклонена: группы требуют замены `@Valid` на `@Validated(...)` в
контроллере, на них проще ошибиться, а два явных record читаются однозначно.
JSON-контракт `PUT` по составу полей не меняется.

### D2. Нормализация телефона в сервисе после маппинга
Добавляется небольшая утилита seller-service (например,
`util/PhoneNormalizer`, статический метод): `8XXXXXXXXXX` превращается в
`+7XXXXXXXXXX`, остальное возвращается как есть. Значение к этому моменту уже
прошло `@Pattern`. `StoreServiceImpl` применяет ее к `store.phoneNumber`
после `toEntity` при создании и после `updateFromDTO` при изменении, если
телефон был передан. Record `StoreRequest` неизменяем, поэтому нормализуется
сущность, а не DTO.

Утилита сделана статической по той же причине, что в C3: конструкторы
сервисов не меняются, а существующие тесты продолжают компилироваться.
Аналогичный код уже есть в user-service (`ContactNormalizer`), но общего
модуля в проекте нет. Дублирование осознанное, это тот же класс проблем,
что `integration-events` OBS-02.

### D3. Тексты сообщений на английском
- `Email is required.` и `Phone is required.` — для `POST`;
- `Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX.`;
- `Description can't exceed 255 characters.`

## Risks / Trade-offs

- [BREAKING для клиентов, которые отправляют другие форматы телефона] →
  frontend `/stores` не использует, другие клиенты API неизвестны. Отмечено в
  proposal.
- [Старые телефоны в БД в неканонической форме] → В текущей локальной БД
  один магазин, телефон `+79990000002` в каноническом формате. При деплое на
  production данные стоит проверить тем же запросом
  (`phone !~ '^(\+7|8)[0-9]{10}$'`). Старая запись станет канонической при
  следующем изменении телефона.
- [Текст ошибки не виден клиенту] → Стандартное тело 400 Spring не включает
  сообщения. Это исправит S4.

## Migration Plan

Задеплоить seller-service. Схема и данные не меняются. Откат — предыдущий
образ.
