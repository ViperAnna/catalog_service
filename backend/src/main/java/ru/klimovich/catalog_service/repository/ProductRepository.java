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

    List<Product> findByNameIgnoreCase(String name);

    Page<Product> findByCategoriesContaining(Pageable pageable, String categoryId);

    List<Product> findByCategoriesContaining(String categoryId);

    List<Product> findProductByStatus(Status status);

}
