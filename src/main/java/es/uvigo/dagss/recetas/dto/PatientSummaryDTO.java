package es.uvigo.dagss.recetas.dto;

import es.uvigo.dagss.recetas.entities.Patient;

public record PatientSummaryDTO(Long id, String name, String lastName) {
    public static PatientSummaryDTO from(Patient p) {
        if (p == null) return null;
        return new PatientSummaryDTO(p.getId(), p.getName(), p.getLastName());
    }
}
