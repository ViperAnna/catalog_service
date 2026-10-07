## MODIFIED Requirements

### Requirement: Outbox publishing to seller-events-topic
seller-service SHALL каждые 3 секунды (fixed rate) в одной транзакции выбирать
до 100 событий с processed = false по возрастанию createdAt и публиковать их по
одному с ключом aggregateId в топик по aggregateType, ожидая подтверждения не
более 10 секунд. Событие SHALL помечаться processed только после
подтверждения. При ошибке отправки обработка пачки SHALL прекращаться.
Обработанные события SHALL NOT удаляться.

#### Scenario: Kafka недоступна
- **WHEN** отправка события завершилась ошибкой или таймаутом
- **THEN** событие и следующие за ним остаются processed = false до следующего цикла
- **AND** события, отправленные ранее в этом цикле, помечаются processed

#### Scenario: Неизвестный тип события
- **WHEN** eventType записи не является значением SellerEventType для SELLER_APPLICATION или StoreEventType для STORE
- **THEN** запись помечается processed без публикации

#### Scenario: Событие заявки продавца
- **WHEN** в outbox есть запись SELLER_APPLICATION
- **THEN** она публикуется в `seller-events-topic`

#### Scenario: Событие магазина
- **WHEN** в outbox есть запись STORE / STORE_DELETED
- **THEN** она публикуется в `store-events-topic`, а не в `seller-events-topic`

#### Scenario: Неизвестный aggregateType
- **WHEN** aggregateType записи не SELLER_APPLICATION и не STORE
- **THEN** запись помечается processed без публикации

**Assumption**: ASM-02, ASM-03
