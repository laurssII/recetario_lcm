package es.uvigo.dagss.recetas.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.security.AccessPolicy;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.dto.PharmacyDTO;
import es.uvigo.dagss.recetas.services.PharmacyService;
import jakarta.validation.Valid;

@Tag(name = "Farmacias")
@RestController
@RequestMapping("/api/pharmacies")
public class PharmacyController {

    private final PharmacyService pharmacyService;
    private final AccessPolicy access;

    public PharmacyController(PharmacyService pharmacyService, AccessPolicy access) {
        this.pharmacyService = pharmacyService;
        this.access = access;
    }

    @Operation(summary = "Listar farmacias",
            description = "Permite filtrar por nombre. Solo administradores.")
    @GetMapping
    public List<PharmacyDTO> list(@Parameter(description = "Filtro por nombre") @RequestParam(required = false) String name) {
        return pharmacyService.list(name);
    }

    @Operation(summary = "Consultar una farmacia",
            description = "Un administrador o la propia farmacia.")
    @GetMapping("/{id}")
    public PharmacyDTO findById(@PathVariable Long id) {
        access.requireAdminOrSelf(UserType.PHARMACY, id);
        return pharmacyService.findById(id);
    }

    @Operation(summary = "Crear una farmacia",
            description = "Su contraseña inicial es el número de colegiado del farmacéutico. Solo administradores.")
    @PostMapping
    public ResponseEntity<PharmacyDTO> create(@Valid @RequestBody PharmacyDTO dto) {
        PharmacyDTO created = pharmacyService.create(dto);
        return ResponseEntity.created(URI.create("/api/pharmacies/" + created.id())).body(created);
    }

    @Operation(summary = "Modificar una farmacia",
            description = "Solo administradores.")
    @PutMapping("/{id}")
    public PharmacyDTO update(@PathVariable Long id, @Valid @RequestBody PharmacyDTO dto) {
        return pharmacyService.update(id, dto);
    }

    @Operation(summary = "Dar de baja una farmacia",
            description = "Baja lógica. Solo administradores.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        pharmacyService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Modificar los datos propios")
    @PatchMapping("/{id}/profile")
    public PharmacyDTO updateOwnProfile(@PathVariable Long id,
            @Valid @RequestBody es.uvigo.dagss.recetas.dto.PharmacyProfileUpdateRequest request) {
        access.requireSelf(UserType.PHARMACY, id);
        return pharmacyService.updateOwnProfile(id, request);
    }

    @Operation(summary = "Cambiar la contraseña propia",
            description = "Exige la contraseña actual.")
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changeOwnPassword(@PathVariable Long id,
            @Valid @RequestBody es.uvigo.dagss.recetas.dto.PasswordChangeRequest request) {
        access.requireSelf(UserType.PHARMACY, id);
        pharmacyService.changeOwnPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
