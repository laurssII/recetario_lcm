package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.validation.InputNormalizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PharmacyProfileUpdateRequest(
        @NotBlank(message = "El nombre del establecimiento es obligatorio")
        @Size(max = 100, message = "El nombre del establecimiento no puede superar los 100 caracteres") String name,
        @NotBlank(message = "El nombre del farmacéutico es obligatorio")
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String pharmacistFirstName,
        @NotBlank(message = "Los apellidos del farmacéutico son obligatorios")
        @Pattern(regexp = PERSON_NAME_REGEX, message = LAST_NAME_INVALID) String pharmacistLastName,
        @NotNull(message = "El domicilio es obligatorio") @Valid AddressDTO address,
        @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_REGEX, message = PHONE_INVALID) String phone,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email) {

    public PharmacyProfileUpdateRequest {
        name = InputNormalizer.trim(name);
        pharmacistFirstName = InputNormalizer.trim(pharmacistFirstName);
        pharmacistLastName = InputNormalizer.trim(pharmacistLastName);
        phone = InputNormalizer.trim(phone);
        email = InputNormalizer.trim(email);
    }
}
