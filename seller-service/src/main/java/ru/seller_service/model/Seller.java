package ru.seller_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.seller_service.model.status.SellerStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sellers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Seller {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private String keycloakUserId;

    @Column(name = "seller_name", nullable = false, unique = true)
    private String sellerName;

    @Column(name = "inn", nullable = false, unique = true)
    private String inn;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "payment_account", nullable = false)
    private String paymentAccount;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerStatus sellerStatus;

    @OneToMany(mappedBy = "seller", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Store> stores = new ArrayList<>();

    @CreatedDate
    @Column(name = "date_create", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "date_update")
    private LocalDateTime updatedAt;
}
