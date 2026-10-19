# catalog-categories Specification

## Purpose
Baseline (as-is) поведения catalog-service по управлению категориями товаров:
публичное чтение списка и отдельной категории, создание, изменение и удаление
категорий администратором, нормализация имени и правила удаления категорий,
к которым привязаны товары. Спецификация описывает фактическое текущее
поведение кода, а не целевое.

## Baseline Context
- Источник: ветка `feature/SAM1-14`, коммит `78cec79`. Код категорий в рабочей
  копии не изменен.
- Метод: статический анализ кода и сгенерированного `CategoryMapperImpl`.
  Формат ответа `GET /categories` подтвержден запросом к dev-стенду через
  api-gateway. Тестов для категорий нет.
- Хранение: MongoDB, коллекция `categories`. Изображения хранятся в MinIO,
  механика хранения описана в `catalog-media`.
- Секция Requirements нормативна. Секции Observations, Assumptions и
  Open Questions нормативными не являются. Поведение, помеченное как
  observation, зафиксировано потому, что так работает код сейчас. Изменять его
  нужно отдельным change.
- Пути указаны относительно catalog-service. Через api-gateway они доступны без
  изменений, через nginx — с префиксом `/api`.
- Профиль `generate-data` (`CategoryInitializer`) пересоздает категории из
  `data/categories.json`. Это инструмент наполнения тестовыми данными, он вне
  scope spec.

## Requirements

### Requirement: Category API access
catalog-service SHALL разрешать без аутентификации `GET /categories` и
`GET /categories/{id}`. Все остальные запросы к `/categories/**` SHALL требовать
JWT с realm-ролью ADMIN (claim `realm_access.roles`).

#### Scenario: Анонимное чтение
- **WHEN** клиент без токена вызывает `GET /categories`
- **THEN** catalog-service отвечает 200

#### Scenario: Создание без роли ADMIN
- **WHEN** клиент с JWT, содержащим только роль SELLER, вызывает `POST /categories`
- **THEN** catalog-service отвечает 403

#### Scenario: Создание без токена
- **WHEN** клиент без токена вызывает `DELETE /categories/{id}`
- **THEN** catalog-service отвечает 401

### Requirement: Category creation
catalog-service SHALL принимать `POST /categories` как `multipart/form-data` с
полями name, description, image, status и SHALL создавать категорию с
нормализованным именем, описанием, статусом и загруженным изображением. Ответ
SHALL иметь код 201 и тело `{message, time}` без данных созданной категории.

#### Scenario: Успешное создание
- **WHEN** администратор отправляет валидные name, JPEG-изображение и status ACTIVE
- **THEN** категория сохраняется, изображение загружается в хранилище категорий
- **AND** ответ 201 содержит message "The category was successfully created." и time

#### Scenario: Создание без статуса
- **WHEN** status не передан
- **THEN** категория сохраняется без статуса, и в ответах чтения status = null

**Observation**: OBS-01, OBS-05

### Requirement: Category field validation
catalog-service SHALL отклонять с кодом 400 создание категории, если name пуст
или длиннее 20 символов, description длиннее 500 символов, image отсутствует или
не проходит проверку изображения (`catalog-media`), или status не равен ACTIVE,
INACTIVE, ARCHIVED или DRAFT. При изменении SHALL действовать те же ограничения,
но name и image SHALL быть необязательными.

#### Scenario: Нет изображения
- **WHEN** администратор создает категорию без image
- **THEN** catalog-service отвечает 400 с message, начинающимся с "Validation failed:"

#### Scenario: Длинное имя
- **WHEN** name содержит 21 символ
- **THEN** catalog-service отвечает 400

#### Scenario: Недопустимый статус
- **WHEN** status равен "active" (в нижнем регистре)
- **THEN** catalog-service отвечает 400

**Observation**: OBS-03

### Requirement: Category name normalization and uniqueness on creation
При создании catalog-service SHALL нормализовать name: убирать пробелы по краям,
заменять последовательности пробельных символов одним пробелом и приводить
строку к нижнему регистру. Если категория с таким нормализованным именем уже
существует, catalog-service SHALL отвечать 409.

#### Scenario: Нормализация
- **WHEN** администратор создает категорию с name "  Home   Goods "
- **THEN** категория сохраняется с name "home goods"

#### Scenario: Дубликат после нормализации
- **WHEN** существует категория "home goods" и создается категория "HOME GOODS"
- **THEN** catalog-service отвечает 409 с message "Category with name [HOME GOODS] already exists."

**Observation**: OBS-02, OBS-08

### Requirement: Category listing
catalog-service SHALL возвращать по `GET /categories` все категории в любом
статусе одним списком, без пагинации и фильтрации.

#### Scenario: Категории в разных статусах
- **WHEN** существуют категории со статусами ACTIVE и DRAFT
- **THEN** обе возвращаются в списке

**Observation**: OBS-05

### Requirement: Category retrieval by id
catalog-service SHALL возвращать по `GET /categories/{id}` категорию с указанным
id в любом статусе. Для несуществующего id SHALL возвращаться 404 с телом
`{message, time}`.

#### Scenario: Категория не найдена
- **WHEN** запрашивается несуществующий id
- **THEN** ответ 404 с message "Category not found with id: [<id>]."

### Requirement: Category response format
Ответ чтения категории SHALL содержать поля id, name, description, imageUrl,
status, createdAt, updatedAt. imageUrl SHALL быть публичным URL изображения,
status SHALL быть именем значения (например, "ACTIVE") или null, а createdAt и
updatedAt SHALL иметь формат `yyyy-MM-dd HH:mm:ss` и заполняться автоматически
при создании и изменении.

#### Scenario: Формат ответа
- **WHEN** запрашивается категория
- **THEN** ответ содержит imageUrl вида `<public-endpoint>/categories/<sha256>_<имя файла>` и даты в формате `yyyy-MM-dd HH:mm:ss`

### Requirement: Category update
catalog-service SHALL принимать `PUT /categories/{id}` с полями name,
description, image, status и SHALL менять только переданные (не null) поля.
Новое name SHALL сохраняться без нормализации и без проверки уникальности.
Для несуществующего id SHALL возвращаться 404. Ответ SHALL иметь код 200 и тело
`{message, time}`.

#### Scenario: Изменение описания
- **WHEN** администратор передает только description
- **THEN** меняется только description, ответ 200 с message "The category was successfully updated."

#### Scenario: Имя без нормализации
- **WHEN** администратор меняет name на "Home Goods"
- **THEN** категория сохраняется с name "Home Goods" в исходном регистре

#### Scenario: Имя совпадает с другой категорией
- **WHEN** администратор меняет name на имя уже существующей категории
- **THEN** изменение сохраняется, ответ 200

**Observation**: OBS-01, OBS-02

### Requirement: Category image replacement on update
Если при изменении передано image, catalog-service SHALL вычислять его SHA-256
и SHALL загружать новое изображение и заменять им изображение категории,
только если хеш отличается от хеша текущего изображения или изображения нет.
Предыдущее изображение SHALL NOT удаляться из хранилища.

#### Scenario: То же изображение
- **WHEN** передан файл с тем же содержимым, что и текущее изображение
- **THEN** изображение категории не меняется

#### Scenario: Новое изображение
- **WHEN** передан файл с другим содержимым
- **THEN** imageUrl категории указывает на новое изображение, а старый объект остается в хранилище

**Observation**: OBS-04

### Requirement: Category deletion
catalog-service SHALL по `DELETE /categories/{id}` сначала проверять, есть ли
товары, в списке категорий которых указан этот id, и при наличии SHALL отвечать
409. Иначе для несуществующего id SHALL возвращаться 404, а существующая
категория SHALL удаляться с ответом 200 и телом `{message, time}`. Изображение
SHALL NOT удаляться из хранилища.

#### Scenario: Категория с товарами
- **WHEN** у категории есть товары
- **THEN** ответ 409 с message "Category has products. Category cannot be deleted."

#### Scenario: Успешное удаление
- **WHEN** у категории нет товаров
- **THEN** категория удалена, ответ 200 с message "The category was successfully deleted."

**Observation**: OBS-04, OBS-07

### Requirement: Category error responses
Для ошибок валидации (400), отсутствия ресурса (404), конфликтов (409) и
недоступности хранилища (503) catalog-service SHALL возвращать тело
`{message, time}`, где time имеет формат `yyyy-MM-dd HH:mm:ss`. Для 400 по
валидации message SHALL начинаться с "Validation failed: " и перечислять
сообщения нарушенных ограничений через запятую. Ответы 401 и 403 SHALL
формироваться Spring Security без этого тела.

#### Scenario: Ошибка валидации
- **WHEN** запрос нарушает два ограничения
- **THEN** ответ 400 с message "Validation failed: <сообщение 1>, <сообщение 2>"

**Assumption**: ASM-01, ASM-02

## Observations (non-normative)
Поведение подтверждено кодом и может быть дефектом. В Requirements оно
зафиксировано как есть. Исправлять его нужно отдельным change после решения.

- **OBS-01. Ответы не содержат категорию.** Ответы на создание и изменение не
  содержат саму категорию и ее id. Чтобы узнать id созданной категории,
  клиенту нужно заново запрашивать список.
- **OBS-02. Нормализация только при создании.** Имя нормализуется и
  проверяется на уникальность только при создании. Через PUT можно задать имя
  в другом регистре или дублирующее имя другой категории.
- **OBS-03. application/octet-stream проходит как изображение.** Тип
  `application/octet-stream` входит в список допустимых для изображения
  категории. Проверка по сигнатуре JPEG выполняется, только если тип файла не
  из списка, поэтому любой файл с этим типом принимается (см. `catalog-media`).
- **OBS-04. Изображения не удаляются.** Замена изображения и удаление категории
  не удаляют объекты из MinIO.
- **OBS-05. Статус категории ни на что не влияет.** Статус необязателен, а
  чтение и привязка товаров не учитывают его: DRAFT и INACTIVE видны
  анонимным пользователям.
- **OBS-06. Набор значений статуса.** Он совпадает с набором статусов товара
  (общий enum `Status`). Отдельной семантики для категорий в коде нет.
- **OBS-07. Порядок проверок при удалении.** Проверка «есть товары» выполняется
  раньше проверки существования. Для несуществующего id без товаров ответ 404,
  но проверка товаров выполняется в любом случае.
- **OBS-08. Уникальность имени без индекса.** Она обеспечивается только
  проверкой в приложении, уникального индекса по name в MongoDB нет.
- **OBS-09. Пустой PUT.** PUT без полей возвращает 200 и ничего не меняет.

## Assumptions (unverified)
- **ASM-01. Превышение лимитов multipart.** Файл больше 5MB или запрос больше
  30MB, вероятно, отклоняется стандартной обработкой Spring (413) до
  контроллера, без тела `{message, time}`.
- **ASM-02. Необработанные ошибки.** Ошибки MinIO, не являющиеся
  `MinioException` (например, IO при чтении файла), не обрабатываются
  `GlobalExceptionHandler` и, вероятно, дают 500.
- **ASM-03. Гонки.** Параллельное создание двух категорий с одним именем может
  пройти проверку (OBS-08).

## Open Questions
- **OQ-01. OBS-01–OBS-09.** Какие из них дефекты?
- **OQ-02. Статус категории.** Должен ли он влиять на публичную выдачу и на
  возможность привязывать к категории товары?
- **OQ-03. Форматирование имени.** Должно ли имя категории храниться в нижнем
  регистре или в исходном написании с регистронезависимой уникальностью?
