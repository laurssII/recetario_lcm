package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.validation.InputNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Excludes healthCenterId: a doctor cannot change their own health center. */
public record DoctorProfileUpdateRequest(
        @NotBlank(message = NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String name,
        @NotBlank(message = LAST_NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = LAST_NAME_INVALID) String lastName,
        @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_REGEX, message = PHONE_INVALID) String phone,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email) {

    public DoctorProfileUpdateRequest {
        name = InputNormalizer.trim(name);
        lastName = InputNormalizer.trim(lastName);
        phone = InputNormalizer.trim(phone);
        email = InputNormalizer.trim(email);
    }
}
