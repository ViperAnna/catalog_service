package ru.seller_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.seller_service.dto.responce.SellerResponse;
import ru.seller_service.model.Seller;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.dto.request.seller.SellerUpdateRequest;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(config = MapperConfiguration.class)
public interface SellerMapper {

    SellerResponse toDTO(Seller seller);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "sellerStatus", constant = "ACTIVE")
    @Mapping(target = "stores", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Seller toEntity(SellerApplication application);

    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "sellerStatus", ignore = true)
    @Mapping(target = "stores", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromDTO(SellerUpdateRequest sellerUpdateDetails, @MappingTarget Seller seller);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakUserId", ignore = true)
    @Mapping(target = "stores", ignore = true)
    @Mapping(target = "sellerStatus", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromApplication(SellerApplication sellerApplication, @MappingTarget Seller seller);


}
