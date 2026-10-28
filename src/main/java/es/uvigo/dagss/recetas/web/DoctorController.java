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
import es.uvigo.dagss.recetas.dto.DoctorDTO;
import es.uvigo.dagss.recetas.services.AppointmentService;
import es.uvigo.dagss.recetas.services.DoctorService;
import jakarta.validation.Valid;

@Tag(name = "Médicos")
@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final AppointmentService appointmentService;
    private final AccessPolicy access;

    public DoctorController(DoctorService doctorService, AppointmentService appointmentService, AccessPolicy access) {
        this.doctorService = doctorService;
        this.appointmentService = appointmentService;
        this.access = access;
    }

    @Operation(summary = "Listar médicos",
            description = "Permite filtrar por nombre y centro de salud. Solo administradores.")
    @GetMapping
    public List<DoctorDTO> list(@Parameter(description = "Filtro por nombre") @RequestParam(required = false) String name,
                                 @Parameter(description = "Filtro por centro de salud") @RequestParam(required = false) Long healthCenterId) {
        return doctorService.list(name, healthCenterId);
    }

    @Operation(summary = "Consultar un médico",
            description = "Un administrador o el propio médico.")
    @GetMapping("/{id}")
    public DoctorDTO findById(@PathVariable Long id) {
        access.requireAdminOrSelf(UserType.DOCTOR, id);
        return doctorService.findById(id);
    }

    @Operation(summary = "Crear un médico",
            description = "Su contraseña inicial es su número de colegiado. Solo administradores.")
    @PostMapping
    public ResponseEntity<DoctorDTO> create(@Valid @RequestBody DoctorDTO dto) {
        DoctorDTO created = doctorService.create(dto);
        return ResponseEntity.created(URI.create("/api/doctors/" + created.id())).body(created);
    }

    @Operation(summary = "Modificar un médico",
            description = "Solo administradores.")
    @PutMapping("/{id}")
    public DoctorDTO update(@PathVariable Long id, @Valid @RequestBody DoctorDTO dto) {
        return doctorService.update(id, dto);
    }

    @Operation(summary = "Dar de baja un médico",
            description = "Baja lógica. Solo administradores.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        doctorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Agenda de un día",
            description = "Citas del médico en el día indicado (hoy por defecto), ordenadas por hora. Un administrador o el propio médico.")
    @GetMapping("/{id}/schedule")
    public List<AppointmentDTO> schedule(@PathVariable Long id,
                                 @Parameter(description = "Día en formato AAAA-MM-DD; hoy por defecto") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate day) {
        access.requireAdminOrSelf(UserType.DOCTOR, id);
        return appointmentService.scheduleForDay(id, day);
    }

    @Operation(summary = "Modificar los datos propios",
            description = "El médico no puede cambiar su centro de salud.")
    @PatchMapping("/{id}/profile")
    public es.uvigo.dagss.recetas.dto.DoctorDTO updateOwnProfile(@PathVariable Long id,
            @Valid @RequestBody es.uvigo.dagss.recetas.dto.DoctorProfileUpdateRequest request) {
        access.requireSelf(UserType.DOCTOR, id);
        return doctorService.updateOwnProfile(id, request);
    }

    @Operation(summary = "Cambiar la contraseña propia",
            description = "Exige la contraseña actual.")
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> changeOwnPassword(@PathVariable Long id,
            @Valid @RequestBody es.uvigo.dagss.recetas.dto.PasswordChangeRequest request) {
        access.requireSelf(UserType.DOCTOR, id);
        doctorService.changeOwnPassword(id, request);
        return ResponseEntity.noContent().build();
    }
}
