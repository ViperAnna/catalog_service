package ru.klimovich.catalog_service.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import ru.klimovich.catalog_service.DTO.ItemDTO;
import ru.klimovich.catalog_service.DTO.SpecificationDTO;
import ru.klimovich.catalog_service.entity.Item;
import ru.klimovich.catalog_service.entity.Specification;
import ru.klimovich.catalog_service.entity.emum.StatusItem;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ItemMapper {

    ItemMapper INSTANCE = Mappers.getMapper(ItemMapper.class);

    @Mappings({
            @Mapping(source = "specification", target = "specification", qualifiedByName = "mapSpecificationDTO"),
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatusDTO")

    })
    Item toEntity(ItemDTO itemDTO);

    @Mappings({
            @Mapping(source = "specification", target = "specification", qualifiedByName = "mapSpecification"),
            @Mapping(source = "status", target = "status", qualifiedByName = "mapStatus")
    })
    ItemDTO toDTO(Item item);

    @Named("mapSpecificationDTO")
    default Specification mapSpecificationDTO(SpecificationDTO specificationDTO) {
        return specificationDTO != null ? SpecificationMapper.INSTANCE.toEntity(specificationDTO) : null;
    }

    @Named("mapSpecification")
    default SpecificationDTO mapSpecification(Specification specification) {
        return specification != null ? SpecificationMapper.INSTANCE.toDTO(specification) : null;
    }

    @Named("mapStatusDTO")
    default StatusItem stringToStatus(String status) {
        if (status == null) {
            return null;
        }
        return StatusItem.valueOf(status.toUpperCase());
    }

    @Named("mapStatus")
    default String statusToString(StatusItem status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case OUT_OF_STOCK -> "out of stock";
            case IN_STOCK -> "in stock";
            case PRE_ORDER -> "pre order";
            case DRAFT -> "draft";
            default -> throw new IllegalArgumentException("Unknown status: " + status);
        };
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateItemFromDTO(ItemDTO itemDetails, @MappingTarget Item item);
}

