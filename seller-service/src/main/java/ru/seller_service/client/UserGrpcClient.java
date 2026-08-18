package ru.seller_service.client;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.klimovich.grpc.user.AssignSellerRoleRequest;
import ru.klimovich.grpc.user.RemoveSellerRoleRequest;
import ru.klimovich.grpc.user.UserServiceGrpc;

@Component
@RequiredArgsConstructor
public class UserGrpcClient {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    public void assignSellerRole(String keycloakUserId) {

        AssignSellerRoleRequest request =
                AssignSellerRoleRequest.newBuilder()
                        .setKeycloakUserId(keycloakUserId)
                        .build();

        userStub.assignSellerRole(request);
    }

    public void removeSellerRole(String keycloakUserId) {

        RemoveSellerRoleRequest request =
                RemoveSellerRoleRequest.newBuilder()
                        .setKeycloakUserId(keycloakUserId)
                        .build();

        userStub.removeSellerRole(request);
    }
}
