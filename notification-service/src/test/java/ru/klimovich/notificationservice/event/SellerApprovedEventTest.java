package ru.klimovich.notificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Requirement "seller-events-topic message contract": consumer допускает payload SELLER_APPROVED
 * без applicationType (change seller-approval-notification-types).
 */
class SellerApprovedEventTest {

    /** Та же конфигурация, что у ObjectMapper Spring Boot: FAIL_ON_UNKNOWN_PROPERTIES отключен. */
    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();

    @Test
    void payloadWithoutApplicationType_parsed() throws Exception {
        SellerApprovedEvent event = objectMapper.readValue(
                "{\"applicationId\":7,\"keycloakUserId\":\"kc-1\",\"sellerName\":\"Shop\",\"email\":\"shop@example.com\"}",
                SellerApprovedEvent.class);

        assertThat(event.getApplicationType()).isNull();
        assertThat(event.getEmail()).isEqualTo("shop@example.com");
    }

    @Test
    void payloadWithApplicationTypeAndUnknownField_parsed() throws Exception {
        SellerApprovedEvent event = objectMapper.readValue(
                "{\"applicationId\":7,\"sellerName\":\"Shop\",\"email\":\"shop@example.com\","
                        + "\"applicationType\":\"DELETION\",\"futureField\":1}",
                SellerApprovedEvent.class);

        assertThat(event.getApplicationType()).isEqualTo("DELETION");
    }
}
