package ru.klimovich.notificationservice.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.klimovich.notificationservice.event.SellerApplicationCreatedEvent;
import ru.klimovich.notificationservice.event.SellerApprovedEvent;
import ru.klimovich.notificationservice.event.SellerRejectedEvent;
import ru.klimovich.notificationservice.event.UserCreatedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

    @Value("${notification.moderator.email}")
    private String moderatorEmail;


    public void sendWelcomeEmail(UserCreatedEvent event, String telegramLink) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(event.getEmail());
        message.setSubject("Welcome!");
        message.setText("""
                Welcome, %s!
                Thank you for registration!
                We are happy to see you.
                                
                -----------------------
                Connect Telegram:
                %s
                """
                .formatted(event.getUsername(), telegramLink));

        send(message);
    }

    public void sendNewSellerApplicationToModeratorEmail(SellerApplicationCreatedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(moderatorEmail);
        message.setSubject("New application!");
        message.setText("""
                New seller application.
                        
                Seller: %s
                Email: %s
                        
                ------------------
                        
                Please review the application.
                """
                .formatted(event.getSellerName(), event.getEmail())
        );
        send(message, "SELLER_APPLICATION_CREATED", event.getApplicationId());
    }

    public void sendSellerApprovedEmail(
            SellerApprovedEvent event) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(event.getEmail());

        // Без типа или с неизвестным типом остается письмо о регистрации, как до появления applicationType.
        switch (String.valueOf(event.getApplicationType())) {
            case "UPDATE" -> {
                message.setSubject("Seller data change approved");
                message.setText("""
                        Hello, %s.

                        Your request to change seller data has been approved.

                        The new data is now in effect.

                        ------------------

                        Thank you for using our service!
                        """
                        .formatted(event.getSellerName())
                );
            }
            case "UNBLOCK" -> {
                message.setSubject("Seller account unblocked");
                message.setText("""
                        Hello, %s.

                        Your request to unblock the seller account has been approved.

                        You can manage your stores and products again. Stores blocked separately
                        remain blocked until a moderator unblocks them.

                        ------------------

                        Thank you for using our service!
                        """
                        .formatted(event.getSellerName())
                );
            }
            case "DELETION" -> {
                message.setSubject("Seller account deletion approved");
                message.setText("""
                        Hello, %s.

                        Your request to delete the seller account has been approved.

                        Your seller account is now closed.

                        ------------------

                        Thank you for using our service!
                        """
                        .formatted(event.getSellerName())
                );
            }
            default -> {
                message.setSubject("Seller application approved!");
                message.setText("""
                        Congratulations, %s!

                        Your seller application has been approved.

                        Your shop can now be used as a seller shop.

                        ------------------

                        Thank you for using our service!
                        """
                        .formatted(
                                event.getSellerName()
                        )
                );
            }
        }

        send(message, "SELLER_APPROVED", event.getApplicationId());
    }


    public void sendSellerRejectedEmail(
            SellerRejectedEvent event) {

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(event.getEmail());
        message.setSubject("Seller application rejected");

        message.setText("""
                Hello, %s.

                Unfortunately, your seller application has been rejected.

                Reason:
                %s

                """
                .formatted(
                        event.getSellerName(),
                        event.getModeratorComment()
                )
        );

        send(message, "SELLER_REJECTED", event.getApplicationId());
    }

    private void send(SimpleMailMessage message) {
        send(message, "USER_CREATED", null);
    }

    /**
     * Ошибка отправки логируется с типом события и applicationId, без адреса получателя
     * (персональные данные не пишутся в лог).
     */
    private void send(SimpleMailMessage message, String eventType, Long applicationId) {
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Unable to send email for event {} (application {})", eventType, applicationId, e);
        }
    }
}
