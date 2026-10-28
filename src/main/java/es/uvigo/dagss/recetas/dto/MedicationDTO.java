package es.uvigo.dagss.recetas.dto;

import es.uvigo.dagss.recetas.entities.Medication;
import es.uvigo.dagss.recetas.validation.InputNormalizer;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record MedicationDTO(
        Long id,
        @NotBlank(message = "El nombre comercial es obligatorio")
        @Size(max = 120, message = "El nombre comercial no puede superar los 120 caracteres") String brandName,
        @NotBlank(message = "El principio activo es obligatorio")
        @Size(max = 120, message = "El principio activo no puede superar los 120 caracteres") String activeIngredient,
        @NotBlank(message = "El fabricante es obligatorio")
        @Size(max = 120, message = "El fabricante no puede superar los 120 caracteres") String manufacturer,
        @NotBlank(message = "La familia/categoría es obligatoria")
        @Size(max = 80, message = "La familia/categoría no puede superar los 80 caracteres") String category,
        @Positive(message = "Las dosis por envase deben ser un número mayor que 0")
        @Max(value = 1000, message = "Las dosis por envase no pueden superar 1000") int unitsPerPackage,
        boolean active) {

    public MedicationDTO {
        brandName = InputNormalizer.trim(brandName);
        activeIngredient = InputNormalizer.trim(activeIngredient);
        manufacturer = InputNormalizer.trim(manufacturer);
        category = InputNormalizer.trim(category);
    }

    public static MedicationDTO from(Medication m) {
        return new MedicationDTO(m.getId(), m.getBrandName(), m.getActiveIngredient(),
                m.getManufacturer(), m.getCategory(), m.getUnitsPerPackage(), m.isActive());
    }
}
