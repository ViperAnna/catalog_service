package ru.klimovich.catalog_service.service;

import ru.klimovich.catalog_service.DTO.ItemDTO;

import java.util.List;

public interface ItemService {

     List<ItemDTO> getAllItems();
    ItemDTO getItemById(String id);
    List<ItemDTO> getItemByName(String nameItem);

    ItemDTO createItem(ItemDTO itemDetails);

     ItemDTO updateItemById(String id, ItemDTO itemDetails);

     void deleteItemById(String id);
}
