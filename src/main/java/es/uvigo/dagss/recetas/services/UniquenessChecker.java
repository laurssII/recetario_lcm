package es.uvigo.dagss.recetas.services;

import org.springframework.stereotype.Component;

import es.uvigo.dagss.recetas.daos.AdministratorDAO;
import es.uvigo.dagss.recetas.daos.DoctorDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.daos.PharmacyDAO;
import es.uvigo.dagss.recetas.exceptions.DuplicateResourceException;

/**
 * Checks that identifying values are not already in use before a record is
 * created or modified. Every check throws {@link DuplicateResourceException}
 * (HTTP 409) naming the offending field.
 *
 * <ul>
 *   <li><b>Username</b>: unique across <i>all</i> user types and case-insensitive,
 *       because the login looks the user up in the four tables at once.</li>
 *   <li><b>DNI/NIE, tax id, health card number, license number</b>: unique
 *       within their own type (the same person may be a doctor and a patient).</li>
 * </ul>
 *
 * Deactivated records (logical deletion) keep their values reserved, so they
 * count as "in use". The {@code excludingId} parameter is the id of the record
 * being updated (so that it does not clash with itself); use {@code null} when
 * creating. User ids come from one shared generator, so an id identifies a
 * single user across the four tables.
 *
 * <p>This is a check-then-write: two simultaneous requests could both pass it.
 * The unique constraints declared on the entities are the final guard (the
 * resulting {@code DataIntegrityViolationException} is also translated to a 409).
 */
@Component
public class UniquenessChecker {

    public static final String USERNAME_TAKEN =
            "Ya existe un usuario con ese nombre de usuario (puede pertenecer a un usuario dado de baja)";
    public static final String PATIENT_NATIONAL_ID_TAKEN =
            "Ya existe un paciente con ese DNI/NIE (puede estar dado de baja)";
    public static final String HEALTH_CARD_TAKEN =
            "Ya existe un paciente con ese número de tarjeta sanitaria (puede estar dado de baja)";
    public static final String DOCTOR_NATIONAL_ID_TAKEN =
            "Ya existe un médico con ese DNI/NIE (puede estar dado de baja)";
    public static final String LICENSE_TAKEN =
            "Ya existe un médico con ese número de colegiado (puede estar dado de baja)";
    public static final String TAX_ID_TAKEN =
            "Ya existe una farmacia con ese DNI/NIF (puede estar dada de baja)";
    public static final String PHARMACIST_LICENSE_TAKEN =
            "Ya existe una farmacia con ese número de colegiado del farmacéutico (puede estar dada de baja)";

    private final AdministratorDAO administratorDAO;
    private final DoctorDAO doctorDAO;
    private final PatientDAO patientDAO;
    private final PharmacyDAO pharmacyDAO;

    public UniquenessChecker(AdministratorDAO administratorDAO, DoctorDAO doctorDAO, PatientDAO patientDAO,
                             PharmacyDAO pharmacyDAO) {
        this.administratorDAO = administratorDAO;
        this.doctorDAO = doctorDAO;
        this.patientDAO = patientDAO;
        this.pharmacyDAO = pharmacyDAO;
    }

    public void checkUsername(String username, Long excludingId) {
        boolean taken = excludingId == null
                ? administratorDAO.existsByUsernameIgnoreCase(username)
                        || doctorDAO.existsByUsernameIgnoreCase(username)
                        || patientDAO.existsByUsernameIgnoreCase(username)
                        || pharmacyDAO.existsByUsernameIgnoreCase(username)
                : administratorDAO.existsByUsernameIgnoreCaseAndIdNot(username, excludingId)
                        || doctorDAO.existsByUsernameIgnoreCaseAndIdNot(username, excludingId)
                        || patientDAO.existsByUsernameIgnoreCaseAndIdNot(username, excludingId)
                        || pharmacyDAO.existsByUsernameIgnoreCaseAndIdNot(username, excludingId);
        failIf(taken, "username", USERNAME_TAKEN);
    }

    public void checkPatient(String nationalId, String healthCardNumber, Long excludingId) {
        failIf(excludingId == null ? patientDAO.existsByNationalId(nationalId)
                : patientDAO.existsByNationalIdAndIdNot(nationalId, excludingId), "nationalId", PATIENT_NATIONAL_ID_TAKEN);
        failIf(excludingId == null ? patientDAO.existsByHealthCardNumber(healthCardNumber)
                : patientDAO.existsByHealthCardNumberAndIdNot(healthCardNumber, excludingId),
                "healthCardNumber", HEALTH_CARD_TAKEN);
    }

    public void checkDoctor(String nationalId, String licenseNumber, Long excludingId) {
        failIf(excludingId == null ? doctorDAO.existsByNationalId(nationalId)
                : doctorDAO.existsByNationalIdAndIdNot(nationalId, excludingId), "nationalId", DOCTOR_NATIONAL_ID_TAKEN);
        failIf(excludingId == null ? doctorDAO.existsByLicenseNumber(licenseNumber)
                : doctorDAO.existsByLicenseNumberAndIdNot(licenseNumber, excludingId), "licenseNumber", LICENSE_TAKEN);
    }

    public void checkPharmacy(String taxId, String pharmacistLicenseNumber, Long excludingId) {
        failIf(excludingId == null ? pharmacyDAO.existsByTaxId(taxId)
                : pharmacyDAO.existsByTaxIdAndIdNot(taxId, excludingId), "taxId", TAX_ID_TAKEN);
        failIf(excludingId == null ? pharmacyDAO.existsByPharmacistLicenseNumber(pharmacistLicenseNumber)
                : pharmacyDAO.existsByPharmacistLicenseNumberAndIdNot(pharmacistLicenseNumber, excludingId),
                "pharmacistLicenseNumber", PHARMACIST_LICENSE_TAKEN);
    }

    private static void failIf(boolean condition, String field, String message) {
        if (condition) {
            throw new DuplicateResourceException(field, message);
        }
    }
}
