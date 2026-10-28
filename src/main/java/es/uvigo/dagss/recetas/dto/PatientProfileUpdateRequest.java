package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.validation.InputNormalizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Excludes healthCenterId and doctorId: a patient cannot change them. */
public record PatientProfileUpdateRequest(
        @NotBlank(message = NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String name,
        @NotBlank(message = LAST_NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = LAST_NAME_INVALID) String lastName,
        @NotNull(message = "El domicilio es obligatorio") @Valid AddressDTO address,
        @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_REGEX, message = PHONE_INVALID) String phone,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email) {

    public PatientProfileUpdateRequest {
        name = InputNormalizer.trim(name);
        lastName = InputNormalizer.trim(lastName);
        phone = InputNormalizer.trim(phone);
        email = InputNormalizer.trim(email);
    }
}
