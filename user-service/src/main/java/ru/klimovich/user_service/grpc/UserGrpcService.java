package ru.klimovich.user_service.grpc;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.klimovich.grpc.user.AssignSellerRoleRequest;
import ru.klimovich.grpc.user.Empty;
import ru.klimovich.grpc.user.RemoveSellerRoleRequest;
import ru.klimovich.grpc.user.UserServiceGrpc;
import ru.klimovich.user_service.service.KeycloakRoleService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService
        extends UserServiceGrpc.UserServiceImplBase {

    private final KeycloakRoleService keycloakRoleService;

    @Override
    public void assignSellerRole(AssignSellerRoleRequest request, StreamObserver<Empty> responseObserver) {
        try {

            keycloakRoleService.assignSellerRole(request.getKeycloakUserId());

            responseObserver.onNext(Empty.newBuilder().build());

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("assignSellerRole failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Role assignment failed").asRuntimeException());
        }
    }


    @Override
    public void removeSellerRole(RemoveSellerRoleRequest request, StreamObserver<Empty> responseObserver) {
        try {
            keycloakRoleService.removeSellerRole(request.getKeycloakUserId());

            responseObserver.onNext(Empty.newBuilder().build());

            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("assignSellerRole failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Role assignment failed").asRuntimeException());
        }
    }
}