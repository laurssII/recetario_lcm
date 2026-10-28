package es.uvigo.dagss.recetas.web;

import java.util.List;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.security.AccessPolicy;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.dto.RefillDTO;
import es.uvigo.dagss.recetas.services.RefillService;

@Tag(name = "Recetas")
@RestController
@RequestMapping("/api/refills")
public class RefillController {

    private final RefillService refillService;
    private final AccessPolicy access;

    public RefillController(RefillService refillService, AccessPolicy access) {
        this.refillService = refillService;
        this.access = access;
    }

    @Operation(summary = "Buscar recetas pendientes por tarjeta sanitaria",
            description = "Para farmacias. El número no distingue mayúsculas ni espacios al principio o al final.")
    @GetMapping
    public List<RefillDTO> findByHealthCardNumber(@Parameter(description = "Número de tarjeta sanitaria (4 letras y 10 dígitos)") @RequestParam String healthCardNumber) {
        return refillService.pendingByHealthCardNumber(healthCardNumber);
    }

    @Operation(summary = "Dispensar una receta",
            description = "Registra la entrega. Solo es posible si la receta está pendiente y hoy está dentro de su ventana de validez. Solo la propia farmacia (`pharmacyId`).")
    @PostMapping("/{id}/fill")
    public RefillDTO fill(@PathVariable Long id, @Parameter(description = "Id de la farmacia que dispensa (la propia)") @RequestParam Long pharmacyId) {
        access.requireSelf(UserType.PHARMACY, pharmacyId);
        return refillService.fill(id, pharmacyId);
    }
}
