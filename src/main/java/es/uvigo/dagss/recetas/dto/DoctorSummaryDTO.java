package es.uvigo.dagss.recetas.dto;

import es.uvigo.dagss.recetas.entities.Doctor;

public record DoctorSummaryDTO(Long id, String name, String lastName) {
    public static DoctorSummaryDTO from(Doctor d) {
        if (d == null) return null;
        return new DoctorSummaryDTO(d.getId(), d.getName(), d.getLastName());
    }
}
