package ru.klimovich.sellerservice.reposytory;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.klimovich.sellerservice.model.Seller;

import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    Optional<Seller> findByKeycloakUserId(String keycloakUserId);

    boolean existsByInn(String inn);
}
