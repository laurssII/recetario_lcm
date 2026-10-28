package es.uvigo.dagss.recetas.dto;

import java.time.LocalDate;
import java.util.List;

import es.uvigo.dagss.recetas.entities.Prescription;

public record PrescriptionDTO(
        Long id,
        Long patientId,
        Long doctorId,
        String doctorName,
        Long medicationId,
        String medicationBrandName,
        double dailyDose,
        String instructions,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        List<RefillDTO> refills) {

    public static PrescriptionDTO from(Prescription p, List<RefillDTO> refills) {
        return new PrescriptionDTO(p.getId(),
                p.getPatient() != null ? p.getPatient().getId() : null,
                p.getDoctor() != null ? p.getDoctor().getId() : null,
                p.getDoctor() != null ? (p.getDoctor().getName() + " " + p.getDoctor().getLastName()) : null,
                p.getMedication() != null ? p.getMedication().getId() : null,
                p.getMedication() != null ? p.getMedication().getBrandName() : null,
                p.getDailyDose(), p.getInstructions(), p.getStartDate(), p.getEndDate(), p.isActive(), refills);
    }
}
