package ru.klimovich.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.klimovich.user_service.dto.request.WishlistItemRequest;
import ru.klimovich.user_service.dto.responce.ItemResponse;
import ru.klimovich.user_service.model.WishlistItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WishlistItemMapper {
    WishlistItem toEntity(WishlistItemRequest request);

    ItemResponse toDTO(WishlistItem item);
}