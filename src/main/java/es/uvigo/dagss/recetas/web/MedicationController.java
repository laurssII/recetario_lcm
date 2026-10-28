package es.uvigo.dagss.recetas.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.dto.MedicationDTO;
import es.uvigo.dagss.recetas.services.MedicationService;
import jakarta.validation.Valid;

@Tag(name = "Medicamentos")
@RestController
@RequestMapping("/api/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @Operation(summary = "Buscar medicamentos",
            description = "Búsqueda aproximada con un único criterio: el primero informado, en este orden: nombre comercial, principio activo, fabricante, categoría. Sin criterios devuelve todos. Cualquier usuario autenticado.")
    @GetMapping
    public List<MedicationDTO> search(@Parameter(description = "Nombre comercial") @RequestParam(required = false) String brandName,
                                       @Parameter(description = "Principio activo") @RequestParam(required = false) String activeIngredient,
                                       @Parameter(description = "Fabricante") @RequestParam(required = false) String manufacturer,
                                       @Parameter(description = "Categoría") @RequestParam(required = false) String category) {
        return medicationService.search(brandName, activeIngredient, manufacturer, category);
    }

    @Operation(summary = "Consultar un medicamento",
            description = "Cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public MedicationDTO findById(@PathVariable Long id) {
        return medicationService.findById(id);
    }

    @Operation(summary = "Crear un medicamento",
            description = "`unitsPerPackage` es el número de unidades de cada caja y determina el plan de recetas. Solo administradores.")
    @PostMapping
    public ResponseEntity<MedicationDTO> create(@Valid @RequestBody MedicationDTO dto) {
        MedicationDTO created = medicationService.create(dto);
        return ResponseEntity.created(URI.create("/api/medications/" + created.id())).body(created);
    }

    @Operation(summary = "Modificar un medicamento",
            description = "Solo administradores.")
    @PutMapping("/{id}")
    public MedicationDTO update(@PathVariable Long id, @Valid @RequestBody MedicationDTO dto) {
        return medicationService.update(id, dto);
    }

    @Operation(summary = "Dar de baja un medicamento",
            description = "Baja lógica. Solo administradores.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        medicationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
