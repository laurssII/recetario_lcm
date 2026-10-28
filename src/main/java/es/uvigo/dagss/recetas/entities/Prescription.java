package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

/** A medication order written by a doctor for a patient. Creating one generates its refill plan. */
@Entity
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Medication medication;

    @ManyToOne
    private Patient patient;

    @ManyToOne
    private Doctor doctor;

    /** Number of units of the medication (pills, ml, etc) to take per day. */
    private double dailyDose;

    private String instructions;

    private LocalDate startDate;

    private LocalDate endDate;

    /** true = ACTIVE, false = INACTIVE. */
    private boolean active = true;

    public Prescription() {
    }

    public Prescription(Medication medication, Patient patient, Doctor doctor, double dailyDose, String instructions, LocalDate startDate, LocalDate endDate) {
        this.medication = medication;
        this.patient = patient;
        this.doctor = doctor;
        this.dailyDose = dailyDose;
        this.instructions = instructions;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Prescription(Medication medication, Patient patient, Doctor doctor, double dailyDose, String instructions, LocalDate startDate, LocalDate endDate, boolean active) {
        this(medication, patient, doctor, dailyDose, instructions, startDate, endDate);
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medication getMedication() {
        return medication;
    }

    public void setMedication(Medication medication) {
        this.medication = medication;
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

    public double getDailyDose() {
        return dailyDose;
    }

    public void setDailyDose(double dailyDose) {
        this.dailyDose = dailyDose;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
