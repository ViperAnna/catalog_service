package ru.klimovich.sellerservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


public record StoreRequest(
        //TODO нормальную валидацию написать
        @NotBlank
        @Size(max = 100)
        String storeName,

        @Size(max = 500)
        String description,

        @Email
        String email,

        @Pattern(regexp = "\\+?[0-9]{7,15}")
        String phone) {

}
