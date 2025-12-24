package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;
import ru.klimovich.catalog_service.dto.request.CharacteristicRequestDTO;
import ru.klimovich.catalog_service.dto.request.ProductRequestDTO;
import ru.klimovich.catalog_service.dto.response.CharacteristicResponseDTO;
import ru.klimovich.catalog_service.dto.response.ProductResponseDTO;
import ru.klimovich.catalog_service.model.Product;
import ru.klimovich.catalog_service.model.Characteristic;
import ru.klimovich.catalog_service.model.ProductStatus;
import ru.klimovich.catalog_service.util.ArticleUtils;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {CharacteristicMapper.class})
public interface ProductMapper {

    @BeforeMapping
    default void setGeneratedFields(ProductRequestDTO productDTO, @MappingTarget Product product) {
        product.setArticleNumber(ArticleUtils.generateArticle());
        if (product.getStatus() == null) {
            product.setStatus(ProductStatus.DRAFT);
        }
    }

    @Mappings({
            @Mapping(source = "characteristic", target = "characteristic", qualifiedByName = "mapCharacteristicDTO"),
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusDTO"),
            @Mapping(target = "articleNumber", ignore = true)
    })
    Product toEntity(ProductRequestDTO productDTO);

    @Mappings({
            @Mapping(source = "characteristic", target = "characteristic", qualifiedByName = "mapCharacteristic"),
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    })
    ProductResponseDTO toDTO(Product product);

    @Named("mapCharacteristicDTO")
    Characteristic mapCharacteristicDTO(CharacteristicRequestDTO characteristicDTO);

    @Named("mapCharacteristic")
    CharacteristicResponseDTO mapCharacteristic(Characteristic characteristic);


    @Named("mapStatusDTO")
    default ProductStatus stringToStatus(String status) {
        return status != null ? ProductStatus.valueOf(status.toUpperCase()) : ProductStatus.DRAFT;
    }

    @Named("mapStatus")
    default String statusToString(ProductStatus status) {
        return status.name().toLowerCase();
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateProductFromDTO(ProductRequestDTO productDetails, @MappingTarget Product product);
}

