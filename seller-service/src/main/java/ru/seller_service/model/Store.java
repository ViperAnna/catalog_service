package ru.seller_service.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import ru.seller_service.model.status.StoreStatus;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    /**
     * ACTIVE или BLOCKED. DELETED не хранится: удаленный магазин физически отсутствует.
     * Колонку добавляет ddl-auto (nullable), значение и NOT NULL задает schema.sql.
     */
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StoreStatus status;

    /** Заявки на изменение удаляются вместе с магазином (удаление владельцем и каскад от Seller). */
    @OneToMany(mappedBy = "store", cascade = CascadeType.REMOVE)
    private List<StoreChangeRequest> changeRequests;

    @Column(name = "description")
    private String description;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone", nullable = false)
    private String phoneNumber;

    @CreatedDate
    @Column(name = "date_created", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "date_updated")
    private LocalDateTime updatedAt;
}
