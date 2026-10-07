package ru.klimovich.notificationservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerApprovedEvent{
        Long applicationId;
        String keycloakUserId;
        String sellerName;
        String email;
        /**
         * REGISTRATION, UPDATE или DELETION. Строка, а не enum: неизвестный тип не должен ломать разбор события.
         * Отсутствует в событиях, записанных до появления поля.
         */
        String applicationType;
}
