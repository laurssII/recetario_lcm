package es.uvigo.dagss.recetas.daos;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Appointment;
import es.uvigo.dagss.recetas.entities.AppointmentStatus;

public interface AppointmentDAO extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorId(Long doctorId);

    List<Appointment> findByPatientId(Long patientId);

    List<Appointment> findByPatientIdAndStatusOrderByDateTimeAsc(Long patientId, AppointmentStatus status);

    List<Appointment> findByDoctorIdAndDateTimeBetweenOrderByDateTimeAsc(Long doctorId, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByDoctorIdAndStatusAndDateTimeBetween(Long doctorId, AppointmentStatus status, LocalDateTime from, LocalDateTime to);

    List<Appointment> findByDateTimeBetweenOrderByDateTimeAsc(LocalDateTime from, LocalDateTime to);

    List<Appointment> findByDateTimeBetweenAndDoctorIdOrderByDateTimeAsc(LocalDateTime from, LocalDateTime to, Long doctorId);

    List<Appointment> findByDateTimeBetweenAndPatientIdOrderByDateTimeAsc(LocalDateTime from, LocalDateTime to, Long patientId);

    List<Appointment> findByDateTimeBetweenAndDoctorIdAndPatientIdOrderByDateTimeAsc(LocalDateTime from, LocalDateTime to, Long doctorId, Long patientId);
}
