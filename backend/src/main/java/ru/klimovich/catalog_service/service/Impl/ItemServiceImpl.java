package ru.klimovich.catalog_service.service.Impl;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import ru.klimovich.catalog_service.DTO.ItemDTO;
import ru.klimovich.catalog_service.entity.Item;
import ru.klimovich.catalog_service.exception.CategoryNotFoundException;
import ru.klimovich.catalog_service.exception.ItemNotFoundException;
import ru.klimovich.catalog_service.mapper.ItemMapper;
import ru.klimovich.catalog_service.mapper.SpecificationMapper;
import ru.klimovich.catalog_service.repository.ItemRepository;
import ru.klimovich.catalog_service.service.CategoryService;
import ru.klimovich.catalog_service.service.ItemService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@AllArgsConstructor
public class ItemServiceImpl implements ItemService {
    private static final Long MAX_VALUE_ARTICLE_NUMBER = 100_000_000L;

    private final ItemRepository itemRepo;
    private final Random random;
    private final CategoryService categoryService;

    @Override
    public ItemDTO createItem(ItemDTO itemDetails) {
        Item item = new Item();
        item.setName(itemDetails.getName());
        item.setDescription(itemDetails.getDescription());
        item.setBrand(itemDetails.getBrand());
        item.setPrice(itemDetails.getPrice());
        item.setArticleNumber(itemDetails.getArticleNumber() != null ? itemDetails.getArticleNumber() : random.longs(MAX_VALUE_ARTICLE_NUMBER).findFirst().orElse(1_000_000_000L));
        item.setPicture(itemDetails.getPicture());
        item.setSpecification(SpecificationMapper.INSTANCE.toEntity(itemDetails.getSpecification()));
        item.setTag(itemDetails.getTag());
        item.setStatus(ItemMapper.INSTANCE.stringToStatus(itemDetails.getStatus()));
        item.setDateCreate(LocalDateTime.now());
        item.setDateUpdate(LocalDateTime.now());
        return ItemMapper.INSTANCE.toDTO(itemRepo.save(item));
    }

    @Override
    public List<ItemDTO> getAllItems() {
        List<Item> itemList = itemRepo.findAll();
        return itemList.stream()
                .map(ItemMapper.INSTANCE::toDTO)
                .toList();
    }

    @Override
    public ItemDTO getItemById(String id) {
        Item item = itemRepo.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        return ItemMapper.INSTANCE.toDTO(item);
    }

    @Override
    public List<ItemDTO> getItemByName(String nameItem) {
        List<Item> items = itemRepo.findByNameIgnoreCase(nameItem);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No items found with name [" + nameItem + "].");
        }
        return items.stream()
                .map(ItemMapper.INSTANCE::toDTO)
                .toList();
    }

    @Override
    public ItemDTO updateItemById(String id, ItemDTO itemDetails) {
        Item item = itemRepo.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        ItemMapper.INSTANCE.updateItemFromDTO(itemDetails, item);
        return ItemMapper.INSTANCE.toDTO(itemRepo.save(item));
    }

    @Override
    public void deleteItemById(String id) {
        Item item = itemRepo.findById(id)
                .orElseThrow(() -> new ItemNotFoundException("Item not found with id: " + id));
        itemRepo.delete(item);
    }
}
