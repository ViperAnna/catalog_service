package ru.seller_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.seller_service.model.Store;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);
    Optional<Store> findByIdAndSellerId(Long storeId, Long sellerId);
    boolean existsBySellerIdAndStoreNameIgnoreCase(Long sellerId, String storeName);
}
