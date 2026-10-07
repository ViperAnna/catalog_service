package ru.seller_service.dto.request.seller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SellerRegistrationRequest(
        @NotBlank(message = "Seller name is required.")
        @Size(max = 50, message = "Seller name can't exceed 50 characters.")
         String sellerName,

        @NotBlank(message = "INN is required.")
        @Pattern(
                regexp = "\\d{10}|\\d{12}",
                message = "INN must contain 10 or 12 digits."
        )
        String inn,

        @NotBlank(message = "Phone is required.")
        @Pattern(
                regexp = "^(\\+7|8)\\d{10}$",
                message = "Phone must be in format +7XXXXXXXXXX or 8XXXXXXXXXX."
        )
        String phoneNumber,

        @NotBlank(message = "Email is required.")
        @Email(message = "Invalid email.")
        @Size(max = 100, message = "Email can't exceed 100 characters.")
        String email,

        @NotBlank(message = "Payment account is required.")
        @Pattern(
                regexp = "\\d{20}",
                message = "Payment account must contain exactly 20 digits."
        )
        String paymentAccount
) {
}
