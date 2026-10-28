package es.uvigo.dagss.recetas.dto;

import static es.uvigo.dagss.recetas.validation.ValidationRules.*;

import es.uvigo.dagss.recetas.entities.Pharmacy;
import es.uvigo.dagss.recetas.validation.InputNormalizer;
import es.uvigo.dagss.recetas.validation.TaxId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** The initial password of a new pharmacy is the pharmacist's license number, so it is stored upper-cased. */
public record PharmacyDTO(
        Long id,
        @NotBlank(message = "El nombre del establecimiento es obligatorio")
        @Size(max = 100, message = "El nombre del establecimiento no puede superar los 100 caracteres") String name,
        @NotBlank(message = "El nombre del farmacéutico es obligatorio")
        @Pattern(regexp = PERSON_NAME_REGEX, message = NAME_INVALID) String pharmacistFirstName,
        @NotBlank(message = "Los apellidos del farmacéutico son obligatorios")
        @Pattern(regexp = PERSON_NAME_REGEX, message = LAST_NAME_INVALID) String pharmacistLastName,
        @NotBlank(message = TAX_ID_REQUIRED) @TaxId String taxId,
        @NotBlank(message = LICENSE_REQUIRED)
        @Pattern(regexp = LICENSE_NUMBER_REGEX, message = LICENSE_INVALID) String pharmacistLicenseNumber,
        @NotNull(message = "El domicilio es obligatorio") @Valid AddressDTO address,
        @NotBlank(message = PHONE_REQUIRED)
        @Pattern(regexp = PHONE_REGEX, message = PHONE_INVALID) String phone,
        @NotBlank(message = EMAIL_REQUIRED)
        @Pattern(regexp = EMAIL_REGEX, message = EMAIL_INVALID) String email,
        @NotBlank(message = USERNAME_REQUIRED)
        @Pattern(regexp = USERNAME_REGEX, message = USERNAME_INVALID) String username,
        boolean active) {

    public PharmacyDTO {
        name = InputNormalizer.trim(name);
        pharmacistFirstName = InputNormalizer.trim(pharmacistFirstName);
        pharmacistLastName = InputNormalizer.trim(pharmacistLastName);
        taxId = InputNormalizer.upper(taxId);
        pharmacistLicenseNumber = InputNormalizer.upper(pharmacistLicenseNumber);
        phone = InputNormalizer.trim(phone);
        email = InputNormalizer.trim(email);
        username = InputNormalizer.trim(username);
    }

    public static PharmacyDTO from(Pharmacy f) {
        return new PharmacyDTO(f.getId(), f.getName(), f.getPharmacistFirstName(), f.getPharmacistLastName(),
                f.getTaxId(), f.getPharmacistLicenseNumber(), AddressDTO.from(f.getAddress()),
                f.getPhone(), f.getEmail(), f.getUsername(), Boolean.TRUE.equals(f.getActive()));
    }
}
