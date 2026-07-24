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
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    //    @Column(name="keycloak_user_id", unique = true, nullable = false)
    @Column(name = "keycloak_user_id", nullable = false, unique = true)
    private String keycloakUserId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone")
    private String phone;

    @Column(name = "nickname_telegram")
    private String nicknameTelegram;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Embedded
    private Address address;

    @CreatedDate
    @Column(name = "date_create")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "date_update")
    private LocalDateTime updatedAt;
}
