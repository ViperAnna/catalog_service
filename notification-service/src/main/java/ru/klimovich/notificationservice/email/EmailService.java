package ru.klimovich.notificationservice.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import ru.klimovich.notificationservice.event.UserCreatedEvent;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String from;

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

        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.error("Unable to send email", e);
        }
    }
}
