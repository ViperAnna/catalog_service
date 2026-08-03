package ru.klimovich.sellerservice.mapper;

import org.mapstruct.Mapper;
import ru.klimovich.sellerservice.dto.request.SellerApplicationRequest;
import ru.klimovich.sellerservice.dto.responce.SellerApplicationResponse;
import ru.klimovich.sellerservice.model.SellerApplication;

@Mapper(componentModel = "spring")
public interface SellerApplicationMapper {

    SellerApplication toEntity(SellerApplicationRequest sellerRegistrationDetails);

    SellerApplicationResponse toResponse(SellerApplication sellerApplication);

}

