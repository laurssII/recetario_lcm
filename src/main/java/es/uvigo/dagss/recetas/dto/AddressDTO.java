package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.entities.Address;
import es.uvigo.dagss.recetas.validation.ConsistentAddress;
import es.uvigo.dagss.recetas.validation.InputNormalizer;
import es.uvigo.dagss.recetas.validation.SpanishProvince;
import es.uvigo.dagss.recetas.validation.SpanishProvinces;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * The postal code is a String to keep leading zeros (04xxx). The province must be a real
 * Spanish province and the postal code must belong to it.
 */
@ConsistentAddress
public record AddressDTO(
        @NotBlank(message = STREET_REQUIRED)
        @Pattern(regexp = STREET_REGEX, message = STREET_INVALID) String street,
        @NotBlank(message = CITY_REQUIRED)
        @Pattern(regexp = CITY_REGEX, message = CITY_INVALID) String city,
        @NotBlank(message = POSTAL_CODE_REQUIRED)
        @Pattern(regexp = POSTAL_CODE_REGEX, message = POSTAL_CODE_INVALID) String postalCode,
        @NotBlank(message = PROVINCE_REQUIRED)
        @SpanishProvince String province) {

    public AddressDTO {
        street = InputNormalizer.trim(street);
        city = InputNormalizer.trim(city);
        postalCode = InputNormalizer.trim(postalCode);
        province = InputNormalizer.trim(province);
    }

    public static AddressDTO from(Address a) {
        if (a == null) return null;
        return new AddressDTO(a.getStreet(), a.getCity(), a.getPostalCode(), a.getProvince());
    }

    /** The province is stored with its canonical spelling ("la coruña" -> "A Coruña"). */
    public Address toEntity() {
        return new Address(street, city, postalCode, SpanishProvinces.canonicalNameOrSame(province));
    }
}
