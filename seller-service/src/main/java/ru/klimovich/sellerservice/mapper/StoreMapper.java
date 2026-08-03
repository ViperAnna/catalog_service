package ru.klimovich.sellerservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.klimovich.sellerservice.dto.request.StoreRequest;
import ru.klimovich.sellerservice.dto.responce.StoreResponse;
import ru.klimovich.sellerservice.model.Store;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)

public interface StoreMapper {

    Store toDTO(StoreRequest storeDetails);

    StoreResponse toEntity(Store store);
}
