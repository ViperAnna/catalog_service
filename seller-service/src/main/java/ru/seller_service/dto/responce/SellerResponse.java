package ru.seller_service.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerStatus;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class SellerResponse {
    private Long id;
    private SellerApplicationType type;
    private SellerStatus sellerStatus;
    private String moderatorComment;
    private LocalDateTime createdAt;
}
