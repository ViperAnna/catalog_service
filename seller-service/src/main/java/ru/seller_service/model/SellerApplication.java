package ru.seller_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerApplicationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_applications")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class SellerApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @Column(name = "inn")
    private String inn;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "payment_account")
    private String paymentAccount;

    @Column(name = "application_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerApplicationStatus sellerApplicationStatus;

    @Column(name = "application_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerApplicationType sellerApplicationType;

    @Column(name = "moderator_comment")
    private String moderatorComment;

    @Column(name = "request_reason")
    private String requestReason;

    @LastModifiedDate
    @Column(name = "date_update")
    private LocalDateTime updatedAt;

    @CreatedDate
    @Column(name = "date_create", updatable = false)
    private LocalDateTime createdAt;
}
