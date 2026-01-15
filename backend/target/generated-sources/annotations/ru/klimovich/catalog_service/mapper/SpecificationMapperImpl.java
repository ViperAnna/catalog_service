package ru.klimovich.catalog_service.mapper;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.DTO.SpecificationDTO;
import ru.klimovich.catalog_service.entity.Specification;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T16:53:40+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 18.0.2 (Amazon.com Inc.)"
)
@Component
public class SpecificationMapperImpl implements SpecificationMapper {

    @Override
    public Specification toEntity(SpecificationDTO specificationDTO) {
        if ( specificationDTO == null ) {
            return null;
        }

        Specification specification = new Specification();

        Map<String, String> map = specificationDTO.getAttributes();
        if ( map != null ) {
            specification.setAttributes( new LinkedHashMap<String, String>( map ) );
        }

        return specification;
    }

    @Override
    public SpecificationDTO toDTO(Specification specification) {
        if ( specification == null ) {
            return null;
        }

        SpecificationDTO specificationDTO = new SpecificationDTO();

        Map<String, String> map = specification.getAttributes();
        if ( map != null ) {
            specificationDTO.setAttributes( new LinkedHashMap<String, String>( map ) );
        }

        return specificationDTO;
    }
}
