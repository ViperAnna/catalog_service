package ru.klimovich.user_service.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class AddressRequest {

    @Size(max = 20, message = "Country can't exceed 20 characters.")
    private String country;

    @Size(max = 20, message = "City can't exceed 20 characters.")
    private String city;

    @Size(max = 20, message = "Street can't exceed 20 characters.")
    private String street;

    @Size(max = 20, message = "Number of house can't exceed 20 characters.")
    private String numberOfHouse;

    @Size(max = 20, message = "Number of apartment can't exceed 20 characters.")
    private String numberOfApartment;

    @Size(max = 20, message = "Postal code can't exceed 20 characters.")
    private String postalCode;
}
