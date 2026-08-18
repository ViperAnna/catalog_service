package ru.seller_service.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerStatus;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SellerVerificationRequest {

    private Long id;

    private Long sellerId;

    private String keycloakUserId;

    private SellerApplicationType type;

    private SellerStatus sellerStatus;

    private String inn;

    private String phoneNumber;

    private String email;

    private String paymentAccount;

    private String moderatorComment;
    
}
