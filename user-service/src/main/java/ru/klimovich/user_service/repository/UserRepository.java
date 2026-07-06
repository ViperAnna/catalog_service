package ru.klimovich.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.user_service.model.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    Optional<User> findByKeycloakUserId(String keycloakUserId);

//    Optional<User> findById(UUID id);

    boolean existsByKeycloakUserId(String keycloakUserId);
}
