package ru.klimovich.notificationservice.service.seller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.klimovich.notificationservice.event.SellerApplicationCreatedEvent;
import ru.klimovich.notificationservice.event.SellerApprovedEvent;
import ru.klimovich.notificationservice.event.SellerRejectedEvent;
import ru.klimovich.notificationservice.service.email.EmailService;

@Service
@RequiredArgsConstructor
public class SellerNotificationService {
    private final EmailService emailService;



    public void notifyModeratorAboutNewApplication(SellerApplicationCreatedEvent event){
        emailService.sendNewSellerApplicationToModeratorEmail(event);
    }

    public void notifySellerApproved(SellerApprovedEvent event){
        emailService.sendSellerApprovedEmail(event);

    }

    public void  notifySellerRejected(SellerRejectedEvent event){
        emailService.sendSellerRejectedEmail(event);

    }
}
