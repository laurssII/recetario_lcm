package es.uvigo.dagss.recetas.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record AvailableSlotDTO(@Schema(description = "Inicio del hueco de 15 minutos", example = "2030-03-04T09:00:00") LocalDateTime start) {
}
