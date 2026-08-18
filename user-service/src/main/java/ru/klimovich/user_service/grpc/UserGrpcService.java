package ru.klimovich.user_service.grpc;


import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.klimovich.grpc.user.AssignSellerRoleRequest;
import ru.klimovich.grpc.user.Empty;
import ru.klimovich.grpc.user.RemoveSellerRoleRequest;
import ru.klimovich.grpc.user.UserServiceGrpc;
import ru.klimovich.user_service.service.KeycloakRoleService;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService
        extends UserServiceGrpc.UserServiceImplBase {

    private final KeycloakRoleService keycloakRoleService;

    @Override
    public void assignSellerRole(AssignSellerRoleRequest request, StreamObserver<Empty> responseObserver) {

        keycloakRoleService.assignSellerRole(request.getKeycloakUserId());

        responseObserver.onNext(Empty.newBuilder().build());

        responseObserver.onCompleted();
    }

    @Override
    public void removeSellerRole(RemoveSellerRoleRequest request, StreamObserver<Empty> responseObserver) {

        keycloakRoleService.removeSellerRole(request.getKeycloakUserId());

        responseObserver.onNext(Empty.newBuilder().build());

        responseObserver.onCompleted();
    }
}