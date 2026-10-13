package ru.seller_service.outbox;

import ru.seller_service.mapper.EventMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Обезличивание payload событий заявок продавца (имя, email, комментарий модератора).
 * Применяется только к записям, публикация которых уже подтверждена или не требуется:
 * повторно отправить такую запись из outbox нельзя. События STORE персональных данных не содержат.
 */
public final class OutboxRedaction {

    static final String SELLER_APPLICATION = "SELLER_APPLICATION";

    private OutboxRedaction() {
    }

    public static boolean containsPersonalData(OutboxEvent event) {
        return SELLER_APPLICATION.equals(event.getAggregateType());
    }

    public static void redact(OutboxEvent event, EventMapper eventMapper) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("applicationId", applicationId(event.getAggregateId()));
        payload.put("redacted", true);
        event.setPayload(eventMapper.toJson(payload));
    }

    private static Object applicationId(String aggregateId) {
        try {
            return Long.valueOf(aggregateId);
        } catch (NumberFormatException e) {
            return aggregateId;
        }
    }
}
