package ru.seller_service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.seller_service.dto.request.seller.SellerRegistrationRequest;
import ru.seller_service.dto.responce.SellerApplicationResponse;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.dto.request.seller.SellerUpdateRequest;

@Mapper(config = MapperConfiguration.class)
public interface SellerApplicationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "sellerApplicationType", ignore = true)
    @Mapping(target = "sellerApplicationStatus", ignore = true)
    @Mapping(target = "moderatorComment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SellerApplication toEntity(SellerRegistrationRequest sellerRegistrationDetails);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "sellerApplicationType", ignore = true)
    @Mapping(target = "sellerApplicationStatus", ignore = true)
    @Mapping(target = "moderatorComment", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    SellerApplication toEntity(SellerUpdateRequest request);
    SellerApplicationResponse toDTO(SellerApplication sellerApplication);


}

