package ru.klimovich.user_service.service;

import org.springframework.security.oauth2.jwt.Jwt;
import ru.klimovich.user_service.dto.request.UserUpdateRequest;
import ru.klimovich.user_service.dto.responce.UserResponse;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserByKeycloakId(String keycloakId);

    UserResponse getUser(Jwt jwt);

    UserResponse updateUserById(String keycloakId, UserUpdateRequest userDetails);

    void deleteUserById(String keycloakId);
}
