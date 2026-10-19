## ADDED Requirements

### Requirement: Archiving products of deleted store
catalog-service SHALL потреблять `store-events-topic` в consumer group
`catalog-group` и на событие `STORE_DELETED` SHALL переводить в статус
`ARCHIVED` все товары с этим storeId, у которых статус еще не `ARCHIVED`.
Остальные поля товаров SHALL NOT меняться, товары SHALL NOT удаляться.
Повторная обработка того же события SHALL NOT менять результат. Неизвестный
eventType SHALL пропускаться с предупреждением в логе.

#### Scenario: Архивация товаров магазина
- **WHEN** получено STORE_DELETED для storeId 3, и у магазина товары в статусах ACTIVE и DRAFT
- **THEN** все товары магазина 3 получают статус ARCHIVED, товары других магазинов не меняются

#### Scenario: Повторная доставка
- **WHEN** то же событие STORE_DELETED обрабатывается второй раз
- **THEN** товары остаются в статусе ARCHIVED, ошибки нет

#### Scenario: Магазин без товаров
- **WHEN** получено STORE_DELETED для магазина без товаров
- **THEN** ничего не меняется, сообщение считается обработанным

#### Scenario: Неизвестный тип события
- **WHEN** в `store-events-topic` приходит конверт с неизвестным eventType
- **THEN** сообщение пропускается с предупреждением, товары не меняются

#### Scenario: Архивные товары в выдаче
- **WHEN** товары удаленного магазина заархивированы
- **THEN** они по-прежнему возвращаются публичной выдачей со статусом ARCHIVED

### Requirement: Store events consumer error handling
Ошибка обработки сообщения из `store-events-topic` (например, некорректный
JSON) SHALL повторяться 3 раза с паузой 1 секунда, после чего сообщение SHALL
публиковаться в `store-events-topic.DLT`. Недоступность Kafka при старте
catalog-service SHALL NOT мешать старту сервиса и работе REST API: потребитель
SHALL повторять подключение, пока Kafka не станет доступна.

#### Scenario: Некорректный JSON
- **WHEN** значение сообщения не является JSON-конвертом
- **THEN** после 4 попыток (1 + 3 повтора) сообщение публикуется в `store-events-topic.DLT`

#### Scenario: Старт без Kafka
- **WHEN** catalog-service стартует при недоступной Kafka
- **THEN** сервис стартует и отвечает на `GET /products`, а потребитель начинает работу после появления Kafka
