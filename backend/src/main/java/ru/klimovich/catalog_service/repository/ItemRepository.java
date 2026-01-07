package ru.klimovich.catalog_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.klimovich.catalog_service.entity.Item;
import ru.klimovich.catalog_service.entity.emum.StatusItem;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemRepository extends MongoRepository<Item, String> {

    Optional<Item> findByArticleNumber(Long articleNumber);

    List<Item> findByNameIgnoreCase(String name);

    List<Item> findItemByStatus(StatusItem status);
}
