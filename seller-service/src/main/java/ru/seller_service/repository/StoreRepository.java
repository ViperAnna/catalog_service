package ru.seller_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.seller_service.model.Store;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findAllBySellerIdOrderByCreatedAtDesc(Long sellerId);
    Optional<Store> findByIdAndSellerId(Long storeId, Long sellerId);

    /**
     * Есть ли у продавца другой магазин с тем же именем без учета регистра и пробелов по краям.
     * Выражение совпадает с уникальным индексом uq_stores_seller_store_name (schema.sql).
     */
    @Query("select count(s) > 0 from Store s where s.seller.id = :sellerId "
            + "and lower(trim(s.storeName)) = lower(trim(:name)) "
            + "and (:excludeId is null or s.id <> :excludeId)")
    boolean existsSameName(@Param("sellerId") Long sellerId,
                           @Param("name") String name,
                           @Param("excludeId") Long excludeId);
}
