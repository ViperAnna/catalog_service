package ru.klimovich.notificationservice.event;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {

    private String userId;
    private String keycloakUserId;
    private String email;
    private String username;

}

