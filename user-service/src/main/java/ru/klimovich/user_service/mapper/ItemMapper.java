package ru.klimovich.user_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.klimovich.user_service.dto.responce.ItemResponse;
import ru.klimovich.user_service.model.Item;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemMapper {
//    Item toEntity(ItemRequest wishlistDTO);

    ItemResponse toDTO(Item item);
}