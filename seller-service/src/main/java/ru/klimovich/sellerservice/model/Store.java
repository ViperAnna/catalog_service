package ru.klimovich.sellerservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

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

    @Column(name = "description")
    private String description;

    @Column(name = "email")
    private String email;

    @Column(name = "phone")
    private String phone;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "date_created")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "date_updated")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
