package es.uvigo.dagss.recetas.web;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.security.AccessPolicy;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.dto.AppointmentDTO;
import es.uvigo.dagss.recetas.dto.NewAppointmentRequest;
import es.uvigo.dagss.recetas.services.AppointmentService;
import jakarta.validation.Valid;

@Tag(name = "Citas")
@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AccessPolicy access;

    public AppointmentController(AppointmentService appointmentService, AccessPolicy access) {
        this.appointmentService = appointmentService;
        this.access = access;
    }

    @Operation(summary = "Listar las citas de un día",
            description = "Permite filtrar por médico y paciente. Solo administradores.")
    @GetMapping
    public List<AppointmentDTO> browse(@Parameter(description = "Día en formato AAAA-MM-DD") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day,
                                        @Parameter(description = "Filtro por médico") @RequestParam(required = false) Long doctorId,
                                        @Parameter(description = "Filtro por paciente") @RequestParam(required = false) Long patientId) {
        return appointmentService.browseForAdmin(day, doctorId, patientId);
    }

    @Operation(summary = "Reservar una cita",
            description = "Reserva con el médico asignado al paciente, en un hueco libre de 15 minutos entre las 8:30 y las 15:30. Un paciente solo puede reservar para sí mismo.")
    @PostMapping
    public ResponseEntity<AppointmentDTO> create(@Valid @RequestBody NewAppointmentRequest request) {
        access.requireSelf(UserType.PATIENT, request.patientId());
        AppointmentDTO created = appointmentService.create(request);
        return ResponseEntity.created(URI.create("/api/appointments/" + created.id())).body(created);
    }

    @Operation(summary = "Cancelar una cita",
            description = "El administrador, el paciente de la cita o su médico.")
    @PostMapping("/{id}/cancel")
    public AppointmentDTO cancel(@PathVariable Long id) {
        access.requireAppointmentParticipant(id);
        return appointmentService.cancel(id);
    }

    @Operation(summary = "Marcar una cita como ausente",
            description = "El paciente no se presentó. Solo el médico de la cita.")
    @PostMapping("/{id}/no-show")
    public AppointmentDTO markNoShow(@PathVariable Long id) {
        access.requireAppointmentDoctor(id);
        return appointmentService.markNoShow(id);
    }

    @Operation(summary = "Marcar una cita como completada",
            description = "Solo el médico de la cita.")
    @PostMapping("/{id}/complete")
    public AppointmentDTO markCompleted(@PathVariable Long id) {
        access.requireAppointmentDoctor(id);
        return appointmentService.markCompleted(id);
    }
}
