package ru.klimovich.notificationservice.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import ru.klimovich.notificationservice.event.SellerApprovedEvent;
import ru.klimovich.notificationservice.exception.UnknownApplicationTypeException;
import ru.klimovich.notificationservice.mapper.EventMapper;
import ru.klimovich.notificationservice.service.seller.SellerNotificationService;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * SELLER_APPROVED с неизвестным непустым applicationType не дает письма и уходит в DLT
 * (change seller-approval-notification-types).
 */
@ExtendWith(MockitoExtension.class)
class SellerEventConsumerTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Mock
    private SellerNotificationService notificationService;

    private SellerEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new SellerEventConsumer(notificationService, new EventMapper(objectMapper));
    }

    private String approvedMessage(String applicationType) throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("applicationId", 7);
        payload.put("keycloakUserId", "kc-user-1");
        payload.put("sellerName", "Shop");
        payload.put("email", "shop@example.com");
        if (applicationType != null) {
            payload.put("applicationType", applicationType);
        }
        Map<String, Object> envelope = Map.of(
                "aggregateType", "SELLER_APPLICATION",
                "eventType", "SELLER_APPROVED",
                "payload", objectMapper.writeValueAsString(payload));
        return objectMapper.writeValueAsString(envelope);
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOO", "update", "DELETION "})
    void unknownApplicationType_throwsAndSendsNothing(String applicationType) throws Exception {
        String message = approvedMessage(applicationType);

        assertThatThrownBy(() -> consumer.consume(message))
                .isInstanceOf(UnknownApplicationTypeException.class)
                .hasMessageContaining("7")
                .hasMessageContaining(applicationType);
        verifyNoInteractions(notificationService);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "REGISTRATION", "UPDATE", "DELETION"})
    void knownOrMissingApplicationType_notifiesSeller(String applicationType) throws Exception {
        consumer.consume(approvedMessage(applicationType));

        ArgumentCaptor<SellerApprovedEvent> captor = ArgumentCaptor.forClass(SellerApprovedEvent.class);
        verify(notificationService).notifySellerApproved(captor.capture());
        assertThat(captor.getValue().getApplicationType()).isEqualTo(applicationType);
        assertThat(captor.getValue().getEmail()).isEqualTo("shop@example.com");
    }
}
