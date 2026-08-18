package ru.seller_service.event;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerApplicationCreatedEvent{
        Long applicationId;
        String keycloakUserId;
        String sellerName;
        String email;
    }
