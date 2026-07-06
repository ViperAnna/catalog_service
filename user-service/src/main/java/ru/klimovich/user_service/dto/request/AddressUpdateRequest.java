package ru.klimovich.user_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class AddressUpdateRequest {

    @NotBlank(message = "country is required.")
    @Size(max = 20, message = "country can't exceed 20 characters.")
    private String country;

    @NotBlank(message = "city is required.")
    @Size(max = 20, message = "city can't exceed 20 characters.")
    private String city;

    @NotBlank(message = "street is required.")
    @Size(max = 20, message = "street can't exceed 20 characters.")
    private String street;

    @NotBlank(message = "numberOfHouse is required.")
    @Size(max = 20, message = "numberOfHouse can't exceed 20 characters.")
    private String numberOfHouse;

    @NotBlank(message = "numberOfApartment is required.")
    @Size(max = 20, message = "numberOfApartment can't exceed 20 characters.")
    private String numberOfApartment;

    @NotBlank(message = "postalCode is required.")
    @Size(max = 20, message = "postalCode can't exceed 20 characters.")
    private String postalCode;
}