package ru.seller_service.service.impl;

import ru.seller_service.client.UserGrpcClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.seller_service.dto.request.seller.SellerDeletionRequest;
import ru.seller_service.dto.request.seller.SellerUnblockRequest;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.dto.request.seller.SellerRegistrationRequest;
import ru.seller_service.dto.request.seller.SellerUpdateRequest;
import ru.seller_service.event.SellerEventType;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.exception.ResourceNotFoundException;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.mapper.SellerApplicationMapperImpl;
import ru.seller_service.model.Seller;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.model.status.SellerApplicationStatus;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.repository.SellerApplicationRepository;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.security.CurrentUserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerApplicationServiceImplTest {

    private static final String USER_ID = "kc-user-1";
    private static final Long APPLICATION_ID = 42L;

    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private SellerApplicationRepository sellerApplicationRepo;
    @Mock
    private SellerRepository sellerRepo;
    @Mock
    private OutboxRepository outboxRepo;
    @Mock
    private UserGrpcClient userGrpcClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SellerApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SellerApplicationServiceImpl(
                currentUserService,
                sellerApplicationRepo,
                new SellerApplicationMapperImpl(),
                sellerRepo,
                outboxRepo,
                new EventMapper(objectMapper),
                userGrpcClient
        );
        when(currentUserService.getUserId()).thenReturn(USER_ID);
        lenient().when(userGrpcClient.isEmailAvailable(any())).thenReturn(true);
    }

    private void stubApplicationSaveAssignsId() {
        when(sellerApplicationRepo.save(any(SellerApplication.class))).thenAnswer(invocation -> {
            SellerApplication application = invocation.getArgument(0);
            application.setId(APPLICATION_ID);
            return application;
        });
    }

    private OutboxEvent captureSingleOutboxEvent() {
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        return captor.getValue();
    }

    private void assertApplicationCreatedEvent(OutboxEvent event, String sellerName, String email) throws Exception {
        assertThat(event.getEventType()).isEqualTo(SellerEventType.SELLER_APPLICATION_CREATED.name());
        assertThat(event.getAggregateType()).isEqualTo("SELLER_APPLICATION");
        assertThat(event.getAggregateId()).isEqualTo(APPLICATION_ID.toString());
        assertThat(event.isProcessed()).isFalse();

        JsonNode payload = objectMapper.readTree(event.getPayload());
        assertThat(payload.get("applicationId").asLong()).isEqualTo(APPLICATION_ID);
        assertThat(payload.get("keycloakUserId").asText()).isEqualTo(USER_ID);
        assertThat(payload.get("sellerName").asText()).isEqualTo(sellerName);
        if (email == null) {
            assertThat(payload.get("email").isNull()).isTrue();
        } else {
            assertThat(payload.get("email").asText()).isEqualTo(email);
        }
        assertThat(payload.has("applicationType")).isFalse();
    }

    @Test
    void registration_success_writesApplicationCreatedEvent() throws Exception {
        SellerRegistrationRequest request = new SellerRegistrationRequest(
                "Shop", "1234567890", "+79990000000", "shop@example.com", "12345678901234567890");
        stubApplicationSaveAssignsId();

        service.createRegistrationRequest(request);

        assertApplicationCreatedEvent(captureSingleOutboxEvent(), "Shop", "shop@example.com");
    }

    private Seller seller(SellerStatus status) {
        Seller seller = new Seller();
        seller.setId(7L);
        seller.setKeycloakUserId(USER_ID);
        seller.setSellerName("Shop");
        seller.setInn("1234567890");
        seller.setPhoneNumber("+79990000000");
        seller.setEmail("shop@example.com");
        seller.setPaymentAccount("12345678901234567890");
        seller.setSellerStatus(status);
        return seller;
    }

    @Test
    void update_success_writesApplicationCreatedEvent() throws Exception {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));
        stubApplicationSaveAssignsId();
        SellerUpdateRequest request = new SellerUpdateRequest(
                "+79991111111", "new@example.com", "09876543210987654321");

        service.createUpdateRequest(request);

        assertApplicationCreatedEvent(captureSingleOutboxEvent(), "Shop", "new@example.com");
    }

    // --- Контактные данные заявок (change seller-application-contact-data) ---

    private SellerApplication captureSavedApplication() {
        ArgumentCaptor<SellerApplication> captor = ArgumentCaptor.forClass(SellerApplication.class);
        verify(sellerApplicationRepo).save(captor.capture());
        return captor.getValue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_withoutEmail_usesSellerEmail(String email) throws Exception {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));
        stubApplicationSaveAssignsId();
        SellerUpdateRequest request = new SellerUpdateRequest(
                "+79991111111", email, "09876543210987654321");

        service.createUpdateRequest(request);

        assertThat(captureSavedApplication().getEmail()).isEqualTo("shop@example.com");
        assertApplicationCreatedEvent(captureSingleOutboxEvent(), "Shop", "shop@example.com");
    }

    @Test
    void update_transfersNormalizedPhone() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));
        stubApplicationSaveAssignsId();

        service.createUpdateRequest(new SellerUpdateRequest(
                "89990001122", "new@example.com", "09876543210987654321"));

        SellerApplication application = captureSavedApplication();
        assertThat(application.getPhoneNumber()).isEqualTo("+79990001122");
        assertThat(application.getEmail()).isEqualTo("new@example.com");
        assertThat(application.getPaymentAccount()).isEqualTo("09876543210987654321");
    }

    @Test
    void registration_normalizesPhone() {
        stubApplicationSaveAssignsId();

        var response = service.createRegistrationRequest(new SellerRegistrationRequest(
                "Shop", "1234567890", "89991234567", "shop@example.com", "12345678901234567890"));

        assertThat(captureSavedApplication().getPhoneNumber()).isEqualTo("+79991234567");
        assertThat(response.phoneNumber()).isEqualTo("+79991234567");
    }

    @Test
    void update_sellerNotFound_writesNoEvent() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.empty());
        SellerUpdateRequest request = new SellerUpdateRequest(
                "+79991111111", "new@example.com", "09876543210987654321");

        assertThatThrownBy(() -> service.createUpdateRequest(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void update_sellerNotActive_writesNoEvent() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.DELETED)));
        SellerUpdateRequest request = new SellerUpdateRequest(
                "+79991111111", "new@example.com", "09876543210987654321");

        assertThatThrownBy(() -> service.createUpdateRequest(request))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void update_pendingApplicationExists_writesNoEvent() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));
        when(sellerApplicationRepo.existsByKeycloakUserIdAndSellerApplicationStatus(USER_ID, SellerApplicationStatus.PENDING))
                .thenReturn(true);
        SellerUpdateRequest request = new SellerUpdateRequest(
                "+79991111111", "new@example.com", "09876543210987654321");

        assertThatThrownBy(() -> service.createUpdateRequest(request))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void deletion_success_writesApplicationCreatedEvent() throws Exception {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));
        stubApplicationSaveAssignsId();

        service.createDeletionRequest(new SellerDeletionRequest("Closing the shop"));

        assertApplicationCreatedEvent(captureSingleOutboxEvent(), "Shop", "shop@example.com");
    }

    @Test
    void deletion_sellerNotFound_writesNoEvent() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createDeletionRequest(new SellerDeletionRequest("Closing the shop")))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void deletion_sellerNotActive_writesNoEvent() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.DELETED)));

        assertThatThrownBy(() -> service.createDeletionRequest(new SellerDeletionRequest("Closing the shop")))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    // --- Blocked seller restrictions (change seller-store-lifecycle) ---

    @Test
    void deletion_blockedSeller_allowed() throws Exception {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.BLOCKED)));
        stubApplicationSaveAssignsId();

        service.createDeletionRequest(new SellerDeletionRequest("Closing the shop"));

        assertApplicationCreatedEvent(captureSingleOutboxEvent(), "Shop", "shop@example.com");
    }

    @Test
    void update_blockedSeller_conflict() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.BLOCKED)));

        assertThatThrownBy(() -> service.createUpdateRequest(new SellerUpdateRequest(
                "+79991111111", "new@example.com", "09876543210987654321")))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    // --- Seller unblock application (change seller-store-lifecycle) ---

    @Test
    void unblock_blockedSeller_createsPendingApplication() throws Exception {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.BLOCKED)));
        stubApplicationSaveAssignsId();

        service.createUnblockRequest(new SellerUnblockRequest("Issue fixed"));

        ArgumentCaptor<SellerApplication> captor = ArgumentCaptor.forClass(SellerApplication.class);
        verify(sellerApplicationRepo).save(captor.capture());
        assertThat(captor.getValue().getSellerApplicationType()).isEqualTo(SellerApplicationType.UNBLOCK);
        assertThat(captor.getValue().getSellerApplicationStatus()).isEqualTo(SellerApplicationStatus.PENDING);
        assertThat(captor.getValue().getRequestReason()).isEqualTo("Issue fixed");
        assertApplicationCreatedEvent(captureSingleOutboxEvent(), "Shop", "shop@example.com");
    }

    @Test
    void unblock_activeSeller_conflict() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));

        assertThatThrownBy(() -> service.createUnblockRequest(new SellerUnblockRequest("Issue fixed")))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void unblock_pendingUpdateExists_conflict() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.BLOCKED)));
        when(sellerApplicationRepo.existsByKeycloakUserIdAndSellerApplicationStatus(USER_ID, SellerApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createUnblockRequest(new SellerUnblockRequest("Issue fixed")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("User already has a pending application.");
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void deletion_blockedSellerWithPendingUpdate_conflict() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.BLOCKED)));
        when(sellerApplicationRepo.existsByKeycloakUserIdAndSellerApplicationStatus(USER_ID, SellerApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createDeletionRequest(new SellerDeletionRequest("Closing the shop")))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void deletion_pendingApplicationExists_writesNoEvent() {
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller(SellerStatus.ACTIVE)));
        when(sellerApplicationRepo.existsByKeycloakUserIdAndSellerApplicationStatus(USER_ID, SellerApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createDeletionRequest(new SellerDeletionRequest("Closing the shop")))
                .isInstanceOf(ResourceConflictException.class);
        verifyNoInteractions(outboxRepo);
    }
    // --- Seller name uniqueness и тексты ошибок (change seller-service-error-handling) ---

    private static SellerRegistrationRequest registration(String sellerName) {
        return new SellerRegistrationRequest(
                sellerName, "1234567890", "+79990000000", "shop@example.com", "12345678901234567890");
    }

    @Test
    void registration_trimsSellerName() {
        stubApplicationSaveAssignsId();

        service.createRegistrationRequest(registration("  My Shop  "));

        ArgumentCaptor<SellerApplication> captor = ArgumentCaptor.forClass(SellerApplication.class);
        verify(sellerApplicationRepo).save(captor.capture());
        assertThat(captor.getValue().getSellerName()).isEqualTo("My Shop");
        verify(sellerRepo).existsSameName("My Shop");
    }

    @Test
    void registration_nameTakenBySeller_conflict() {
        when(sellerRepo.existsSameName("SHOP")).thenReturn(true);

        assertThatThrownBy(() -> service.createRegistrationRequest(registration("  SHOP ")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Seller with name [SHOP] already exists.");
        verify(sellerApplicationRepo, never()).save(any());
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void registration_nameInPendingApplication_conflict() {
        when(sellerApplicationRepo.existsSameNameWithStatus("Shop", SellerApplicationStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> service.createRegistrationRequest(registration("Shop")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Seller name [Shop] is already in review.");
        verify(sellerApplicationRepo, never()).save(any());
        verifyNoInteractions(outboxRepo);
    }

    @Test
    void registration_sellerExists_message() {
        when(sellerRepo.existsByKeycloakUserIdOrApplicantKeycloakUserId(USER_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createRegistrationRequest(registration("Shop")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Seller already exists for user [" + USER_ID + "].");
    }

    @Test
    @DisplayName("Покупатель, по заявке которого уже создан продавец в отдельном аккаунте: 409")
    void registration_applicantAlreadyHasSeller_conflict() {
        when(sellerRepo.existsByKeycloakUserIdOrApplicantKeycloakUserId(USER_ID, USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createRegistrationRequest(registration("Shop")))
                .isInstanceOf(ResourceConflictException.class);
        verify(sellerApplicationRepo, never()).save(any());
    }

    @Test
    @DisplayName("Email заявки занят аккаунтом Keycloak: 409, заявка не сохраняется")
    void registration_emailTaken_conflict() {
        when(userGrpcClient.isEmailAvailable("shop@example.com")).thenReturn(false);

        assertThatThrownBy(() -> service.createRegistrationRequest(new SellerRegistrationRequest(
                "Shop", "1234567890", "+79990000000", "shop@example.com", "12345678901234567890")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Email [shop@example.com] is already used by another account.");
        verify(sellerApplicationRepo, never()).save(any());
    }

    @Test
    void registration_pendingApplicationExists_message() {
        when(sellerApplicationRepo.existsByKeycloakUserIdAndSellerApplicationStatus(USER_ID, SellerApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createRegistrationRequest(registration("Shop")))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("User already has a pending application.");
    }

    @Test
    void currentApplication_none_message() {
        when(sellerApplicationRepo.findFirstByKeycloakUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyCurrentApplication())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No applications found for user [" + USER_ID + "].");
    }
    @Test
    void registration_afterSellerPurged_accepted() {
        when(sellerRepo.existsByKeycloakUserIdOrApplicantKeycloakUserId(USER_ID, USER_ID)).thenReturn(false);
        when(sellerRepo.existsByInn("1234567890")).thenReturn(false);
        stubApplicationSaveAssignsId();

        service.createRegistrationRequest(new SellerRegistrationRequest(
                "Shop", "1234567890", "+79990000000", "shop@example.com", "12345678901234567890"));

        verify(sellerApplicationRepo).save(any(SellerApplication.class));
    }
}
