package es.uvigo.dagss.recetas.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.dto.AdministratorDTO;
import es.uvigo.dagss.recetas.dto.NewAdministratorRequest;
import es.uvigo.dagss.recetas.services.AdministratorService;
import jakarta.validation.Valid;

@Tag(name = "Administradores")
@RestController
@RequestMapping("/api/administrators")
public class AdministratorController {

    private final AdministratorService administratorService;

    public AdministratorController(AdministratorService administratorService) {
        this.administratorService = administratorService;
    }

    @Operation(summary = "Listar administradores",
            description = "Solo administradores.")
    @GetMapping
    public List<AdministratorDTO> list() {
        return administratorService.list();
    }

    @Operation(summary = "Consultar un administrador",
            description = "Solo administradores.")
    @GetMapping("/{id}")
    public AdministratorDTO findById(@PathVariable Long id) {
        return administratorService.findById(id);
    }

    @Operation(summary = "Crear un administrador",
            description = "La contraseña inicial va en el cuerpo de la petición y es obligatoria. Solo administradores.")
    @PostMapping
    public ResponseEntity<AdministratorDTO> create(@Valid @RequestBody NewAdministratorRequest request) {
        AdministratorDTO created = administratorService.create(request.toDto(), request.password());
        return ResponseEntity.created(URI.create("/api/administrators/" + created.id())).body(created);
    }

    @Operation(summary = "Modificar un administrador",
            description = "Solo administradores.")
    @PutMapping("/{id}")
    public AdministratorDTO update(@PathVariable Long id, @Valid @RequestBody AdministratorDTO dto) {
        return administratorService.update(id, dto);
    }

    @Operation(summary = "Dar de baja un administrador",
            description = "Baja lógica. Solo administradores.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        administratorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
