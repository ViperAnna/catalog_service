package ru.klimovich.user_service.service.impl;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.klimovich.user_service.exception.NotSellerAccountException;
import ru.klimovich.user_service.exception.SellerAccountConflictException;
import ru.klimovich.user_service.service.SellerAccountService;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Аккаунт продавца: логин и email — контактный email заявки, только realm-роль SELLER (без составной
 * роли по умолчанию, в которую входит USER), атрибут account_type=seller, письмо установки пароля
 * (change separate-seller-account, D2, D3, D7). Адреса email в лог не пишутся.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SellerAccountServiceImpl implements SellerAccountService {

    static final String ACCOUNT_TYPE = "account_type";
    static final String SELLER_ACCOUNT = "seller";
    private static final String SELLER_ROLE = "SELLER";
    private static final List<String> ACTIONS = List.of("UPDATE_PASSWORD", "VERIFY_EMAIL");

    private final Keycloak keycloak;

    @Value("${application.realm}")
    private String realm;

    @Override
    public String createSellerAccount(String email, String sellerName) {
        String normalizedEmail = normalize(email);
        RealmResource realmResource = keycloak.realm(realm);
        UsersResource users = realmResource.users();

        List<UserRepresentation> existing = users.searchByEmail(normalizedEmail, true);
        if (!existing.isEmpty()) {
            UserRepresentation user = existing.get(0);
            // Остаток незавершенного одобрения: аккаунт продавца, в который ни разу не входили (нет пароля).
            if (isSellerAccount(user) && users.get(user.getId()).credentials().isEmpty()) {
                log.info("Reusing unfinished seller account {}", user.getId());
                return user.getId();
            }
            throw new SellerAccountConflictException();
        }

        String id = create(users, normalizedEmail, sellerName);
        try {
            UserResource user = users.get(id);
            user.roles().realmLevel().remove(List.of(
                    realmResource.roles().get("default-roles-" + realm).toRepresentation()));
            user.roles().realmLevel().add(List.of(
                    realmResource.roles().get(SELLER_ROLE).toRepresentation()));
            user.executeActionsEmail(ACTIONS);
            return id;
        } catch (RuntimeException e) {
            // Не оставлять наполовину настроенный аккаунт: следующая попытка создаст его заново.
            log.error("Seller account {} setup failed, removing it", id, e);
            users.delete(id);
            throw e;
        }
    }

    @Override
    public void deleteSellerAccount(String keycloakUserId) {
        UsersResource users = keycloak.realm(realm).users();
        UserRepresentation user;
        try {
            user = users.get(keycloakUserId).toRepresentation();
        } catch (NotFoundException e) {
            log.info("Seller account {} is already absent", keycloakUserId);
            return;
        }
        if (!isSellerAccount(user)) {
            throw new NotSellerAccountException(keycloakUserId);
        }
        users.delete(keycloakUserId);
    }

    @Override
    public boolean isEmailAvailable(String email) {
        return keycloak.realm(realm).users().searchByEmail(normalize(email), true).isEmpty();
    }

    private static String create(UsersResource users, String email, String sellerName) {
        UserRepresentation rep = new UserRepresentation();
        rep.setUsername(email);
        rep.setEmail(email);
        rep.setFirstName(sellerName);
        rep.setEnabled(true);
        rep.setEmailVerified(false);
        rep.setAttributes(Map.of(ACCOUNT_TYPE, List.of(SELLER_ACCOUNT)));
        try (Response response = users.create(rep)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                // Email или логин заняли между проверкой и созданием.
                throw new SellerAccountConflictException();
            }
            return CreatedResponseUtil.getCreatedId(response);
        }
    }

    private static boolean isSellerAccount(UserRepresentation user) {
        Map<String, List<String>> attributes = user.getAttributes();
        return attributes != null && attributes.getOrDefault(ACCOUNT_TYPE, List.of()).contains(SELLER_ACCOUNT);
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }
}
