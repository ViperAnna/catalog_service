package ru.klimovich.user_service.service.impl;

import lombok.RequiredArgsConstructor;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.klimovich.user_service.service.KeycloakRoleService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KeycloakRoleServiceImpl implements KeycloakRoleService {
    private final Keycloak keycloak;

    @Value("${application.realm}")
    private String realm;


    @Override
    public void assignSellerRole(String keycloakUserId) {

        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakUserId);
        RoleRepresentation sellerRole = realmResource.roles()
                .get("seller")
                .toRepresentation();
        userResource.roles().realmLevel().add(List.of(sellerRole));
    }

    @Override
    public void removeSellerRole(String keycloakUserId) {

        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakUserId);
        RoleRepresentation sellerRole = realmResource.roles()
                .get("seller")
                .toRepresentation();
        userResource.roles().realmLevel().remove(List.of(sellerRole));

    }
}
