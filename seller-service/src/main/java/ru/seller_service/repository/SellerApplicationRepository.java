package ru.seller_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.model.status.SellerApplicationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {
    Optional<SellerApplication> findByIdAndSellerApplicationStatus(Long id, SellerApplicationStatus status);

    Optional<SellerApplication> findFirstByKeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    Optional<SellerApplication> findFirstByKeycloakUserIdAndSellerApplicationStatusOrderByCreatedAtDesc(String keycloakUserId, SellerApplicationStatus status);

    List<SellerApplication> findAllByKeycloakUserIdOrderByCreatedAtDesc(String keycloakUserId);

    List<SellerApplication> findAllBySellerApplicationStatusOrderByCreatedAtAsc(SellerApplicationStatus status);

    boolean existsByKeycloakUserIdAndSellerApplicationStatus(String keycloakUserId, SellerApplicationStatus status);

    Optional<SellerApplication> findByKeycloakUserId(String keycloakUserId);

    boolean existsByInnAndSellerApplicationStatus(String inn, SellerApplicationStatus status);



}
