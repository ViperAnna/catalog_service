package ru.klimovich.sellerservice.reposytory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.sellerservice.model.SellerApplication;
import ru.klimovich.sellerservice.model.status.SellerApplicationStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface SellerApplicationRepository extends JpaRepository<SellerApplication, Long> {

    Optional<SellerApplication> findByKeycloakUserId(String keycloakUserId);

    List<SellerApplication> findByStatus(SellerApplicationStatus status);
}
