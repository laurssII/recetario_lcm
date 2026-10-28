package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue(value = "DOCTOR")
public class Doctor extends User {

    private String lastName;
    @Column(unique = true)
    private String nationalId;
    @Column(unique = true)
    private String licenseNumber;
    private String phone;

    @ManyToOne
    private HealthCenter healthCenter;

    public Doctor() {
        super(UserType.DOCTOR);
    }

    public Doctor(String lastName, String nationalId, String licenseNumber, String phone, HealthCenter healthCenter, String username, String password, String name, String email) {
        super(UserType.DOCTOR, username, password, name, email);
        this.lastName = lastName;
        this.nationalId = nationalId;
        this.licenseNumber = licenseNumber;
        this.phone = phone;
        this.healthCenter = healthCenter;
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

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public HealthCenter getHealthCenter() {
        return healthCenter;
    }

    public void setHealthCenter(HealthCenter healthCenter) {
        this.healthCenter = healthCenter;
    }
}
