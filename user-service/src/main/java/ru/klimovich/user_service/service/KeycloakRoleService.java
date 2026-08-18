package ru.klimovich.user_service.service;

public interface KeycloakRoleService {
    void assignSellerRole(String keycloakUserId);
    void removeSellerRole(String keycloakUserId);
}
