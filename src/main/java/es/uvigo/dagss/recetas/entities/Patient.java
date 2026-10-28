package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@DiscriminatorValue(value = "PATIENT")
public class Patient extends User {

    private String lastName;
    @Column(unique = true)
    private String nationalId;
    @Column(unique = true)
    private String healthCardNumber;
    private String socialSecurityNumber;
    @Embedded
    private Address address;
    private String phone;

    private LocalDate birthDate;

    @ManyToOne
    private HealthCenter healthCenter;

    @ManyToOne
    private Doctor doctor;

    public Patient() {
        super(UserType.PATIENT);
    }

    public Patient(String lastName, String nationalId, String healthCardNumber, String socialSecurityNumber, Address address, String phone, LocalDate birthDate, HealthCenter healthCenter, Doctor doctor, String username, String password, String name, String email) {
        super(UserType.PATIENT, username, password, name, email);
        this.lastName = lastName;
        this.nationalId = nationalId;
        this.healthCardNumber = healthCardNumber;
        this.socialSecurityNumber = socialSecurityNumber;
        this.address = address;
        this.phone = phone;
        this.birthDate = birthDate;
        this.healthCenter = healthCenter;
        this.doctor = doctor;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public String getHealthCardNumber() {
        return healthCardNumber;
    }

    public void setHealthCardNumber(String healthCardNumber) {
        this.healthCardNumber = healthCardNumber;
    }

    public String getSocialSecurityNumber() {
        return socialSecurityNumber;
    }

    public void setSocialSecurityNumber(String socialSecurityNumber) {
        this.socialSecurityNumber = socialSecurityNumber;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public HealthCenter getHealthCenter() {
        return healthCenter;
    }

    public void setHealthCenter(HealthCenter healthCenter) {
        this.healthCenter = healthCenter;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public void setDoctor(Doctor doctor) {
        this.doctor = doctor;
    }

}
