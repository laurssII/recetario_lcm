package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.validation.InputNormalizer;
import es.uvigo.dagss.recetas.validation.NationalId;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The initial password of a new doctor is their license number, so it is stored upper-cased. */
public record DoctorDTO(
        Long id,
        @NotBlank(message = NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String name,
        @NotBlank(message = LAST_NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = LAST_NAME_INVALID) String lastName,
        @NotBlank(message = NATIONAL_ID_REQUIRED) @NationalId String nationalId,
        @NotBlank(message = LICENSE_REQUIRED)
        @Pattern(regexp = LICENSE_NUMBER_REGEX, message = LICENSE_INVALID) String licenseNumber,
        @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_REGEX, message = PHONE_INVALID) String phone,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email,
        @NotNull(message = "El centro de salud es obligatorio") Long healthCenterId,
        HealthCenterSummaryDTO healthCenter,
        @NotBlank(message = USERNAME_REQUIRED)
        @Pattern(regexp = USERNAME_REGEX, message = USERNAME_INVALID) String username,
        boolean active) {

    public DoctorDTO {
        name = InputNormalizer.trim(name);
        lastName = InputNormalizer.trim(lastName);
        nationalId = InputNormalizer.upper(nationalId);
        licenseNumber = InputNormalizer.upper(licenseNumber);
        phone = InputNormalizer.trim(phone);
        email = InputNormalizer.trim(email);
        username = InputNormalizer.trim(username);
    }

    public static DoctorDTO from(Doctor d) {
        return new DoctorDTO(d.getId(), d.getName(), d.getLastName(), d.getNationalId(), d.getLicenseNumber(),
                d.getPhone(), d.getEmail(),
                d.getHealthCenter() != null ? d.getHealthCenter().getId() : null,
                HealthCenterSummaryDTO.from(d.getHealthCenter()),
                d.getUsername(), Boolean.TRUE.equals(d.getActive()));
    }
}
