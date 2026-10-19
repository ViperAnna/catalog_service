## ADDED Requirements

### Requirement: Store deleted event
При удалении магазина seller-service SHALL в той же транзакции записывать в
outbox событие с aggregateType `STORE`, aggregateId = id магазина, eventType
`STORE_DELETED`, processed = false и payload JSON `{storeId, sellerId}`.
Событие SHALL публиковаться в `store-events-topic`. Если удаление откатилось,
событие SHALL NOT сохраняться.

#### Scenario: Событие при удалении магазина
- **WHEN** продавец с id 7 удаляет свой магазин 3
- **THEN** в outbox записывается STORE_DELETED с aggregateId "3" и payload `{storeId: 3, sellerId: 7}`

#### Scenario: Удаление чужого магазина
- **WHEN** продавец пытается удалить магазин другого продавца
- **THEN** ответ 404, событие не записывается

## MODIFIED Requirements

### Requirement: Store deletion
seller-service SHALL удалять по `DELETE /stores/{id}` магазин текущего Seller
физически и SHALL отвечать 204. Для чужого или несуществующего магазина SHALL
возвращаться 404. Товары магазина seller-service SHALL NOT изменять сам: их
асинхронно архивирует catalog-service по событию `STORE_DELETED`
(`catalog-products`, `Archiving products of deleted store`).

#### Scenario: Удаление своего магазина
- **WHEN** продавец удаляет свой магазин
- **THEN** запись магазина удалена, ответ 204

#### Scenario: Товары после удаления
- **WHEN** удаляется магазин, у которого есть товары в каталоге
- **THEN** после обработки события STORE_DELETED товары остаются в каталоге со ссылкой на удаленный storeId и статусом ARCHIVED
