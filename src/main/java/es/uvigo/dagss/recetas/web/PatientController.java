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
import es.uvigo.dagss.recetas.dto.*;
import es.uvigo.dagss.recetas.services.AppointmentService;
import es.uvigo.dagss.recetas.services.PatientService;
import es.uvigo.dagss.recetas.services.PrescriptionService;
import es.uvigo.dagss.recetas.services.RefillService;
import jakarta.validation.Valid;

@Tag(name = "Pacientes")
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final PrescriptionService prescriptionService;
    private final RefillService refillService;
    private final AccessPolicy access;

    public PatientController(PatientService patientService, AppointmentService appointmentService,
                              PrescriptionService prescriptionService, RefillService refillService,
                              AccessPolicy access) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.prescriptionService = prescriptionService;
        this.refillService = refillService;
        this.access = access;
    }

    @Operation(summary = "Listar pacientes",
            description = "Permite filtrar por nombre, centro de salud y médico. Solo administradores.")
    @GetMapping
    public List<PatientDTO> list(@Parameter(description = "Filtro por nombre") @RequestParam(required = false) String name,
                                  @Parameter(description = "Filtro por centro de salud") @RequestParam(required = false) Long healthCenterId,
                                  @Parameter(description = "Filtro por médico") @RequestParam(required = false) Long doctorId) {
        return patientService.list(name, healthCenterId, doctorId);
    }

    @Operation(summary = "Consultar un paciente",
            description = "Un administrador, el propio paciente o su médico asignado.")
    @GetMapping("/{id}")
    public PatientDTO findById(@PathVariable Long id) {
        access.requirePatientRecordAccess(id);
        return patientService.findById(id);
    }

    @Operation(summary = "Crear un paciente",
            description = "Su contraseña inicial es su DNI. El médico asignado debe trabajar en el centro de salud del paciente. Solo administradores.")
    @PostMapping
    public ResponseEntity<PatientDTO> create(@Valid @RequestBody PatientDTO dto) {
        PatientDTO created = patientService.create(dto);
        return ResponseEntity.created(URI.create("/api/patients/" + created.id())).body(created);
    }

    @Operation(summary = "Modificar un paciente",
            description = "Solo administradores.")
    @PutMapping("/{id}")
    public PatientDTO update(@PathVariable Long id, @Valid @RequestBody PatientDTO dto) {
        return patientService.update(id, dto);
    }

    @Operation(summary = "Dar de baja un paciente",
            description = "Baja lógica. Solo administradores.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        patientService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Citas planificadas de un paciente",
            description = "Solo las citas en estado SCHEDULED. Un administrador o el propio paciente.")
    @GetMapping("/{id}/appointments")
    public List<AppointmentDTO> appointments(@PathVariable Long id) {
        access.requireAdminOrSelf(UserType.PATIENT, id);
        return appointmentService.scheduledForPatient(id);
    }

    @Operation(summary = "Huecos libres para reservar cita",
            description = "Huecos de 15 minutos entre las 8:30 y las 15:30 en los que el médico asignado al paciente no tiene otra cita. Solo el propio paciente.")
    @GetMapping("/{id}/available-slots")
    public List<AvailableSlotDTO> availableSlots(@PathVariable Long id,
            @Parameter(description = "Día en formato AAAA-MM-DD") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        access.requireSelf(UserType.PATIENT, id);
        return appointmentService.availableSlots(id, day);
    }

    @Operation(summary = "Prescripciones en vigor de un paciente",
            description = "Incluye el plan de recetas de cada una. Un administrador, el propio paciente o su médico.")
    @GetMapping("/{id}/prescriptions")
    public List<PrescriptionDTO> prescriptions(@PathVariable Long id) {
        access.requirePatientRecordAccess(id);
        return prescriptionService.activePrescriptionsForPatient(id);
    }

    @Operation(summary = "Recetas pendientes de recoger",
            description = "Recetas en estado SCHEDULED, de la más próxima a la más lejana. Solo el propio paciente.")
    @GetMapping("/{id}/refills")
    public List<RefillDTO> refills(@PathVariable Long id) {
        access.requireSelf(UserType.PATIENT, id);
        return refillService.pendingForPatient(id);
    }

    @Operation(summary = "Modificar los datos propios",
            description = "El paciente no puede cambiar su centro de salud ni su médico.")
    @PatchMapping("/{id}/profile")
    public PatientDTO updateOwnProfile(@PathVariable Long id, @Valid @RequestBody PatientProfileUpdateRequest request) {
        access.requireSelf(UserType.PATIENT, id);
        return patientService.updateOwnProfile(id, request);
    }

    @Operation(summary = "Cambiar la contraseña propia",
            description = "Exige la contraseña actual.")
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changeOwnPassword(@PathVariable Long id, @Valid @RequestBody PasswordChangeRequest request) {
        access.requireSelf(UserType.PATIENT, id);
        patientService.changeOwnPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
