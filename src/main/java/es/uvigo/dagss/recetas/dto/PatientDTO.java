package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import java.time.LocalDate;

import es.uvigo.dagss.recetas.entities.Patient;
import es.uvigo.dagss.recetas.validation.BirthDate;
import es.uvigo.dagss.recetas.validation.InputNormalizer;
import es.uvigo.dagss.recetas.validation.NationalId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The initial password of a new patient is their DNI, so it is stored upper-cased. */
public record PatientDTO(
        Long id,
        @NotBlank(message = NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String name,
        @NotBlank(message = LAST_NAME_REQUIRED)
        @Pattern(regexp = PERSON_NAME_REGEX, message = LAST_NAME_INVALID) String lastName,
        @NotBlank(message = NATIONAL_ID_REQUIRED) @NationalId String nationalId,
        @NotBlank(message = HEALTH_CARD_REQUIRED)
        @Pattern(regexp = HEALTH_CARD_REGEX, message = HEALTH_CARD_INVALID) String healthCardNumber,
        @NotBlank(message = SOCIAL_SECURITY_REQUIRED)
        @Pattern(regexp = SOCIAL_SECURITY_REGEX, message = SOCIAL_SECURITY_INVALID) String socialSecurityNumber,
        @NotNull(message = "El domicilio es obligatorio") @Valid AddressDTO address,
        @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_REGEX, message = PHONE_INVALID) String phone,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email,
        @NotNull(message = BIRTH_DATE_REQUIRED) @BirthDate LocalDate birthDate,
        @NotNull(message = "El centro de salud es obligatorio") Long healthCenterId,
        HealthCenterSummaryDTO healthCenter,
        @NotNull(message = "El médico asignado es obligatorio") Long doctorId,
        DoctorSummaryDTO doctor,
        @NotBlank(message = USERNAME_REQUIRED)
        @Pattern(regexp = USERNAME_REGEX, message = USERNAME_INVALID) String username,
        boolean active) {

    public PatientDTO {
        name = InputNormalizer.trim(name);
        lastName = InputNormalizer.trim(lastName);
        nationalId = InputNormalizer.upper(nationalId);
        healthCardNumber = InputNormalizer.upper(healthCardNumber);
        socialSecurityNumber = InputNormalizer.trim(socialSecurityNumber);
        phone = InputNormalizer.trim(phone);
        email = InputNormalizer.trim(email);
        username = InputNormalizer.trim(username);
    }

    public static PatientDTO from(Patient p) {
        return new PatientDTO(p.getId(), p.getName(), p.getLastName(), p.getNationalId(),
                p.getHealthCardNumber(), p.getSocialSecurityNumber(), AddressDTO.from(p.getAddress()),
                p.getPhone(), p.getEmail(), p.getBirthDate(),
                p.getHealthCenter() != null ? p.getHealthCenter().getId() : null,
                HealthCenterSummaryDTO.from(p.getHealthCenter()),
                p.getDoctor() != null ? p.getDoctor().getId() : null,
                DoctorSummaryDTO.from(p.getDoctor()),
                p.getUsername(), Boolean.TRUE.equals(p.getActive()));
    }
}
