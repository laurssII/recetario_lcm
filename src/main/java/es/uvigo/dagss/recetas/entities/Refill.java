package es.uvigo.dagss.recetas.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

/** One refill slip of a prescription's plan: lets the patient pick up one box at a pharmacy within its validity window. */
@Entity
public class Refill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Prescription prescription;

    private LocalDate validFrom;

    private LocalDate validUntil;

    private int boxes;

    @Enumerated(EnumType.STRING)
    @Column(name = "REFILL_STATUS", length = 20)
    private RefillStatus status;

    @ManyToOne
    private Pharmacy pharmacy;

    public Refill() {
    }

    public Refill(Prescription prescription, LocalDate validFrom, LocalDate validUntil, int boxes, RefillStatus status, Pharmacy pharmacy) {
        this.prescription = prescription;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.boxes = boxes;
        this.status = status;
        this.pharmacy = pharmacy;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Prescription getPrescription() {
        return prescription;
    }

    public void setPrescription(Prescription prescription) {
        this.prescription = prescription;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public int getBoxes() {
        return boxes;
    }

    public void setBoxes(int boxes) {
        this.boxes = boxes;
    }

    public RefillStatus getStatus() {
        return status;
    }

    public void setStatus(RefillStatus status) {
        this.status = status;
    }

    public Pharmacy getPharmacy() {
        return pharmacy;
    }

    public void setPharmacy(Pharmacy pharmacy) {
        this.pharmacy = pharmacy;
    }
}
