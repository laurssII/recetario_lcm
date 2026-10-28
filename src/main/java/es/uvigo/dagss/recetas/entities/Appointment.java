package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Appointment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Patient patient;

    @ManyToOne
    private Doctor doctor;

    private LocalDateTime dateTime;

    private int duration = 15;

    @Enumerated(EnumType.STRING)
    @Column(name = "APPOINTMENT_STATUS", length = 20)
    private AppointmentStatus status;

    public Appointment() {
    }

    public Appointment(Patient patient, Doctor doctor, LocalDateTime dateTime, AppointmentStatus status) {
        this.patient = patient;
        this.doctor = doctor;
        this.dateTime = dateTime;
        this.status = status;
    }

    public Appointment(Patient patient, Doctor doctor, LocalDateTime dateTime, int duration, AppointmentStatus status) {
        this.patient = patient;
        this.doctor = doctor;
        this.dateTime = dateTime;
        this.duration = duration;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public void setDateTime(LocalDateTime dateTime) {
        this.dateTime = dateTime;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
