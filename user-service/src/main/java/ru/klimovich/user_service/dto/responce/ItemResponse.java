package ru.klimovich.user_service.dto.responce;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.klimovich.user_service.model.Wishlist;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ItemResponse {
    private Long id;
    private String name;
    private String productId;
    private Wishlist wishlists;
}


