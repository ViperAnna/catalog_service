package ru.seller_service.event;

import lombok.*;
import ru.seller_service.model.status.SellerApplicationType;

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
        /** Тип одобренной заявки: notification-service выбирает по нему письмо продавцу. */
        SellerApplicationType applicationType;
}
