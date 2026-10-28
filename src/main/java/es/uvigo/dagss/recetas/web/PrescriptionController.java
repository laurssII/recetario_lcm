package es.uvigo.dagss.recetas.web;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.security.AccessPolicy;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.dto.NewPrescriptionRequest;
import es.uvigo.dagss.recetas.dto.PrescriptionDTO;
import es.uvigo.dagss.recetas.services.PrescriptionService;
import jakarta.validation.Valid;

@Tag(name = "Prescripciones")
@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final AccessPolicy access;

    public PrescriptionController(PrescriptionService prescriptionService, AccessPolicy access) {
        this.prescriptionService = prescriptionService;
        this.access = access;
    }

    @Operation(summary = "Crear una prescripción y su plan de recetas",
            description = "La prescripción empieza hoy y su plan se genera automáticamente: cada receta cubre una caja y es válida desde 7 días antes hasta 7 días después de su fecha exacta (la primera, sin margen anterior), sin pasar de la fecha de fin. Solo el propio médico (`doctorId`).")
    @PostMapping
    public ResponseEntity<PrescriptionDTO> create(@Valid @RequestBody NewPrescriptionRequest request) {
        access.requireSelf(UserType.DOCTOR, request.doctorId());
        PrescriptionDTO created = prescriptionService.create(request);
        return ResponseEntity.created(URI.create("/api/prescriptions/" + created.id())).body(created);
    }

    @Operation(summary = "Anular una prescripción",
            description = "Desactiva la prescripción y cancela sus recetas pendientes; las ya dispensadas no cambian. Solo el médico que la emitió.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        access.requirePrescriptionDoctor(id);
        prescriptionService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
