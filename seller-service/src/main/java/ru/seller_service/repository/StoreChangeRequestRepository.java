package ru.seller_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.seller_service.model.StoreChangeRequest;
import ru.seller_service.model.status.StoreChangeRequestStatus;

import java.util.List;

public interface StoreChangeRequestRepository extends JpaRepository<StoreChangeRequest, Long> {

    boolean existsByStoreIdAndStatus(Long storeId, StoreChangeRequestStatus status);

    List<StoreChangeRequest> findAllByStoreIdOrderByCreatedAtDesc(Long storeId);

    List<StoreChangeRequest> findAllByStatusOrderByCreatedAtAsc(StoreChangeRequestStatus status);
}
