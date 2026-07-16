package ru.klimovich.user_service.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.klimovich.user_service.model.Item;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WishlistResponse {
    Long id;
    String name;
    List<Item> items;
    LocalDateTime createdAt;
}
