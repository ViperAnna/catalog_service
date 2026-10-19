package ru.klimovich.catalog_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Status;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findByArticleNumber(String articleNumber);

    List<Product> findAllByIdIn(List<String> productsIds);

    List<Product> findByNameIgnoreCase(String name);

    Page<Product> findByCategoriesContaining(Pageable pageable, String categoryId);

    List<Product> findByCategoriesContaining(String categoryId);

    List<Product> findProductByStatus(Status status);
    List<Product> findAllByStoreId(Long storeId);

    // Публичная выдача: только доступные товары. "Not" дает $ne, поэтому документы
    // без признаков блокировки (созданные до их появления) тоже считаются доступными.
    Page<Product> findByStatusAndStoreBlockedNotAndSellerBlockedNot(
            Status status, Boolean storeBlocked, Boolean sellerBlocked, Pageable pageable);

    Page<Product> findByCategoriesContainingAndStatusAndStoreBlockedNotAndSellerBlockedNot(
            String categoryId, Status status, Boolean storeBlocked, Boolean sellerBlocked, Pageable pageable);

    List<Product> findByNameIgnoreCaseAndStatusAndStoreBlockedNotAndSellerBlockedNot(
            String name, Status status, Boolean storeBlocked, Boolean sellerBlocked);

    List<Product> findAllByIdInAndStatusAndStoreBlockedNotAndSellerBlockedNot(
            List<String> productsIds, Status status, Boolean storeBlocked, Boolean sellerBlocked);
    Page<Product> findAllByStoreId(Long storeId, Pageable pageable);
}
