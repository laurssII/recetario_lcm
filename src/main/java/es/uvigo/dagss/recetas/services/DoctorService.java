package es.uvigo.dagss.recetas.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.DoctorDAO;
import es.uvigo.dagss.recetas.daos.HealthCenterDAO;
import es.uvigo.dagss.recetas.dto.DoctorDTO;
import es.uvigo.dagss.recetas.entities.Doctor;
import es.uvigo.dagss.recetas.entities.HealthCenter;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;
import es.uvigo.dagss.recetas.validation.PasswordPolicy;

@Service
@Transactional
public class DoctorService {

    private final DoctorDAO doctorDAO;
    private final HealthCenterDAO healthCenterDAO;
    private final UniquenessChecker uniqueness;
    private final PasswordEncoder passwordEncoder;

    public DoctorService(DoctorDAO doctorDAO, HealthCenterDAO healthCenterDAO, UniquenessChecker uniqueness,
                         PasswordEncoder passwordEncoder) {
        this.doctorDAO = doctorDAO;
        this.healthCenterDAO = healthCenterDAO;
        this.uniqueness = uniqueness;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<DoctorDTO> list(String name, Long healthCenterId) {
        List<Doctor> result;
        if (healthCenterId != null) {
            result = doctorDAO.findByHealthCenterId(healthCenterId);
            if (name != null && !name.isBlank()) {
                result = result.stream().filter(d -> d.getName().toLowerCase().contains(name.toLowerCase())).toList();
            }
        } else if (name != null && !name.isBlank()) {
            result = doctorDAO.findByNameContaining(name);
        } else {
            result = doctorDAO.findAll();
        }
        return result.stream().map(DoctorDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public DoctorDTO findById(Long id) {
        return DoctorDTO.from(getOrFail(id));
    }

    public DoctorDTO create(DoctorDTO dto) {
        HealthCenter center = getHealthCenterOrFail(dto.healthCenterId());
        uniqueness.checkUsername(dto.username(), null);
        uniqueness.checkDoctor(dto.nationalId(), dto.licenseNumber(), null);
        Doctor doctor = new Doctor(dto.lastName(), dto.nationalId(), dto.licenseNumber(), dto.phone(), center,
                dto.username(), passwordEncoder.encode(dto.licenseNumber()), dto.name(), dto.email());
        return DoctorDTO.from(doctorDAO.save(doctor));
    }

    public DoctorDTO update(Long id, DoctorDTO dto) {
        Doctor doctor = getOrFail(id);
        uniqueness.checkDoctor(dto.nationalId(), dto.licenseNumber(), id);
        doctor.setName(dto.name());
        doctor.setLastName(dto.lastName());
        doctor.setNationalId(dto.nationalId());
        doctor.setLicenseNumber(dto.licenseNumber());
        doctor.setPhone(dto.phone());
        doctor.setEmail(dto.email());
        doctor.setHealthCenter(getHealthCenterOrFail(dto.healthCenterId()));
        return DoctorDTO.from(doctorDAO.save(doctor));
    }

    public void deactivate(Long id) {
        Doctor doctor = getOrFail(id);
        doctor.deactivate();
        doctorDAO.save(doctor);
    }

    public DoctorDTO updateOwnProfile(Long id, es.uvigo.dagss.recetas.dto.DoctorProfileUpdateRequest request) {
        Doctor doctor = getOrFail(id);
        doctor.setName(request.name());
        doctor.setLastName(request.lastName());
        doctor.setPhone(request.phone());
        doctor.setEmail(request.email());
        return DoctorDTO.from(doctorDAO.save(doctor));
    }

    public void changeOwnPassword(Long id, es.uvigo.dagss.recetas.dto.PasswordChangeRequest request) {
        Doctor doctor = getOrFail(id);
        if (!passwordEncoder.matches(request.currentPassword(), doctor.getPassword())) {
            throw new InvalidOperationException("La contraseña actual no es correcta");
        }
        PasswordPolicy.check(request.newPassword());
        doctor.setPassword(passwordEncoder.encode(request.newPassword()));
        doctorDAO.save(doctor);
    }

    private Doctor getOrFail(Long id) {
        return doctorDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Doctor", id));
    }

    private HealthCenter getHealthCenterOrFail(Long id) {
        return healthCenterDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("HealthCenter", id));
    }
}
