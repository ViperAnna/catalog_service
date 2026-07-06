package ru.klimovich.user_service.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserRequest {

    @NotBlank(message = "Firstname is required.")
    @Size(max = 20, message = "Firstname can't exceed 20 characters.")
    private String firstName;

    @NotBlank(message = "Lastname is required.")
    @Size(max = 20, message = "Lastname can't exceed 20 characters.")
    private String lastName;

    @NotBlank(message = "Phone is required.")
    @Pattern(regexp = "\\+?[0-9]{7,15}",
            message = "Phone must contain 7-15 digits.")
    private String phone;

    @Size(max = 20, message = "NicknameTelegram can't exceed 20 characters.")
    private String nicknameTelegram;

    @NotBlank(message = "Email is required.")
    @Email
    @Size(max = 100, message = "Email can't exceed 100 characters.")
    private String email;

    @NotBlank
    private String password;

    @Valid
    private AddressRequest address;
}