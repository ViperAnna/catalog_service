package ru.klimovich.user_service.event;

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
