package ru.klimovich.user_service.mapper;

import org.mapstruct.*;
import ru.klimovich.user_service.dto.request.UserRequest;
import ru.klimovich.user_service.dto.request.UserUpdateRequest;
import ru.klimovich.user_service.dto.responce.UserResponse;
import ru.klimovich.user_service.model.User;

import static org.mapstruct.NullValuePropertyMappingStrategy.IGNORE;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toEntity(UserRequest userDTO);

    UserResponse toDTO(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateUserFromDTO(UserUpdateRequest userDetails, @MappingTarget User user);

//    @BeanMapping(nullValuePropertyMappingStrategy = IGNORE)
//    void update(AddressUpdateRequest dto, @MappingTarget Address entity);
}