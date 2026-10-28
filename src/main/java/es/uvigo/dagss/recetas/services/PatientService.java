package es.uvigo.dagss.recetas.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.DoctorDAO;
import es.uvigo.dagss.recetas.daos.HealthCenterDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.dto.PatientDTO;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.HealthCenter;
import es.uvigo.dagss.recetas.entities.Patient;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;
import es.uvigo.dagss.recetas.validation.PasswordPolicy;

@Service
@Transactional
public class PatientService {

    private final PatientDAO patientDAO;
    private final HealthCenterDAO healthCenterDAO;
    private final DoctorDAO doctorDAO;
    private final UniquenessChecker uniqueness;
    private final PasswordEncoder passwordEncoder;

    public PatientService(PatientDAO patientDAO, HealthCenterDAO healthCenterDAO, DoctorDAO doctorDAO,
                          UniquenessChecker uniqueness, PasswordEncoder passwordEncoder) {
        this.patientDAO = patientDAO;
        this.healthCenterDAO = healthCenterDAO;
        this.doctorDAO = doctorDAO;
        this.uniqueness = uniqueness;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<PatientDTO> list(String name, Long healthCenterId, Long doctorId) {
        List<Patient> result;
        if (doctorId != null) {
            result = patientDAO.findByDoctorId(doctorId);
        } else if (healthCenterId != null) {
            result = patientDAO.findByHealthCenterId(healthCenterId);
        } else if (name != null && !name.isBlank()) {
            result = patientDAO.findByNameContaining(name);
        } else {
            result = patientDAO.findAll();
        }
        if (name != null && !name.isBlank() && (doctorId != null || healthCenterId != null)) {
            result = result.stream().filter(p -> p.getName().toLowerCase().contains(name.toLowerCase())).toList();
        }
        return result.stream().map(PatientDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PatientDTO findById(Long id) {
        return PatientDTO.from(getOrFail(id));
    }

    public PatientDTO create(PatientDTO dto) {
        HealthCenter center = getHealthCenterOrFail(dto.healthCenterId());
        Doctor doctor = getDoctorOrFail(dto.doctorId());
        checkDoctorBelongsToCenter(doctor, center);
        uniqueness.checkUsername(dto.username(), null);
        uniqueness.checkPatient(dto.nationalId(), dto.healthCardNumber(), null);
        Patient patient = new Patient(dto.lastName(), dto.nationalId(), dto.healthCardNumber(), dto.socialSecurityNumber(),
                dto.address() != null ? dto.address().toEntity() : null, dto.phone(),
                dto.birthDate(), center, doctor, dto.username(), passwordEncoder.encode(dto.nationalId()), dto.name(), dto.email());
        return PatientDTO.from(patientDAO.save(patient));
    }

    public PatientDTO update(Long id, PatientDTO dto) {
        Patient patient = getOrFail(id);
        uniqueness.checkPatient(dto.nationalId(), dto.healthCardNumber(), id);
        patient.setName(dto.name());
        patient.setLastName(dto.lastName());
        patient.setNationalId(dto.nationalId());
        patient.setHealthCardNumber(dto.healthCardNumber());
        patient.setSocialSecurityNumber(dto.socialSecurityNumber());
        if (dto.address() != null) patient.setAddress(dto.address().toEntity());
        patient.setPhone(dto.phone());
        patient.setEmail(dto.email());
        patient.setBirthDate(dto.birthDate());
        // A patient cannot change their own health center or doctor from their profile;
        // this full update is reserved for administrators.
        HealthCenter center = getHealthCenterOrFail(dto.healthCenterId());
        Doctor doctor = getDoctorOrFail(dto.doctorId());
        checkDoctorBelongsToCenter(doctor, center);
        patient.setHealthCenter(center);
        patient.setDoctor(doctor);
        return PatientDTO.from(patientDAO.save(patient));
    }

    public void deactivate(Long id) {
        Patient patient = getOrFail(id);
        patient.deactivate();
        patientDAO.save(patient);
    }

    public PatientDTO updateOwnProfile(Long id, es.uvigo.dagss.recetas.dto.PatientProfileUpdateRequest request) {
        Patient patient = getOrFail(id);
        patient.setName(request.name());
        patient.setLastName(request.lastName());
        if (request.address() != null) patient.setAddress(request.address().toEntity());
        patient.setPhone(request.phone());
        patient.setEmail(request.email());
        return PatientDTO.from(patientDAO.save(patient));
    }

    public void changeOwnPassword(Long id, es.uvigo.dagss.recetas.dto.PasswordChangeRequest request) {
        Patient patient = getOrFail(id);
        if (!passwordEncoder.matches(request.currentPassword(), patient.getPassword())) {
            throw new InvalidOperationException("La contraseña actual no es correcta");
        }
        PasswordPolicy.check(request.newPassword());
        patient.setPassword(passwordEncoder.encode(request.newPassword()));
        patientDAO.save(patient);
    }

    private Patient getOrFail(Long id) {
        return patientDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Patient", id));
    }

    private HealthCenter getHealthCenterOrFail(Long id) {
        return healthCenterDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("HealthCenter", id));
    }

    /** The assigned doctor must work at the patient's health center. */
    private void checkDoctorBelongsToCenter(Doctor doctor, HealthCenter center) {
        if (doctor.getHealthCenter() == null || !doctor.getHealthCenter().getId().equals(center.getId())) {
            throw new InvalidOperationException("El médico asignado no pertenece al centro de salud seleccionado");
        }
    }

    private Doctor getDoctorOrFail(Long id) {
        return doctorDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Doctor", id));
    }
}
