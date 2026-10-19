package ru.seller_service.dto.request.store;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record StoreRequest(
        @NotBlank
        @Size(max = 100)
        String storeName,

        @Size(max = 255, message = "Description can't exceed 255 characters.")
        String description,

        @NotBlank(message = "Email is required.")
        @Email
        String email,

        @NotBlank(message = "Phone is required.")
        @Pattern(regexp = "^(\\+7|8)\\d{10}$",
                message = "Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX.")
        String phoneNumber) {

}
