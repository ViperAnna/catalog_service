package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;
import ru.klimovich.catalog_service.dto.request.CharacteristicRequest;
import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.CategoryShortResponse;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Characteristic;
import ru.klimovich.catalog_service.model.Status;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CharacteristicMapper.class})
public interface ProductMapper {

    @Mappings({
            @Mapping(target = "id", ignore = true),
            @Mapping(target = "images", ignore = true),
            @Mapping(target = "articleNumber", expression = "java(java.util.UUID.randomUUID().toString())"),
            @Mapping(target = "status", qualifiedByName = "mapStatusDTO")
    })
    Product toEntity(ProductRequest productDTO);

    @Mappings({
            @Mapping(source = "images", target = "imagesUrl"),
            @Mapping(target = "categories", qualifiedByName = "mapCategories")
    })
    ProductResponse toDTO(Product product, @Context Map<String, String> categoryNames);

    @Named("mapCategories")
    default List<CategoryShortResponse> mapCategories(List<String> ids, @Context Map<String, String> categoryNames) {
        if (ids == null) return List.of();
        return ids.stream()
                .map(id -> new CategoryShortResponse(id, categoryNames.get(id)))
                .toList();
    }

    @Named("mapCharacteristic")
    CharacteristicRequest mapCharacteristic(Characteristic characteristic);

    @Named("mapStatusDTO")
    default Status stringToStatus(String status) {
        return status != null ? Status.valueOf(status.toUpperCase()) : Status.DRAFT;
    }

    @Named("mapStatus")
    default String statusToString(Status status) {
        return status.name().toLowerCase();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "articleNumber", ignore = true)
    @Mapping(target = "images", ignore = true)
    void updateProductFromDTO(ProductRequest productDetails, @MappingTarget Product product);

}

