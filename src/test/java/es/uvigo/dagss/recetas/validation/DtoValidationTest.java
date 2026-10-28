package es.uvigo.dagss.recetas.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import es.uvigo.dagss.recetas.dto.AddressDTO;
import es.uvigo.dagss.recetas.dto.DoctorDTO;
import es.uvigo.dagss.recetas.dto.MedicationDTO;
import es.uvigo.dagss.recetas.dto.NewAdministratorRequest;
import es.uvigo.dagss.recetas.dto.PasswordChangeRequest;
import es.uvigo.dagss.recetas.dto.PatientDTO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

/**
 * Checks the input rules documented in doc/validaciones.md on the DTOs, using
 * the real Bean Validation engine (no Spring context needed).
 */
class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static AddressDTO address(String postalCode, String province) {
        return new AddressDTO("20 Principe Street", "Vigo", postalCode, province);
    }

    private static PatientDTO patient(String nationalId, String healthCard, String socialSecurity, String phone,
                                      String email, LocalDate birthDate, AddressDTO address) {
        return new PatientDTO(null, "Xoan", "Rodriguez Iglesias", nationalId, healthCard, socialSecurity, address,
                phone, email, birthDate, 1L, null, 2L, null, "xrodriguez", true);
    }

    private static PatientDTO validPatient() {
        return patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "xrodriguez@example.com",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra"));
    }

    private static Set<String> messagesOn(Object dto, String propertyPath) {
        return validator.validate(dto).stream()
                .filter(v -> v.getPropertyPath().toString().equals(propertyPath))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void shouldAcceptAValidPatient() {
        assertThat(validator.validate(validPatient())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "1234567890", "586123456", "98612345a", "986 123 456", "+34986123456"})
    void shouldRejectPhonesThatAreNotNineDigitsStartingBySixToNine(String phone) {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", phone, "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra"));
        assertThat(messagesOn(dto, "phone")).containsExactly(ValidationRules.PHONE_INVALID);
    }

    @Test
    void shouldReportABlankPhoneOnlyAsRequired() {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "   ", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra"));
        assertThat(messagesOn(dto, "phone")).containsExactly(ValidationRules.PHONE_REQUIRED);
    }

    @Test
    void shouldCheckTheDniControlLetterAndAcceptNie() {
        assertThat(messagesOn(patient("12345678A", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra")), "nationalId"))
                .containsExactly(ValidationRules.NATIONAL_ID_INVALID);
        assertThat(messagesOn(patient("12345678z", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra")), "nationalId")).isEmpty();
        assertThat(messagesOn(patient("X1234567L", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra")), "nationalId")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"HC-000101", "ABC1234567890", "ABCD123456789", "1234ABCD567890"})
    void shouldRejectMalformedHealthCardNumbers(String healthCard) {
        PatientDTO dto = patient("20000001Y", healthCard, "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra"));
        assertThat(messagesOn(dto, "healthCardNumber")).containsExactly(ValidationRules.HEALTH_CARD_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678901", "1234567890123", "SSN-000101"})
    void shouldRejectMalformedSocialSecurityNumbers(String socialSecurity) {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", socialSecurity, "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra"));
        assertThat(messagesOn(dto, "socialSecurityNumber")).containsExactly(ValidationRules.SOCIAL_SECURITY_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@b", "sin-arroba", "a@b.c", "a b@c.es", "@dominio.es"})
    void shouldRejectInvalidEmails(String email) {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", email,
                LocalDate.of(1985, 4, 12), address("36202", "Pontevedra"));
        assertThat(messagesOn(dto, "email")).containsExactly(ValidationRules.EMAIL_INVALID);
    }

    @Test
    void shouldRejectBirthDatesInTheFutureOrBefore1900() {
        for (LocalDate invalid : new LocalDate[] {LocalDate.now().plusDays(1), LocalDate.of(1899, 12, 31)}) {
            PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                    invalid, address("36202", "Pontevedra"));
            assertThat(messagesOn(dto, "birthDate")).containsExactly(ValidationRules.BIRTH_DATE_INVALID);
        }
        PatientDTO today = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.now(), address("36202", "Pontevedra"));
        assertThat(messagesOn(today, "birthDate")).isEmpty();
    }

    @Test
    void shouldRejectNamesWithDigitsAndReportBlankNamesAsRequired() {
        PatientDTO withDigit = new PatientDTO(null, "J0hn", "Garcia", "20000001Y", "RIXO8504120001", "360000010101",
                address("36202", "Pontevedra"), "986200001", "a@b.es", LocalDate.of(1985, 4, 12), 1L, null, 2L, null,
                "jgarcia", true);
        assertThat(messagesOn(withDigit, "name")).containsExactly(ValidationRules.NAME_INVALID);

        PatientDTO blank = new PatientDTO(null, "  ", "Garcia", "20000001Y", "RIXO8504120001", "360000010101",
                address("36202", "Pontevedra"), "986200001", "a@b.es", LocalDate.of(1985, 4, 12), 1L, null, 2L, null,
                "jgarcia", true);
        assertThat(messagesOn(blank, "name")).containsExactly(ValidationRules.NAME_REQUIRED);
    }

    @Test
    void shouldRequireTheMandatoryAssociations() {
        PatientDTO dto = new PatientDTO(null, "Xoan", "Rodriguez", "20000001Y", "RIXO8504120001", "360000010101",
                null, "986200001", "a@b.es", LocalDate.of(1985, 4, 12), null, null, null, null, "xrodriguez", true);
        assertThat(messagesOn(dto, "address")).isNotEmpty();
        assertThat(messagesOn(dto, "healthCenterId")).isNotEmpty();
        assertThat(messagesOn(dto, "doctorId")).isNotEmpty();
    }

    @Test
    void shouldRejectProvincesThatAreNotSpanishProvinces() {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Atlantis"));
        assertThat(messagesOn(dto, "address.province")).containsExactly(ValidationRules.PROVINCE_INVALID);
    }

    @Test
    void shouldAcceptAlternativeSpellingsOfAProvince() {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("15001", "la coruña"));
        assertThat(validator.validate(dto)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"3620", "362021", "3620a", "00001", "53001", "36 202"})
    void shouldRejectMalformedPostalCodes(String postalCode) {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address(postalCode, "Pontevedra"));
        assertThat(messagesOn(dto, "address.postalCode")).contains(ValidationRules.POSTAL_CODE_INVALID);
    }

    @Test
    void shouldKeepLeadingZerosInPostalCodes() {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("04001", "Almería"));
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void shouldRejectAPostalCodeThatDoesNotBelongToTheProvince() {
        PatientDTO dto = patient("20000001Y", "RIXO8504120001", "360000010101", "986200001", "a@b.es",
                LocalDate.of(1985, 4, 12), address("36202", "Madrid"));
        assertThat(messagesOn(dto, "address.postalCode"))
                .containsExactly("El código postal 36202 no corresponde a la provincia de Madrid (debería empezar por 28)");
    }

    @Test
    void shouldCheckTheDoctorLicenseNumber() {
        DoctorDTO valid = new DoctorDTO(null, "Laura", "Fernandez Perez", "10000001S", "lic-4501", "986100001",
                "laura@healthsystem.example", 1L, null, "lfernandez", true);
        assertThat(validator.validate(valid)).isEmpty();
        assertThat(valid.licenseNumber()).isEqualTo("LIC-4501"); // normalized: it is the initial password

        DoctorDTO invalid = new DoctorDTO(null, "Laura", "Fernandez Perez", "10000001S", "A B", "986100001",
                "laura@healthsystem.example", 1L, null, "lfernandez", true);
        assertThat(messagesOn(invalid, "licenseNumber")).containsExactly(ValidationRules.LICENSE_INVALID);
    }

    @Test
    void shouldCheckMedicationFields() {
        MedicationDTO valid = new MedicationDTO(null, "Ibuprofen Cinfa 600mg", "Ibuprofen", "Cinfa", "Anti-inflammatories", 30, true);
        assertThat(validator.validate(valid)).isEmpty();

        MedicationDTO noUnits = new MedicationDTO(null, "Ibuprofen", "Ibuprofen", "Cinfa", "Analgesics", 0, true);
        assertThat(messagesOn(noUnits, "unitsPerPackage")).isNotEmpty();
        MedicationDTO tooMany = new MedicationDTO(null, "Ibuprofen", "Ibuprofen", "Cinfa", "Analgesics", 1001, true);
        assertThat(messagesOn(tooMany, "unitsPerPackage")).isNotEmpty();
        MedicationDTO noManufacturer = new MedicationDTO(null, "Ibuprofen", "Ibuprofen", " ", "Analgesics", 30, true);
        assertThat(messagesOn(noManufacturer, "manufacturer")).isNotEmpty();
    }

    @Test
    void shouldRequireAtLeastFourCharactersInANewPassword() {
        assertThat(messagesOn(new PasswordChangeRequest("old", "abc"), "newPassword"))
                .containsExactly(ValidationRules.PASSWORD_INVALID);
        assertThat(validator.validate(new PasswordChangeRequest("old", "abcd"))).isEmpty();
    }

    @Test
    void shouldRequireAPasswordWhenCreatingAnAdministrator() {
        assertThat(validator.validate(new NewAdministratorRequest("Ana Gil", "ana@example.com", "anagil", "clave-1"))).isEmpty();
        assertThat(messagesOn(new NewAdministratorRequest("Ana Gil", "ana@example.com", "anagil", null), "password"))
                .containsExactly(ValidationRules.PASSWORD_REQUIRED);
        assertThat(messagesOn(new NewAdministratorRequest("Ana Gil", "ana@example.com", "anagil", "abc"), "password"))
                .containsExactly(ValidationRules.PASSWORD_INVALID);
    }
}
