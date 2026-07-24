package ru.klimovich.user_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Wishlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @ElementCollection
    @CollectionTable(
            name= "wishlist_products",
            joinColumns = @JoinColumn(name="wishlist_id")
    )
    @Column(name="product_id")
    private Set<String> productIds = new HashSet<>();


    @Column(name = "keycloak_user_id", nullable = false)
    private String keycloakUserId;

    @CreatedDate
    @Column(name = "date_create")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "date_update")
    private LocalDateTime updatedAt;
}
