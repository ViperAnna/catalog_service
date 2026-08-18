package ru.seller_service.dto.request.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerUpdateRequest(

        @NotBlank(message = "Phone is required.")
        @Pattern(
                regexp = "\\+?[0-9]{7,15}",
                message = "Phone must contain 7-15 digits."
        )
        String phone,

        @Email(message = "Invalid email.")
        @Size(max = 100, message = "Email can't exceed 100 characters.")
        String email,

        @NotBlank(message = "Payment account is required.")
        @Pattern(
                regexp = "\\d{20}",
                message = "Payment account must contain exactly 20 digits."
        )
        String paymentAccount) {
}

