package ru.klimovich.user_service.dto.responce;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressResponse {
    private String country;
    private String city;
    private String street;
    private String numberOfHouse;
    private String numberOfApartment;
    private String postalCode;
}
