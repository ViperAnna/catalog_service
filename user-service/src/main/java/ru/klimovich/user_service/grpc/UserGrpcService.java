package ru.klimovich.user_service.grpc;


import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.klimovich.grpc.user.AssignSellerRoleRequest;
import ru.klimovich.grpc.user.CreateSellerAccountRequest;
import ru.klimovich.grpc.user.CreateSellerAccountResponse;
import ru.klimovich.grpc.user.DeleteSellerAccountRequest;
import ru.klimovich.grpc.user.Empty;
import ru.klimovich.grpc.user.IsEmailAvailableRequest;
import ru.klimovich.grpc.user.IsEmailAvailableResponse;
import ru.klimovich.grpc.user.RemoveSellerRoleRequest;
import ru.klimovich.grpc.user.UserServiceGrpc;
import ru.klimovich.user_service.exception.NotSellerAccountException;
import ru.klimovich.user_service.exception.SellerAccountConflictException;
import ru.klimovich.user_service.service.KeycloakRoleService;
import ru.klimovich.user_service.service.SellerAccountService;

@GrpcService
@RequiredArgsConstructor
@Slf4j
public class UserGrpcService
        extends UserServiceGrpc.UserServiceImplBase {

    private final KeycloakRoleService keycloakRoleService;
    private final SellerAccountService sellerAccountService;

    @Override
    public void createSellerAccount(CreateSellerAccountRequest request,
                                    StreamObserver<CreateSellerAccountResponse> responseObserver) {
        try {
            String id = sellerAccountService.createSellerAccount(request.getEmail(), request.getSellerName());
            responseObserver.onNext(CreateSellerAccountResponse.newBuilder().setKeycloakUserId(id).build());
            responseObserver.onCompleted();
        } catch (SellerAccountConflictException e) {
            responseObserver.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("createSellerAccount failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Seller account creation failed").asRuntimeException());
        }
    }

    @Override
    public void deleteSellerAccount(DeleteSellerAccountRequest request, StreamObserver<Empty> responseObserver) {
        try {
            sellerAccountService.deleteSellerAccount(request.getKeycloakUserId());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (NotSellerAccountException e) {
            responseObserver.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (Exception e) {
            log.error("deleteSellerAccount failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Seller account deletion failed").asRuntimeException());
        }
    }

    @Override
    public void isEmailAvailable(IsEmailAvailableRequest request,
                                 StreamObserver<IsEmailAvailableResponse> responseObserver) {
        try {
            boolean available = sellerAccountService.isEmailAvailable(request.getEmail());
            responseObserver.onNext(IsEmailAvailableResponse.newBuilder().setAvailable(available).build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("isEmailAvailable failed", e);
            responseObserver.onError(Status.INTERNAL.withDescription("Email check failed").asRuntimeException());
        }
    }

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