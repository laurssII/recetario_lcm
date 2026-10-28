package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue(value = "PHARMACY")
public class Pharmacy extends User {

    private String pharmacistFirstName;
    private String pharmacistLastName;
    @Column(unique = true)
    private String taxId;
    @Column(unique = true)
    private String pharmacistLicenseNumber;
    @Embedded
    private Address address;
    private String phone;

    public Pharmacy() {
        super(UserType.PHARMACY);
    }

    public Pharmacy(String pharmacistFirstName, String pharmacistLastName, String taxId, String pharmacistLicenseNumber, Address address, String phone, String username, String password, String name, String email) {
        super(UserType.PHARMACY, username, password, name, email);
        this.pharmacistFirstName = pharmacistFirstName;
        this.pharmacistLastName = pharmacistLastName;
        this.taxId = taxId;
        this.pharmacistLicenseNumber = pharmacistLicenseNumber;
        this.address = address;
        this.phone = phone;
    }

    public String getPharmacistFirstName() {
        return pharmacistFirstName;
    }

    public void setPharmacistFirstName(String pharmacistFirstName) {
        this.pharmacistFirstName = pharmacistFirstName;
    }

    public String getPharmacistLastName() {
        return pharmacistLastName;
    }

    public void setPharmacistLastName(String pharmacistLastName) {
        this.pharmacistLastName = pharmacistLastName;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getPharmacistLicenseNumber() {
        return pharmacistLicenseNumber;
    }

    public void setPharmacistLicenseNumber(String pharmacistLicenseNumber) {
        this.pharmacistLicenseNumber = pharmacistLicenseNumber;
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

}
