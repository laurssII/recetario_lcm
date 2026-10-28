package es.uvigo.dagss.recetas.security;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.AppointmentDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.daos.PrescriptionDAO;
import es.uvigo.dagss.recetas.entities.Appointment;
import es.uvigo.dagss.recetas.entities.Prescription;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.exceptions.ForbiddenException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

/**
 * Second line of defence of the API. The URL rules in {@code SecurityConfig}
 * decide which <i>roles</i> may call an endpoint; this class decides whether
 * <i>this particular user</i> may touch <i>this particular record</i> (a
 * patient can read their own data but not another patient's; a doctor can
 * close their own appointments but not a colleague's; and so on).
 *
 * Every method returns normally if the access is allowed and throws
 * {@link ForbiddenException} (HTTP 403) otherwise. The controllers call them
 * before doing any work. The full matrix is in {@code doc/seguridad.md}.
 */
@Component
@Transactional(readOnly = true)
public class AccessPolicy {

    static final String DENIED = "No tiene permiso para acceder a este recurso";

    private final PatientDAO patientDAO;
    private final AppointmentDAO appointmentDAO;
    private final PrescriptionDAO prescriptionDAO;

    public AccessPolicy(PatientDAO patientDAO, AppointmentDAO appointmentDAO, PrescriptionDAO prescriptionDAO) {
        this.patientDAO = patientDAO;
        this.appointmentDAO = appointmentDAO;
        this.prescriptionDAO = prescriptionDAO;
    }

    /** Only the user with that role and id (e.g. a patient editing their own profile). */
    public void requireSelf(UserType role, Long id) {
        if (!current().isSelf(role, id)) {
            throw denied();
        }
    }

    /** An administrator, or the user with that role and id. */
    public void requireAdminOrSelf(UserType role, Long id) {
        AuthenticatedUser user = current();
        if (!user.is(UserType.ADMINISTRATOR) && !user.isSelf(role, id)) {
            throw denied();
        }
    }

    /**
     * A patient's record: an administrator, the patient themselves, or the
     * doctor the patient is assigned to.
     */
    public void requirePatientRecordAccess(Long patientId) {
        AuthenticatedUser user = current();
        if (user.is(UserType.ADMINISTRATOR) || user.isSelf(UserType.PATIENT, patientId)) {
            return;
        }
        if (user.is(UserType.DOCTOR) && patientId != null && patientDAO.findById(patientId)
                .map(patient -> patient.getDoctor() != null && user.id().equals(patient.getDoctor().getId()))
                .orElse(false)) {
            return;
        }
        throw denied();
    }

    /** Cancelling an appointment: an administrator, or the patient or the doctor of that appointment. */
    public void requireAppointmentParticipant(Long appointmentId) {
        AuthenticatedUser user = current();
        Appointment appointment = findAppointment(appointmentId);
        boolean allowed = user.is(UserType.ADMINISTRATOR)
                || (user.is(UserType.PATIENT) && appointment.getPatient() != null
                        && user.id().equals(appointment.getPatient().getId()))
                || isDoctorOf(user, appointment);
        if (!allowed) {
            throw denied();
        }
    }

    /** Marking an appointment as completed / no-show: only the doctor of that appointment. */
    public void requireAppointmentDoctor(Long appointmentId) {
        AuthenticatedUser user = current();
        if (!isDoctorOf(user, findAppointment(appointmentId))) {
            throw denied();
        }
    }

    /** Cancelling a prescription: only the doctor who issued it. */
    public void requirePrescriptionDoctor(Long prescriptionId) {
        AuthenticatedUser user = current();
        Prescription prescription = prescriptionDAO.findById(prescriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Prescription", prescriptionId));
        if (!user.is(UserType.DOCTOR) || prescription.getDoctor() == null
                || !user.id().equals(prescription.getDoctor().getId())) {
            throw denied();
        }
    }

    private boolean isDoctorOf(AuthenticatedUser user, Appointment appointment) {
        return user.is(UserType.DOCTOR) && appointment.getDoctor() != null
                && user.id().equals(appointment.getDoctor().getId());
    }

    private Appointment findAppointment(Long id) {
        return appointmentDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Appointment", id));
    }

    private AuthenticatedUser current() {
        return CurrentUser.get().orElseThrow(this::denied);
    }

    private ForbiddenException denied() {
        return new ForbiddenException(DENIED);
    }
}
