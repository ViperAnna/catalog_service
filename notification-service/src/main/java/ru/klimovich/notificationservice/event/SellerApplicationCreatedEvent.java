package ru.klimovich.notificationservice.event;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerApplicationCreatedEvent {
        Long applicationId;
        String keycloakUserId;
        String sellerName;
        String email;
    }
