package es.uvigo.dagss.recetas.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import es.uvigo.dagss.recetas.daos.AppointmentDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.daos.PrescriptionDAO;
import es.uvigo.dagss.recetas.entities.Address;
import es.uvigo.dagss.recetas.entities.Appointment;
import es.uvigo.dagss.recetas.entities.AppointmentStatus;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.HealthCenter;
import es.uvigo.dagss.recetas.entities.Patient;
import es.uvigo.dagss.recetas.entities.Prescription;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.exceptions.ForbiddenException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

/** Rules about which user may touch which record. The role-level rules (URL patterns) are covered by SecurityIntegrationTest. */
class AccessPolicyTest {

    private static final long ADMIN = 1, DOCTOR_A = 10, DOCTOR_B = 11, PATIENT_A = 20, PATIENT_B = 21, PHARMACY = 30;

    private final PatientDAO patientDAO = mock(PatientDAO.class);
    private final AppointmentDAO appointmentDAO = mock(AppointmentDAO.class);
    private final PrescriptionDAO prescriptionDAO = mock(PrescriptionDAO.class);
    private final AccessPolicy policy = new AccessPolicy(patientDAO, appointmentDAO, prescriptionDAO);

    private final HealthCenter center = new HealthCenter("Center", new Address("1 St", "Vigo", "36201", "Pontevedra"), "986000001", "c@x.es");
    private final Doctor doctorA = doctor(DOCTOR_A);
    private final Doctor doctorB = doctor(DOCTOR_B);
    private final Patient patientA = patient(PATIENT_A, doctorA);
    private final Patient patientB = patient(PATIENT_B, doctorB);

    @BeforeEach
    void records() {
        when(patientDAO.findById(PATIENT_A)).thenReturn(Optional.of(patientA));
        when(patientDAO.findById(PATIENT_B)).thenReturn(Optional.of(patientB));
        Appointment appointment = new Appointment(patientA, doctorA, LocalDateTime.now().plusDays(1), AppointmentStatus.SCHEDULED);
        when(appointmentDAO.findById(100L)).thenReturn(Optional.of(appointment));
        Prescription prescription = new Prescription(null, patientA, doctorA, 1.0, "with food", LocalDate.now(), LocalDate.now().plusDays(10));
        when(prescriptionDAO.findById(200L)).thenReturn(Optional.of(prescription));
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void loggedInAs(UserType role, long id) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(new AuthenticatedUser(id, role), null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private Doctor doctor(long id) {
        Doctor d = new Doctor("Doc", "1", "LIC-" + id, "986100001", center, "doc" + id, "x", "Doc", "d@x.es");
        d.setId(id);
        return d;
    }

    private Patient patient(long id, Doctor doctor) {
        Patient p = new Patient("Pat", "2", "HC-" + id, "SSN", new Address("1 St", "Vigo", "36201", "Pontevedra"), "986200001",
                LocalDate.of(1990, 1, 1), center, doctor, "pat" + id, "x", "Pat", "p@x.es");
        p.setId(id);
        return p;
    }

    private void assertAllowed(Runnable action) {
        assertThatCode(action::run).doesNotThrowAnyException();
    }

    private void assertDenied(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void withoutAuthenticationEverythingIsDenied() {
        assertDenied(() -> policy.requireSelf(UserType.PATIENT, PATIENT_A));
        assertDenied(() -> policy.requirePatientRecordAccess(PATIENT_A));
    }

    @Test
    void selfRequiresTheSameRoleAndId() {
        loggedInAs(UserType.PATIENT, PATIENT_A);
        assertAllowed(() -> policy.requireSelf(UserType.PATIENT, PATIENT_A));
        assertDenied(() -> policy.requireSelf(UserType.PATIENT, PATIENT_B));
        assertDenied(() -> policy.requireSelf(UserType.DOCTOR, PATIENT_A));
    }

    @Test
    void adminOrSelf() {
        loggedInAs(UserType.ADMINISTRATOR, ADMIN);
        assertAllowed(() -> policy.requireAdminOrSelf(UserType.DOCTOR, DOCTOR_A));
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertAllowed(() -> policy.requireAdminOrSelf(UserType.DOCTOR, DOCTOR_A));
        assertDenied(() -> policy.requireAdminOrSelf(UserType.DOCTOR, DOCTOR_B));
    }

    @Test
    void aPatientRecordIsVisibleToAdminsTheOwnerAndTheAssignedDoctorOnly() {
        loggedInAs(UserType.ADMINISTRATOR, ADMIN);
        assertAllowed(() -> policy.requirePatientRecordAccess(PATIENT_A));
        loggedInAs(UserType.PATIENT, PATIENT_A);
        assertAllowed(() -> policy.requirePatientRecordAccess(PATIENT_A));
        assertDenied(() -> policy.requirePatientRecordAccess(PATIENT_B));
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertAllowed(() -> policy.requirePatientRecordAccess(PATIENT_A));
        assertDenied(() -> policy.requirePatientRecordAccess(PATIENT_B));
        assertDenied(() -> policy.requirePatientRecordAccess(999L));
        loggedInAs(UserType.PHARMACY, PHARMACY);
        assertDenied(() -> policy.requirePatientRecordAccess(PATIENT_A));
    }

    @Test
    void anAppointmentCanBeCancelledByAdminsItsPatientAndItsDoctor() {
        loggedInAs(UserType.ADMINISTRATOR, ADMIN);
        assertAllowed(() -> policy.requireAppointmentParticipant(100L));
        loggedInAs(UserType.PATIENT, PATIENT_A);
        assertAllowed(() -> policy.requireAppointmentParticipant(100L));
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertAllowed(() -> policy.requireAppointmentParticipant(100L));
        loggedInAs(UserType.PATIENT, PATIENT_B);
        assertDenied(() -> policy.requireAppointmentParticipant(100L));
        loggedInAs(UserType.DOCTOR, DOCTOR_B);
        assertDenied(() -> policy.requireAppointmentParticipant(100L));
    }

    @Test
    void onlyTheDoctorOfAnAppointmentCanCompleteIt() {
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertAllowed(() -> policy.requireAppointmentDoctor(100L));
        loggedInAs(UserType.DOCTOR, DOCTOR_B);
        assertDenied(() -> policy.requireAppointmentDoctor(100L));
        loggedInAs(UserType.ADMINISTRATOR, ADMIN);
        assertDenied(() -> policy.requireAppointmentDoctor(100L));
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertThatThrownBy(() -> policy.requireAppointmentDoctor(999L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void onlyTheIssuingDoctorCanCancelAPrescription() {
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertAllowed(() -> policy.requirePrescriptionDoctor(200L));
        loggedInAs(UserType.DOCTOR, DOCTOR_B);
        assertDenied(() -> policy.requirePrescriptionDoctor(200L));
        loggedInAs(UserType.PATIENT, PATIENT_A);
        assertDenied(() -> policy.requirePrescriptionDoctor(200L));
        loggedInAs(UserType.DOCTOR, DOCTOR_A);
        assertThatThrownBy(() -> policy.requirePrescriptionDoctor(999L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
