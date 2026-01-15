package ru.klimovich.catalog_service.mapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import ru.klimovich.catalog_service.DTO.ItemDTO;
import ru.klimovich.catalog_service.DTO.SpecificationDTO;
import ru.klimovich.catalog_service.entity.Item;
import ru.klimovich.catalog_service.entity.Specification;
import ru.klimovich.catalog_service.entity.emum.StatusItem;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T16:53:39+0300",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 18.0.2 (Amazon.com Inc.)"
)
@Component
public class ItemMapperImpl implements ItemMapper {

    @Override
    public Item toEntity(ItemDTO itemDTO) {
        if ( itemDTO == null ) {
            return null;
        }

        Item item = new Item();

        item.setSpecification( mapSpecificationDTO( itemDTO.getSpecification() ) );
        item.setStatus( stringToStatus( itemDTO.getStatus() ) );
        item.setId( itemDTO.getId() );
        item.setName( itemDTO.getName() );
        item.setDescription( itemDTO.getDescription() );
        item.setBrand( itemDTO.getBrand() );
        item.setPrice( itemDTO.getPrice() );
        item.setArticleNumber( itemDTO.getArticleNumber() );
        List<String> list = itemDTO.getPicture();
        if ( list != null ) {
            item.setPicture( new ArrayList<String>( list ) );
        }
        List<String> list1 = itemDTO.getTag();
        if ( list1 != null ) {
            item.setTag( new ArrayList<String>( list1 ) );
        }

        return item;
    }

    @Override
    public ItemDTO toDTO(Item item) {
        if ( item == null ) {
            return null;
        }

        ItemDTO itemDTO = new ItemDTO();

        itemDTO.setSpecification( mapSpecification( item.getSpecification() ) );
        itemDTO.setStatus( statusToString( item.getStatus() ) );
        itemDTO.setId( item.getId() );
        itemDTO.setName( item.getName() );
        itemDTO.setDescription( item.getDescription() );
        itemDTO.setBrand( item.getBrand() );
        itemDTO.setPrice( item.getPrice() );
        itemDTO.setArticleNumber( item.getArticleNumber() );
        List<String> list = item.getPicture();
        if ( list != null ) {
            itemDTO.setPicture( new ArrayList<String>( list ) );
        }
        List<String> list1 = item.getTag();
        if ( list1 != null ) {
            itemDTO.setTag( new ArrayList<String>( list1 ) );
        }

        return itemDTO;
    }

    @Override
    public void updateItemFromDTO(ItemDTO itemDetails, Item item) {
        if ( itemDetails == null ) {
            return;
        }

        if ( itemDetails.getId() != null ) {
            item.setId( itemDetails.getId() );
        }
        if ( itemDetails.getName() != null ) {
            item.setName( itemDetails.getName() );
        }
        if ( itemDetails.getDescription() != null ) {
            item.setDescription( itemDetails.getDescription() );
        }
        if ( itemDetails.getBrand() != null ) {
            item.setBrand( itemDetails.getBrand() );
        }
        if ( itemDetails.getPrice() != null ) {
            item.setPrice( itemDetails.getPrice() );
        }
        if ( itemDetails.getArticleNumber() != null ) {
            item.setArticleNumber( itemDetails.getArticleNumber() );
        }
        if ( item.getPicture() != null ) {
            List<String> list = itemDetails.getPicture();
            if ( list != null ) {
                item.getPicture().clear();
                item.getPicture().addAll( list );
            }
        }
        else {
            List<String> list = itemDetails.getPicture();
            if ( list != null ) {
                item.setPicture( new ArrayList<String>( list ) );
            }
        }
        if ( itemDetails.getSpecification() != null ) {
            if ( item.getSpecification() == null ) {
                item.setSpecification( new Specification() );
            }
            specificationDTOToSpecification( itemDetails.getSpecification(), item.getSpecification() );
        }
        if ( item.getTag() != null ) {
            List<String> list1 = itemDetails.getTag();
            if ( list1 != null ) {
                item.getTag().clear();
                item.getTag().addAll( list1 );
            }
        }
        else {
            List<String> list1 = itemDetails.getTag();
            if ( list1 != null ) {
                item.setTag( new ArrayList<String>( list1 ) );
            }
        }
        if ( itemDetails.getStatus() != null ) {
            item.setStatus( Enum.valueOf( StatusItem.class, itemDetails.getStatus() ) );
        }
    }

    protected void specificationDTOToSpecification(SpecificationDTO specificationDTO, Specification mappingTarget) {
        if ( specificationDTO == null ) {
            return;
        }

        if ( mappingTarget.getAttributes() != null ) {
            Map<String, String> map = specificationDTO.getAttributes();
            if ( map != null ) {
                mappingTarget.getAttributes().clear();
                mappingTarget.getAttributes().putAll( map );
            }
        }
        else {
            Map<String, String> map = specificationDTO.getAttributes();
            if ( map != null ) {
                mappingTarget.setAttributes( new LinkedHashMap<String, String>( map ) );
            }
        }
    }
}
