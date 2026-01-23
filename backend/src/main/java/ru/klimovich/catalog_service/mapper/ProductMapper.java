package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;
import ru.klimovich.catalog_service.dto.request.CharacteristicRequest;
import ru.klimovich.catalog_service.dto.request.ProductRequest;
import ru.klimovich.catalog_service.dto.response.CategoryShortResponse;
import ru.klimovich.catalog_service.dto.response.ProductResponse;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Characteristic;
import ru.klimovich.catalog_service.model.ProductStatus;
import ru.klimovich.catalog_service.util.Utils;

import java.util.List;
import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CharacteristicMapper.class})
public interface ProductMapper {
    @BeforeMapping
    default void setGeneratedFields(ProductRequest productDTO, @MappingTarget Product product) {
        product.setArticleNumber(Utils.generateArticle());
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.DRAFT);
        }
    }

    @Mappings({
            @Mapping(source = "characteristic", target = "characteristic", qualifiedByName = "mapCharacteristicDTO"),
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusDTO"),
            @Mapping(target = "articleNumber", ignore = true)
    })
    Product toEntity(ProductRequest productDTO);

    @Mappings({
            @Mapping(source = "product.id", target = "id"),
            @Mapping(source = "product.name", target = "name"),
            @Mapping(source = "product.description", target = "description"),
            @Mapping(source = "product.brand", target = "brand"),
            @Mapping(source = "product.price", target = "price"),
            @Mapping(source = "product.articleNumber", target = "articleNumber"),
            @Mapping(source = "product.picture", target = "picture"),
            @Mapping(source = "product.characteristic", target = "characteristic", qualifiedByName = "mapCharacteristic"),
            @Mapping(source = "product.tag", target = "tag"),
            @Mapping(source = "product.status", target = "status", qualifiedByName = "mapStatus"),
            @Mapping(target = "categories", expression = "java(mapCategories(product.getCategories(), categoryNames))")
    })
    ProductResponse toDTO(Product product, Map<String, String> categoryNames);

    default List<CategoryShortResponse> mapCategories(List<String> ids, Map<String, String> categoryNames) {
        if (ids == null) return List.of();
        return ids.stream()
                .map(id -> new CategoryShortResponse(id, categoryNames.get(id)))
                .toList();
    }

    @Named("mapCharacteristicDTO")
    Characteristic mapCharacteristicDTO(CharacteristicRequest characteristicDTO);

    @Named("mapCharacteristic")
    CharacteristicRequest mapCharacteristic(Characteristic characteristic);

    @Named("mapStatusDTO")
    default ProductStatus stringToStatus(String status) {
        return status != null ? ProductStatus.valueOf(status.toUpperCase()) : ProductStatus.DRAFT;
    }

    @Named("mapStatus")
    default String statusToString(ProductStatus status) {
        return status.name().toLowerCase();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductFromDTO(ProductRequest productDetails, @MappingTarget Product product);

}

