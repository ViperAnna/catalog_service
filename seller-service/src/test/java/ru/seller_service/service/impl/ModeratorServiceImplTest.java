package ru.seller_service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import ru.seller_service.client.UserGrpcClient;
import ru.seller_service.exception.ResourceConflictException;
import ru.seller_service.mapper.EventMapper;
import ru.seller_service.mapper.SellerApplicationMapperImpl;
import ru.seller_service.mapper.SellerMapperImpl;
import ru.seller_service.model.Seller;
import ru.seller_service.model.SellerApplication;
import ru.seller_service.model.status.SellerApplicationStatus;
import ru.seller_service.model.status.SellerApplicationType;
import ru.seller_service.model.status.SellerStatus;
import ru.seller_service.outbox.OutboxEvent;
import ru.seller_service.outbox.OutboxRepository;
import ru.seller_service.repository.SellerApplicationRepository;
import ru.seller_service.repository.SellerRepository;
import ru.seller_service.security.CurrentUserService;
import ru.seller_service.dto.request.application.ReviewSellerApplicationRequest;
import ru.seller_service.exception.ResourceForbiddenException;

import java.sql.SQLException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Уникальность sellerName при одобрении REGISTRATION (change seller-service-error-handling).
 */
@ExtendWith(MockitoExtension.class)
class ModeratorServiceImplTest {

    private static final Long APPLICATION_ID = 42L;
    private static final String USER_ID = "kc-user-1";

    @Mock
    private SellerApplicationRepository sellerApplicationRepo;
    @Mock
    private SellerRepository sellerRepo;
    @Mock
    private UserGrpcClient userGrpcClient;
    @Mock
    private OutboxRepository outboxRepo;
    @Mock
    private CurrentUserService currentUserService;

    private static final String MODERATOR_ID = "kc-moderator-1";

    private ModeratorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ModeratorServiceImpl(
                sellerApplicationRepo,
                new SellerApplicationMapperImpl(),
                new SellerMapperImpl(),
                sellerRepo,
                userGrpcClient,
                outboxRepo,
                new EventMapper(new ObjectMapper()),
                currentUserService
        );
        lenient().when(currentUserService.getUserId()).thenReturn(MODERATOR_ID);
        SellerApplication application = SellerApplication.builder()
                .id(APPLICATION_ID)
                .keycloakUserId(USER_ID)
                .sellerName("Shop")
                .inn("1234567890")
                .phoneNumber("+79990000000")
                .email("shop@example.com")
                .paymentAccount("12345678901234567890")
                .sellerApplicationStatus(SellerApplicationStatus.PENDING)
                .sellerApplicationType(SellerApplicationType.REGISTRATION)
                .build();
        lenient().when(sellerApplicationRepo.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
    }

    @Test
    @DisplayName("Одобрение UPDATE переносит телефон, SELLER_APPROVED содержит email заявки")
    void approveUpdate_transfersPhoneAndEmail() throws Exception {
        SellerApplication update = SellerApplication.builder()
                .id(43L)
                .keycloakUserId(USER_ID)
                .sellerName("Shop")
                .inn("1234567890")
                .phoneNumber("+79990001122")
                .email("shop@example.com")
                .paymentAccount("09876543210987654321")
                .sellerApplicationStatus(SellerApplicationStatus.PENDING)
                .sellerApplicationType(SellerApplicationType.UPDATE)
                .build();
        Seller seller = new Seller();
        seller.setKeycloakUserId(USER_ID);
        seller.setSellerName("Shop");
        seller.setPhoneNumber("+79990000000");
        seller.setEmail("shop@example.com");
        seller.setPaymentAccount("12345678901234567890");
        seller.setSellerStatus(SellerStatus.ACTIVE);
        when(sellerApplicationRepo.findById(43L)).thenReturn(Optional.of(update));
        when(sellerRepo.findByKeycloakUserId(USER_ID)).thenReturn(Optional.of(seller));

        service.approve(43L);

        assertThat(seller.getPhoneNumber()).isEqualTo("+79990001122");
        assertThat(seller.getPaymentAccount()).isEqualTo("09876543210987654321");
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("SELLER_APPROVED");
        assertThat(new ObjectMapper().readTree(captor.getValue().getPayload()).get("email").asText())
                .isEqualTo("shop@example.com");
        assertThat(new ObjectMapper().readTree(captor.getValue().getPayload()).path("applicationType").asText())
                .isEqualTo("UPDATE");
        verifyNoInteractions(userGrpcClient);
    }

    private static DataIntegrityViolationException violation(String sqlState, String constraint) {
        return new DataIntegrityViolationException("could not execute statement",
                new SQLException("ERROR: duplicate key value violates unique constraint \"" + constraint + "\"", sqlState));
    }

    @Test
    @DisplayName("Успешное одобрение: Seller сохраняется через saveAndFlush до gRPC")
    void approve_success() throws Exception {
        service.approve(APPLICATION_ID);

        verify(sellerRepo).existsSameName("Shop");
        verify(sellerRepo).saveAndFlush(any(Seller.class));
        verify(userGrpcClient).assignSellerRole(USER_ID);
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(new ObjectMapper().readTree(captor.getValue().getPayload()).path("applicationType").asText())
                .isEqualTo("REGISTRATION");
    }

    @Test
    @DisplayName("Имя занято: 409 до записи Seller и без gRPC")
    void approve_nameTaken_conflict() {
        when(sellerRepo.existsSameName("Shop")).thenReturn(true);

        assertThatThrownBy(() -> service.approve(APPLICATION_ID))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Seller with name [Shop] already exists.");
        verify(sellerRepo, never()).saveAndFlush(any());
        verify(sellerRepo, never()).save(any());
        verifyNoInteractions(userGrpcClient, outboxRepo);
    }

    @Test
    @DisplayName("Гонка: нарушение uq_sellers_seller_name дает 409 без gRPC")
    void approve_functionalIndexViolation_conflict() {
        when(sellerRepo.saveAndFlush(any(Seller.class)))
                .thenThrow(violation("23505", "uq_sellers_seller_name"));

        assertThatThrownBy(() -> service.approve(APPLICATION_ID))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Seller with name [Shop] already exists.");
        verifyNoInteractions(userGrpcClient, outboxRepo);
    }

    @Test
    @DisplayName("Гонка: нарушение UNIQUE (seller_name) Hibernate дает 409 без gRPC")
    void approve_hibernateConstraintViolation_conflict() {
        when(sellerRepo.saveAndFlush(any(Seller.class)))
                .thenThrow(violation("23505", "uk8757g69e614binmjpvpoeda7d"));

        assertThatThrownBy(() -> service.approve(APPLICATION_ID))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Seller with name [Shop] already exists.");
        verifyNoInteractions(userGrpcClient, outboxRepo);
    }

    @Test
    @DisplayName("Другое ограничение (inn) пробрасывается без изменений")
    void approve_otherConstraint_rethrown() {
        DataIntegrityViolationException innViolation = violation("23505", "ukpiumhdqb6mypa86tshmed7cuj");
        when(sellerRepo.saveAndFlush(any(Seller.class))).thenThrow(innViolation);

        assertThatThrownBy(() -> service.approve(APPLICATION_ID)).isSameAs(innViolation);
        verifyNoInteractions(userGrpcClient, outboxRepo);
    }

    @Test
    @DisplayName("Другой SQLState (not null) пробрасывается без изменений")
    void approve_otherSqlState_rethrown() {
        DataIntegrityViolationException notNull = violation("23502", "uq_sellers_seller_name");
        when(sellerRepo.saveAndFlush(any(Seller.class))).thenThrow(notNull);

        assertThatThrownBy(() -> service.approve(APPLICATION_ID)).isSameAs(notNull);
        assertThat(notNull.getCause()).isInstanceOf(SQLException.class);
        verifyNoInteractions(userGrpcClient, outboxRepo);
    }
    // --- Правила модерации (change seller-moderation-rules) ---

    @Test
    @DisplayName("Одобрение своей заявки: 403, ничего не меняется")
    void approve_ownApplication_forbidden() {
        when(currentUserService.getUserId()).thenReturn(USER_ID);

        assertThatThrownBy(() -> service.approve(APPLICATION_ID))
                .isInstanceOf(ResourceForbiddenException.class)
                .hasMessage("Moderator can't review own application.");
        verify(sellerRepo, never()).saveAndFlush(any());
        verify(sellerApplicationRepo, never()).save(any());
        verifyNoInteractions(userGrpcClient, outboxRepo);
    }

    @Test
    @DisplayName("Отклонение своей заявки: 403, статус и комментарий не меняются")
    void reject_ownApplication_forbidden() {
        when(currentUserService.getUserId()).thenReturn(USER_ID);
        SellerApplication application = sellerApplicationRepo.findById(APPLICATION_ID).orElseThrow();

        assertThatThrownBy(() -> service.reject(APPLICATION_ID, new ReviewSellerApplicationRequest("Bad data")))
                .isInstanceOf(ResourceForbiddenException.class);
        assertThat(application.getSellerApplicationStatus()).isEqualTo(SellerApplicationStatus.PENDING);
        assertThat(application.getModeratorComment()).isNull();
        verifyNoInteractions(outboxRepo);
    }

    @Test
    @DisplayName("Своя уже рассмотренная заявка: 403, а не 409")
    void approve_ownReviewedApplication_forbiddenNotConflict() {
        when(currentUserService.getUserId()).thenReturn(USER_ID);
        sellerApplicationRepo.findById(APPLICATION_ID).orElseThrow()
                .setSellerApplicationStatus(SellerApplicationStatus.REJECTED);

        assertThatThrownBy(() -> service.approve(APPLICATION_ID))
                .isInstanceOf(ResourceForbiddenException.class);
    }

    @Test
    @DisplayName("Отклонение чужой заявки: REJECTED, причина сохранена и попадает в событие")
    void reject_foreignApplication_rejectedWithReason() throws Exception {
        SellerApplication application = sellerApplicationRepo.findById(APPLICATION_ID).orElseThrow();

        service.reject(APPLICATION_ID, new ReviewSellerApplicationRequest("Invalid INN"));

        assertThat(application.getSellerApplicationStatus()).isEqualTo(SellerApplicationStatus.REJECTED);
        assertThat(application.getModeratorComment()).isEqualTo("Invalid INN");
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("SELLER_REJECTED");
        assertThat(new ObjectMapper().readTree(captor.getValue().getPayload()).get("moderatorComment").asText())
                .isEqualTo("Invalid INN");
    }
}
