package ru.klimovich.notificationservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerRejectedEvent{
        Long applicationId;
        String keycloakUserId;
        String sellerName;
        String email;
        String moderatorComment;
}