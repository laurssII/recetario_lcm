package es.uvigo.dagss.recetas.dto;

import es.uvigo.dagss.recetas.entities.HealthCenter;

/** Reduced version of HealthCenter, nested inside other DTOs (doctor, patient...). */
public record HealthCenterSummaryDTO(Long id, String name) {
    public static HealthCenterSummaryDTO from(HealthCenter c) {
        if (c == null) return null;
        return new HealthCenterSummaryDTO(c.getId(), c.getName());
    }
}
