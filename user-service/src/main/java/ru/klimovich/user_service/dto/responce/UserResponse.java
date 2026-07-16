package ru.klimovich.user_service.dto.responce;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserResponse {
    private UUID id;
    private String keycloakUserId;
    private String firstName;
    private String lastName;
    private String phone;
    private String nicknameTelegram;
    private String email;
    private AddressResponse address;
}
