package es.uvigo.dagss.recetas.dto;

import java.time.LocalDateTime;

import es.uvigo.dagss.recetas.entities.Appointment;
import es.uvigo.dagss.recetas.entities.AppointmentStatus;

public record AppointmentDTO(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        LocalDateTime dateTime,
        int duration,
        AppointmentStatus status) {

    public static AppointmentDTO from(Appointment a) {
        String patientName = a.getPatient() != null
                ? a.getPatient().getName() + " " + a.getPatient().getLastName()
                : null;
        String doctorName = a.getDoctor() != null
                ? a.getDoctor().getName() + " " + a.getDoctor().getLastName()
                : null;
        return new AppointmentDTO(a.getId(),
                a.getPatient() != null ? a.getPatient().getId() : null, patientName,
                a.getDoctor() != null ? a.getDoctor().getId() : null, doctorName,
                a.getDateTime(), a.getDuration(), a.getStatus());
    }
}
