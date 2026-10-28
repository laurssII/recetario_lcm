package es.uvigo.dagss.recetas.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.dto.HealthCenterDTO;
import es.uvigo.dagss.recetas.services.HealthCenterService;
import jakarta.validation.Valid;

@Tag(name = "Centros de salud")
@RestController
@RequestMapping("/api/health-centers")
public class HealthCenterController {

    private final HealthCenterService healthCenterService;

    public HealthCenterController(HealthCenterService healthCenterService) {
        this.healthCenterService = healthCenterService;
    }

    @Operation(summary = "Listar centros de salud",
            description = "Permite filtrar por nombre y localidad. Cualquier usuario autenticado.")
    @GetMapping
    public List<HealthCenterDTO> list(@Parameter(description = "Filtro por nombre") @RequestParam(required = false) String name,
                                       @Parameter(description = "Filtro por localidad") @RequestParam(required = false) String city) {
        return healthCenterService.list(name, city);
    }

    @Operation(summary = "Consultar un centro de salud",
            description = "Cualquier usuario autenticado.")
    @GetMapping("/{id}")
    public HealthCenterDTO findById(@PathVariable Long id) {
        return healthCenterService.findById(id);
    }

    @Operation(summary = "Crear un centro de salud",
            description = "Solo administradores.")
    @PostMapping
    public ResponseEntity<HealthCenterDTO> create(@Valid @RequestBody HealthCenterDTO dto) {
        HealthCenterDTO created = healthCenterService.create(dto);
        return ResponseEntity.created(URI.create("/api/health-centers/" + created.id())).body(created);
    }

    @Operation(summary = "Modificar un centro de salud",
            description = "Solo administradores.")
    @PutMapping("/{id}")
    public HealthCenterDTO update(@PathVariable Long id, @Valid @RequestBody HealthCenterDTO dto) {
        return healthCenterService.update(id, dto);
    }

    @Operation(summary = "Dar de baja un centro de salud",
            description = "Baja lógica: el centro queda inactivo, no se borra. Solo administradores.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        healthCenterService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
