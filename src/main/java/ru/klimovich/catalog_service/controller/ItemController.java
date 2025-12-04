package ru.klimovich.catalog_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.klimovich.catalog_service.DTO.ItemDTO;
import ru.klimovich.catalog_service.entity.Item;
import ru.klimovich.catalog_service.service.Impl.ItemServiceImpl;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/items")
public class ItemController {

    private final ItemServiceImpl itemService;

    @PostMapping
    public ResponseEntity<ItemDTO> creatItem(@RequestBody ItemDTO itemDTO) {
        ItemDTO newItem = itemService.createItem(itemDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(newItem);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable String id) {
        return ResponseEntity.ok(itemService.getItemById(id));

    }

    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        return ResponseEntity
                .ok(itemService.getAllItems());
    }

    @GetMapping("/itemByName/{itemName}")
    public ResponseEntity<List<ItemDTO>> getItemByName(@PathVariable String itemName) {
        return ResponseEntity.ok(itemService.getItemByName(itemName));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ItemDTO> updateItem(@PathVariable String id, @RequestBody ItemDTO itemDTO) {
        return ResponseEntity.ok(itemService.updateItemById(id, itemDTO));

    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        itemService.deleteItemById(id);
        return ResponseEntity.noContent().build();
    }
}
