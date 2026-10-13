package ru.seller_service.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    // SELECT ... FOR UPDATE: подача заявки, ее одобрение и удаление магазина (владельцем или
    // вместе с продавцом) выполняются по очереди, поэтому удаление не пропускает заявку,
    // вставленную параллельно, а одобрение не обновляет уже удаленный магазин.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.id = :storeId and s.seller.id = :sellerId")
    Optional<Store> findByIdAndSellerIdForUpdate(@Param("storeId") Long storeId, @Param("sellerId") Long sellerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.id = :storeId")
    Optional<Store> findByIdForUpdate(@Param("storeId") Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Store s where s.seller.id = :sellerId order by s.createdAt desc")
    List<Store> findAllBySellerIdForUpdate(@Param("sellerId") Long sellerId);

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
