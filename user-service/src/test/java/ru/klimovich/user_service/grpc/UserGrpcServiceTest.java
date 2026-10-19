package ru.klimovich.user_service.grpc;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.klimovich.grpc.user.CreateSellerAccountRequest;
import ru.klimovich.grpc.user.CreateSellerAccountResponse;
import ru.klimovich.grpc.user.DeleteSellerAccountRequest;
import ru.klimovich.grpc.user.Empty;
import ru.klimovich.grpc.user.IsEmailAvailableRequest;
import ru.klimovich.grpc.user.IsEmailAvailableResponse;
import ru.klimovich.user_service.exception.NotSellerAccountException;
import ru.klimovich.user_service.exception.SellerAccountConflictException;
import ru.klimovich.user_service.service.KeycloakRoleService;
import ru.klimovich.user_service.service.SellerAccountService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Change separate-seller-account: gRPC-методы аккаунта продавца и коды ошибок.
 */
@ExtendWith(MockitoExtension.class)
class UserGrpcServiceTest {

    @Mock private KeycloakRoleService keycloakRoleService;
    @Mock private SellerAccountService sellerAccountService;

    @InjectMocks private UserGrpcService grpcService;

    @SuppressWarnings("unchecked")
    private static <T> StreamObserver<T> observer() {
        return mock(StreamObserver.class);
    }

    private static Status.Code errorCode(StreamObserver<?> observer) {
        ArgumentCaptor<Throwable> error = ArgumentCaptor.forClass(Throwable.class);
        verify(observer).onError(error.capture());
        return ((StatusRuntimeException) error.getValue()).getStatus().getCode();
    }

    @Test
    @DisplayName("CreateSellerAccount: возвращает id аккаунта")
    void createReturnsId() {
        when(sellerAccountService.createSellerAccount("shop@test.com", "My Shop")).thenReturn("id-1");
        StreamObserver<CreateSellerAccountResponse> observer = observer();

        grpcService.createSellerAccount(CreateSellerAccountRequest.newBuilder()
                .setEmail("shop@test.com").setSellerName("My Shop").build(), observer);

        ArgumentCaptor<CreateSellerAccountResponse> response = ArgumentCaptor.forClass(CreateSellerAccountResponse.class);
        verify(observer).onNext(response.capture());
        assertThat(response.getValue().getKeycloakUserId()).isEqualTo("id-1");
        verify(observer).onCompleted();
    }

    @Test
    @DisplayName("CreateSellerAccount: email занят -> ALREADY_EXISTS")
    void createConflictIsAlreadyExists() {
        when(sellerAccountService.createSellerAccount("shop@test.com", "My Shop"))
                .thenThrow(new SellerAccountConflictException());
        StreamObserver<CreateSellerAccountResponse> observer = observer();

        grpcService.createSellerAccount(CreateSellerAccountRequest.newBuilder()
                .setEmail("shop@test.com").setSellerName("My Shop").build(), observer);

        assertThat(errorCode(observer)).isEqualTo(Status.Code.ALREADY_EXISTS);
    }

    @Test
    @DisplayName("CreateSellerAccount: ошибка Keycloak -> INTERNAL")
    void createFailureIsInternal() {
        when(sellerAccountService.createSellerAccount("shop@test.com", "My Shop"))
                .thenThrow(new RuntimeException("keycloak down"));
        StreamObserver<CreateSellerAccountResponse> observer = observer();

        grpcService.createSellerAccount(CreateSellerAccountRequest.newBuilder()
                .setEmail("shop@test.com").setSellerName("My Shop").build(), observer);

        assertThat(errorCode(observer)).isEqualTo(Status.Code.INTERNAL);
    }

    @Test
    @DisplayName("DeleteSellerAccount: аккаунт покупателя -> FAILED_PRECONDITION")
    void deleteBuyerAccountIsFailedPrecondition() {
        doThrow(new NotSellerAccountException("buyer-id")).when(sellerAccountService).deleteSellerAccount("buyer-id");
        StreamObserver<Empty> observer = observer();

        grpcService.deleteSellerAccount(DeleteSellerAccountRequest.newBuilder().setKeycloakUserId("buyer-id").build(), observer);

        assertThat(errorCode(observer)).isEqualTo(Status.Code.FAILED_PRECONDITION);
    }

    @Test
    @DisplayName("IsEmailAvailable: возвращает признак")
    void isEmailAvailable() {
        when(sellerAccountService.isEmailAvailable("free@test.com")).thenReturn(true);
        StreamObserver<IsEmailAvailableResponse> observer = observer();

        grpcService.isEmailAvailable(IsEmailAvailableRequest.newBuilder().setEmail("free@test.com").build(), observer);

        ArgumentCaptor<IsEmailAvailableResponse> response = ArgumentCaptor.forClass(IsEmailAvailableResponse.class);
        verify(observer).onNext(response.capture());
        assertThat(response.getValue().getAvailable()).isTrue();
    }
}
