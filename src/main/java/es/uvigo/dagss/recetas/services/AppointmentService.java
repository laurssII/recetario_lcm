package es.uvigo.dagss.recetas.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.AppointmentDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.dto.AppointmentDTO;
import es.uvigo.dagss.recetas.dto.AvailableSlotDTO;
import es.uvigo.dagss.recetas.dto.NewAppointmentRequest;
import es.uvigo.dagss.recetas.entities.Appointment;
import es.uvigo.dagss.recetas.entities.AppointmentStatus;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.Patient;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class AppointmentService {

    /** Office hours: 8:30 to 15:30, in fixed 15-minute slots. */
    private static final LocalTime OFFICE_START = LocalTime.of(8, 30);
    private static final LocalTime OFFICE_END = LocalTime.of(15, 30);
    private static final int SLOT_MINUTES = 15;

    private final AppointmentDAO appointmentDAO;
    private final PatientDAO patientDAO;

    public AppointmentService(AppointmentDAO appointmentDAO, PatientDAO patientDAO) {
        this.appointmentDAO = appointmentDAO;
        this.patientDAO = patientDAO;
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> browseForAdmin(LocalDate day, Long doctorId, Long patientId) {
        LocalDateTime from = day.atStartOfDay();
        LocalDateTime to = day.plusDays(1).atStartOfDay();
        List<Appointment> result;
        if (doctorId != null && patientId != null) {
            result = appointmentDAO.findByDateTimeBetweenAndDoctorIdAndPatientIdOrderByDateTimeAsc(from, to, doctorId, patientId);
        } else if (doctorId != null) {
            result = appointmentDAO.findByDateTimeBetweenAndDoctorIdOrderByDateTimeAsc(from, to, doctorId);
        } else if (patientId != null) {
            result = appointmentDAO.findByDateTimeBetweenAndPatientIdOrderByDateTimeAsc(from, to, patientId);
        } else {
            result = appointmentDAO.findByDateTimeBetweenOrderByDateTimeAsc(from, to);
        }
        return result.stream().map(AppointmentDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> scheduleForDay(Long doctorId, LocalDate day) {
        LocalDate date = day != null ? day : LocalDate.now();
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        return appointmentDAO.findByDoctorIdAndDateTimeBetweenOrderByDateTimeAsc(doctorId, from, to)
                .stream().map(AppointmentDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDTO> scheduledForPatient(Long patientId) {
        return appointmentDAO.findByPatientIdAndStatusOrderByDateTimeAsc(patientId, AppointmentStatus.SCHEDULED)
                .stream().map(AppointmentDTO::from).toList();
    }

    /** A slot is free when the doctor has no SCHEDULED appointment in it. */
    @Transactional(readOnly = true)
    public List<AvailableSlotDTO> availableSlots(Long patientId, LocalDate day) {
        Patient patient = patientDAO.findById(patientId)
                .orElseThrow(() -> ResourceNotFoundException.of("Patient", patientId));
        Doctor doctor = patient.getDoctor();
        if (doctor == null) {
            throw new InvalidOperationException("The patient has no assigned doctor");
        }

        LocalDateTime from = day.atTime(OFFICE_START);
        LocalDateTime to = day.atTime(OFFICE_END);

        Set<LocalDateTime> taken = appointmentDAO
                .findByDoctorIdAndStatusAndDateTimeBetween(doctor.getId(), AppointmentStatus.SCHEDULED, from, to)
                .stream().map(Appointment::getDateTime).collect(Collectors.toSet());

        List<AvailableSlotDTO> slots = new ArrayList<>();
        for (LocalDateTime slot = from; slot.isBefore(to); slot = slot.plusMinutes(SLOT_MINUTES)) {
            if (!taken.contains(slot)) {
                slots.add(new AvailableSlotDTO(slot));
            }
        }
        return slots;
    }

    public AppointmentDTO create(NewAppointmentRequest request) {
        Patient patient = patientDAO.findById(request.patientId())
                .orElseThrow(() -> ResourceNotFoundException.of("Patient", request.patientId()));
        Doctor doctor = patient.getDoctor();
        if (doctor == null) {
            throw new InvalidOperationException("The patient has no assigned doctor");
        }
        validateSlotWithinOfficeHours(request.dateTime());

        boolean taken = !appointmentDAO.findByDoctorIdAndStatusAndDateTimeBetween(
                doctor.getId(), AppointmentStatus.SCHEDULED,
                request.dateTime(), request.dateTime().plusMinutes(1)).isEmpty();
        if (taken) {
            throw new InvalidOperationException("The selected slot is no longer available");
        }

        Appointment appointment = new Appointment(patient, doctor, request.dateTime(), AppointmentStatus.SCHEDULED);
        return AppointmentDTO.from(appointmentDAO.save(appointment));
    }

    public AppointmentDTO cancel(Long appointmentId) {
        Appointment appointment = getOrFail(appointmentId);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return AppointmentDTO.from(appointmentDAO.save(appointment));
    }

    public AppointmentDTO markNoShow(Long appointmentId) {
        Appointment appointment = getOrFail(appointmentId);
        appointment.setStatus(AppointmentStatus.NO_SHOW);
        return AppointmentDTO.from(appointmentDAO.save(appointment));
    }

    public AppointmentDTO markCompleted(Long appointmentId) {
        Appointment appointment = getOrFail(appointmentId);
        appointment.setStatus(AppointmentStatus.COMPLETED);
        return AppointmentDTO.from(appointmentDAO.save(appointment));
    }

    private void validateSlotWithinOfficeHours(LocalDateTime dateTime) {
        LocalTime time = dateTime.toLocalTime();
        boolean fits = !time.isBefore(OFFICE_START) && time.isBefore(OFFICE_END)
                && time.getMinute() % SLOT_MINUTES == 0;
        if (!fits) {
            throw new InvalidOperationException(
                    "The appointment must fall on a " + SLOT_MINUTES
                            + "-minute slot between " + OFFICE_START + " and " + OFFICE_END);
        }
    }

    private Appointment getOrFail(Long id) {
        return appointmentDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Appointment", id));
    }
}
