package es.uvigo.dagss.recetas.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import es.uvigo.dagss.recetas.daos.AdministratorDAO;
import es.uvigo.dagss.recetas.daos.DoctorDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.daos.PharmacyDAO;
import es.uvigo.dagss.recetas.exceptions.DuplicateResourceException;

class UniquenessCheckerTest {

    private final AdministratorDAO administratorDAO = mock(AdministratorDAO.class);
    private final DoctorDAO doctorDAO = mock(DoctorDAO.class);
    private final PatientDAO patientDAO = mock(PatientDAO.class);
    private final PharmacyDAO pharmacyDAO = mock(PharmacyDAO.class);
    private final UniquenessChecker checker = new UniquenessChecker(administratorDAO, doctorDAO, patientDAO, pharmacyDAO);

    /** Runs the action and returns the duplicate error it throws (fails if it does not throw one). */
    private static DuplicateResourceException duplicate(Runnable action) {
        try {
            action.run();
        } catch (DuplicateResourceException e) {
            return e;
        }
        throw new AssertionError("A DuplicateResourceException was expected");
    }

    @Test
    void aFreeUsernameIsAccepted() {
        assertThatCode(() -> checker.checkUsername("nuevo", null)).doesNotThrowAnyException();
    }

    @Test
    void aUsernameTakenByAnyRoleIsRejected() {
        when(pharmacyDAO.existsByUsernameIgnoreCase("compostela")).thenReturn(true);
        DuplicateResourceException e = duplicate(() -> checker.checkUsername("compostela", null));
        assertThat(e.getField()).isEqualTo("username");
        assertThat(e.getMessage()).isEqualTo(UniquenessChecker.USERNAME_TAKEN);

        when(administratorDAO.existsByUsernameIgnoreCase("admin")).thenReturn(true);
        assertThat(duplicate(() -> checker.checkUsername("admin", null)).getField()).isEqualTo("username");
    }

    @Test
    void whenUpdatingARecordDoesNotClashWithItself() {
        when(patientDAO.existsByUsernameIgnoreCase("xrodriguez")).thenReturn(true);                 // it is taken...
        when(patientDAO.existsByUsernameIgnoreCaseAndIdNot("xrodriguez", 5L)).thenReturn(false);    // ...but only by id 5
        assertThatCode(() -> checker.checkUsername("xrodriguez", 5L)).doesNotThrowAnyException();
        assertThat(duplicate(() -> checker.checkUsername("xrodriguez", null)).getField()).isEqualTo("username");
    }

    @Test
    void patientDniAndHealthCardAreCheckedSeparately() {
        when(patientDAO.existsByNationalId("20000001Y")).thenReturn(true);
        assertThat(duplicate(() -> checker.checkPatient("20000001Y", "AAAA0000000001", null)).getField())
                .isEqualTo("nationalId");

        when(patientDAO.existsByHealthCardNumber("RIXO8504120001")).thenReturn(true);
        assertThat(duplicate(() -> checker.checkPatient("20000009Z", "RIXO8504120001", null)).getField())
                .isEqualTo("healthCardNumber");
    }

    @Test
    void doctorAndPharmacyIdentifiersAreChecked() {
        when(doctorDAO.existsByLicenseNumberAndIdNot("LIC-4501", 7L)).thenReturn(true);
        assertThat(duplicate(() -> checker.checkDoctor("10000001S", "LIC-4501", 7L)).getField())
                .isEqualTo("licenseNumber");

        when(pharmacyDAO.existsByTaxId("30000001C")).thenReturn(true);
        assertThat(duplicate(() -> checker.checkPharmacy("30000001C", "PLIC-1", null)).getField())
                .isEqualTo("taxId");
    }
}
