package ru.klimovich.catalog_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.ProductStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findByArticleNumber(String articleNumber);

    List<Product> findByNameIgnoreCase(String name);

    List<Product> findByCategoriesContaining(String nameCategory);

    List<Product> findProductByStatus(ProductStatus status);

}
