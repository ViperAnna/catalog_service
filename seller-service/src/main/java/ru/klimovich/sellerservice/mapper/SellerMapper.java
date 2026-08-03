package ru.klimovich.sellerservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import ru.klimovich.sellerservice.dto.responce.SellerResponse;
import ru.klimovich.sellerservice.model.Seller;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SellerMapper {

    SellerResponse toDTO(Seller seller);
}
