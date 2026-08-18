package ru.seller_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.seller_service.dto.request.store.StoreRequest;
import ru.seller_service.dto.responce.StoreResponse;
import ru.seller_service.model.Store;

@Mapper(config = MapperConfiguration.class)
public interface StoreMapper {

    StoreResponse toDTO(Store store);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Store toEntity(StoreRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "seller", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromDTO(StoreRequest request, @MappingTarget Store store);
}
