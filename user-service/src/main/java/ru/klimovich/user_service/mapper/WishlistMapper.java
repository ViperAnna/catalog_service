package ru.klimovich.user_service.mapper;

import org.mapstruct.*;
import ru.klimovich.user_service.dto.request.WishlistRequest;
import ru.klimovich.user_service.dto.responce.WishlistResponse;
import ru.klimovich.user_service.dto.responce.WishlistShortResponse;
import ru.klimovich.user_service.model.Wishlist;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WishlistMapper {
    Wishlist toEntity(WishlistRequest wishlistDTO);

    WishlistResponse toDTO(Wishlist wishlist);

    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateFromDTO(WishlistRequest wishlistDetails, @MappingTarget Wishlist wishlist);

    WishlistShortResponse toShortDTO(Wishlist wishlist);
}