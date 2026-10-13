package ru.seller_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.seller_service.model.status.StoreChangeRequestStatus;

import java.time.LocalDateTime;

/**
 * Заявка владельца на изменение магазина. Поле со значением null не меняет текущее значение
 * при одобрении. Удаляется вместе с магазином (Store.changeRequests, cascade REMOVE).
 */
@Entity
@Table(name = "store_change_requests")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class StoreChangeRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @Column(name = "store_name", length = 100)
    private String storeName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phoneNumber;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private StoreChangeRequestStatus status;

    @Column(name = "moderator_comment", length = 500)
    private String moderatorComment;

    @CreatedDate
    @Column(name = "date_created", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "date_updated")
    private LocalDateTime updatedAt;
}
