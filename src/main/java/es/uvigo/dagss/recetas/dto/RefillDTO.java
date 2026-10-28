package es.uvigo.dagss.recetas.dto;

import java.time.LocalDate;

import es.uvigo.dagss.recetas.entities.Refill;
import es.uvigo.dagss.recetas.entities.RefillStatus;

public record RefillDTO(
        Long id,
        Long prescriptionId,
        String medicationBrandName,
        String medicationActiveIngredient,
        String medicationManufacturer,
        String doctorName,
        LocalDate validFrom,
        LocalDate validUntil,
        int boxes,
        RefillStatus status,
        Long pharmacyId) {

    public static RefillDTO from(Refill r) {
        var prescription = r.getPrescription();
        var medication = prescription != null ? prescription.getMedication() : null;
        var doctor = prescription != null ? prescription.getDoctor() : null;
        return new RefillDTO(
                r.getId(),
                prescription != null ? prescription.getId() : null,
                medication != null ? medication.getBrandName() : null,
                medication != null ? medication.getActiveIngredient() : null,
                medication != null ? medication.getManufacturer() : null,
                doctor != null ? (doctor.getName() + " " + doctor.getLastName()) : null,
                r.getValidFrom(), r.getValidUntil(), r.getBoxes(), r.getStatus(),
                r.getPharmacy() != null ? r.getPharmacy().getId() : null);
    }
}
