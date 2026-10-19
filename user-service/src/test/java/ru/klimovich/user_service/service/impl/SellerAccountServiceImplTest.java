package ru.klimovich.user_service.service.impl;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.test.util.ReflectionTestUtils;
import ru.klimovich.user_service.exception.NotSellerAccountException;
import ru.klimovich.user_service.exception.SellerAccountConflictException;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Change separate-seller-account (D2, D3, D7): аккаунт продавца в Keycloak только с ролью SELLER.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SellerAccountServiceImplTest {

    private static final String REALM = "user-service-realm";
    private static final String NEW_ID = "seller-account-id";

    @Mock private Keycloak keycloak;
    @Mock private RealmResource realm;
    @Mock private UsersResource users;
    @Mock private UserResource user;
    @Mock private RolesResource roles;
    @Mock private RoleMappingResource roleMappings;
    @Mock private RoleScopeResource realmRoles;

    private final RoleRepresentation defaultRole = new RoleRepresentation("default-roles-" + REALM, null, false);
    private final RoleRepresentation sellerRole = new RoleRepresentation("SELLER", null, false);

    private SellerAccountServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SellerAccountServiceImpl(keycloak);
        ReflectionTestUtils.setField(service, "realm", REALM);
        when(keycloak.realm(REALM)).thenReturn(realm);
        when(realm.users()).thenReturn(users);
        when(realm.roles()).thenReturn(roles);
        when(users.get(any())).thenReturn(user);
        when(user.roles()).thenReturn(roleMappings);
        when(roleMappings.realmLevel()).thenReturn(realmRoles);
        RoleResource defaultRoleResource = mock(RoleResource.class);
        RoleResource sellerRoleResource = mock(RoleResource.class);
        when(defaultRoleResource.toRepresentation()).thenReturn(defaultRole);
        when(sellerRoleResource.toRepresentation()).thenReturn(sellerRole);
        when(roles.get("default-roles-" + REALM)).thenReturn(defaultRoleResource);
        when(roles.get("SELLER")).thenReturn(sellerRoleResource);
    }

    private static Response created() {
        return Response.created(URI.create("http://keycloak/admin/realms/" + REALM + "/users/" + NEW_ID)).build();
    }

    private static UserRepresentation existing(String id, boolean sellerAccount) {
        UserRepresentation u = new UserRepresentation();
        u.setId(id);
        u.setEmail("shop@test.com");
        if (sellerAccount) {
            u.setAttributes(Map.of("account_type", List.of("seller")));
        }
        return u;
    }

    @Test
    @DisplayName("Создание: email в нижнем регистре, атрибут продавца, только SELLER, письмо с действиями")
    void createSellerAccount() {
        when(users.searchByEmail("shop@test.com", true)).thenReturn(List.of());
        when(users.create(any(UserRepresentation.class))).thenReturn(created());

        String id = service.createSellerAccount("  Shop@Test.com ", "My Shop");

        assertThat(id).isEqualTo(NEW_ID);
        ArgumentCaptor<UserRepresentation> rep = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(rep.capture());
        assertThat(rep.getValue().getUsername()).isEqualTo("shop@test.com");
        assertThat(rep.getValue().getEmail()).isEqualTo("shop@test.com");
        assertThat(rep.getValue().isEnabled()).isTrue();
        assertThat(rep.getValue().isEmailVerified()).isFalse();
        assertThat(rep.getValue().getFirstName()).isEqualTo("My Shop");
        assertThat(rep.getValue().getAttributes()).containsEntry("account_type", List.of("seller"));
        verify(realmRoles).remove(List.of(defaultRole));
        verify(realmRoles).add(List.of(sellerRole));
        verify(user).executeActionsEmail(List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"));
    }

    @Test
    @DisplayName("Повтор: незавершенный аккаунт продавца с тем же email переиспользуется")
    void reuseUnfinishedSellerAccount() {
        when(users.searchByEmail("shop@test.com", true)).thenReturn(List.of(existing("old-id", true)));
        when(user.credentials()).thenReturn(List.of());

        assertThat(service.createSellerAccount("shop@test.com", "My Shop")).isEqualTo("old-id");
        verify(users, never()).create(any());
    }

    @Test
    @DisplayName("Email занят обычным аккаунтом: конфликт")
    void emailUsedByRegularAccount() {
        when(users.searchByEmail("shop@test.com", true)).thenReturn(List.of(existing("buyer-id", false)));

        assertThatThrownBy(() -> service.createSellerAccount("shop@test.com", "My Shop"))
                .isInstanceOf(SellerAccountConflictException.class);
        verify(users, never()).create(any());
    }

    @Test
    @DisplayName("Email занят аккаунтом продавца, в который уже входили: конфликт")
    void emailUsedByActiveSellerAccount() {
        when(users.searchByEmail("shop@test.com", true)).thenReturn(List.of(existing("seller-id", true)));
        when(user.credentials()).thenReturn(List.of(new CredentialRepresentation()));

        assertThatThrownBy(() -> service.createSellerAccount("shop@test.com", "My Shop"))
                .isInstanceOf(SellerAccountConflictException.class);
    }

    @Test
    @DisplayName("Сбой после создания пользователя: созданный пользователь удаляется, ошибка пробрасывается")
    void failureAfterCreationRemovesUser() {
        when(users.searchByEmail("shop@test.com", true)).thenReturn(List.of());
        when(users.create(any(UserRepresentation.class))).thenReturn(created());
        doThrow(new RuntimeException("smtp down")).when(user).executeActionsEmail(any());

        assertThatThrownBy(() -> service.createSellerAccount("shop@test.com", "My Shop"))
                .isInstanceOf(RuntimeException.class);
        verify(users).delete(NEW_ID);
    }

    @Test
    @DisplayName("Удаление: только аккаунт с атрибутом продавца")
    void deleteOnlySellerAccount() {
        when(user.toRepresentation()).thenReturn(existing("seller-id", true));
        service.deleteSellerAccount("seller-id");
        verify(users).delete("seller-id");
    }

    @Test
    @DisplayName("Удаление аккаунта покупателя запрещено")
    void deleteBuyerAccountRejected() {
        when(user.toRepresentation()).thenReturn(existing("buyer-id", false));
        assertThatThrownBy(() -> service.deleteSellerAccount("buyer-id"))
                .isInstanceOf(NotSellerAccountException.class);
        verify(users, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("Удаление уже отсутствующего аккаунта: без ошибки")
    void deleteMissingAccountIsIdempotent() {
        when(user.toRepresentation()).thenThrow(new NotFoundException());
        service.deleteSellerAccount("gone-id");
        verify(users, never()).delete(any(String.class));
    }

    @Test
    @DisplayName("Проверка email: свободен, если пользователей с ним нет")
    void isEmailAvailable() {
        when(users.searchByEmail("free@test.com", true)).thenReturn(List.of());
        when(users.searchByEmail("busy@test.com", true)).thenReturn(List.of(existing("x", false)));

        assertThat(service.isEmailAvailable(" Free@Test.com")).isTrue();
        assertThat(service.isEmailAvailable("busy@test.com")).isFalse();
    }
}
