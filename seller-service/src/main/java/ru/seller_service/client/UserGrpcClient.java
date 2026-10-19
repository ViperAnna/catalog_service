package ru.seller_service.client;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import ru.klimovich.grpc.user.AssignSellerRoleRequest;
import ru.klimovich.grpc.user.CreateSellerAccountRequest;
import ru.klimovich.grpc.user.DeleteSellerAccountRequest;
import ru.klimovich.grpc.user.IsEmailAvailableRequest;
import ru.klimovich.grpc.user.RemoveSellerRoleRequest;
import ru.klimovich.grpc.user.UserServiceGrpc;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.util.MessageKeys;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class UserGrpcClient {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    public void assignSellerRole(String keycloakUserId) {

        AssignSellerRoleRequest request =
                AssignSellerRoleRequest.newBuilder()
                        .setKeycloakUserId(keycloakUserId)
                        .build();

        userStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .assignSellerRole(request);
    }

    /**
     * Отдельный аккаунт продавца только с ролью SELLER (change separate-seller-account).
     * Email занят другим аккаунтом (ALREADY_EXISTS) — 409.
     */
    public String createSellerAccount(String email, String sellerName) {
        CreateSellerAccountRequest request = CreateSellerAccountRequest.newBuilder()
                .setEmail(email)
                .setSellerName(sellerName)
                .build();
        try {
            return userStub
                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                    .createSellerAccount(request)
                    .getKeycloakUserId();
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.ALREADY_EXISTS) {
                throw new ResourceConflictException(MessageKeys.SELLER_EMAIL_ALREADY_USED, email);
            }
            throw e;
        }
    }

    public void deleteSellerAccount(String keycloakUserId) {
        DeleteSellerAccountRequest request = DeleteSellerAccountRequest.newBuilder()
                .setKeycloakUserId(keycloakUserId)
                .build();

        userStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .deleteSellerAccount(request);
    }

    public boolean isEmailAvailable(String email) {
        IsEmailAvailableRequest request = IsEmailAvailableRequest.newBuilder()
                .setEmail(email)
                .build();

        return userStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .isEmailAvailable(request)
                .getAvailable();
    }

    public void removeSellerRole(String keycloakUserId) {

        RemoveSellerRoleRequest request =
                RemoveSellerRoleRequest.newBuilder()
                        .setKeycloakUserId(keycloakUserId)
                        .build();

        userStub
                .withDeadlineAfter(5, TimeUnit.SECONDS)
                .removeSellerRole(request);
    }
}
