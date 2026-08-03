package ru.klimovich.sellerservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.klimovich.sellerservice.model.status.SellerApplicationStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "seller_application")
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

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private String keycloakUserId;

    @Column(name = "inn", nullable = false, unique = true)
    private String inn;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "payment_account", nullable = false)
    private String paymentAccount;

    @Column(name = "seller_application_status")
    @Enumerated(EnumType.STRING)
    private SellerApplicationStatus sellerApplicationStatus;

    @Column(name = "moderator_comment")
    private String moderatorComment;

    @CreatedDate
    @Column(name = "date_create")
    private LocalDateTime createdAt;
}
