package ru.klimovich.sellerservice.reposytory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.sellerservice.model.Store;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByKeycloakUserId(String keycloakUserId);
}
