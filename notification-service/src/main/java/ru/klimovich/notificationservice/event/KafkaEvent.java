package ru.klimovich.notificationservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KafkaEvent {
    private String aggregateType;
    private SellerEventType eventType;
    private String payload;
}
