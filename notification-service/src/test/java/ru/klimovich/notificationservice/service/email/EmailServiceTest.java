package ru.klimovich.notificationservice.service.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import ru.klimovich.notificationservice.event.SellerApprovedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Requirement "Seller event email notifications": письмо об одобрении зависит от applicationType
 * (change seller-approval-notification-types).
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);
        ReflectionTestUtils.setField(emailService, "from", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "moderatorEmail", "moderator@example.com");
    }

    private SimpleMailMessage sendApproved(String applicationType) {
        SellerApprovedEvent event = new SellerApprovedEvent();
        event.setApplicationId(7L);
        event.setKeycloakUserId("kc-user-1");
        event.setSellerName("Shop");
        event.setEmail("shop@example.com");
        event.setApplicationType(applicationType);

        emailService.sendSellerApprovedEmail(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("shop@example.com");
        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
        return message;
    }

    @Test
    @DisplayName("REGISTRATION: прежнее письмо об одобрении регистрации")
    void registration() {
        SimpleMailMessage message = sendApproved("REGISTRATION");

        assertThat(message.getSubject()).isEqualTo("Seller application approved!");
        assertThat(message.getText())
                .contains("Congratulations, Shop!")
                .contains("Your shop can now be used as a seller shop.");
    }

    @Test
    @DisplayName("UPDATE: письмо об одобрении изменения данных")
    void update() {
        SimpleMailMessage message = sendApproved("UPDATE");

        assertThat(message.getSubject()).isEqualTo("Seller data change approved");
        assertThat(message.getText())
                .contains("Hello, Shop.")
                .contains("Your request to change seller data has been approved.")
                .contains("The new data is now in effect.")
                .doesNotContain("Your shop can now be used");
    }

    @Test
    @DisplayName("DELETION: письмо об одобрении удаления без фразы про магазин")
    void deletion() {
        SimpleMailMessage message = sendApproved("DELETION");

        assertThat(message.getSubject()).isEqualTo("Seller account deletion approved");
        assertThat(message.getText())
                .contains("Hello, Shop.")
                .contains("Your request to delete the seller account has been approved.")
                .contains("Your seller account is now closed.")
                .doesNotContain("Your shop can now be used");
    }

    /** Неизвестный непустой тип сюда не доходит: его отсекает SellerEventConsumer (DLT). */
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("Без типа или с пустым типом: письмо как для REGISTRATION")
    void missingOrBlankType_fallsBackToRegistration(String applicationType) {
        SimpleMailMessage message = sendApproved(applicationType);

        assertThat(message.getSubject()).isEqualTo("Seller application approved!");
        assertThat(message.getText()).contains("Your shop can now be used as a seller shop.");
    }
}
