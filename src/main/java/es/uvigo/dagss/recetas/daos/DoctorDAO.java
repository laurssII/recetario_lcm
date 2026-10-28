package es.uvigo.dagss.recetas.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Doctor;

public interface DoctorDAO extends JpaRepository<Doctor, Long> {
    List<Doctor> findByNameContaining(String name);

    List<Doctor> findByHealthCenterId(Long healthCenterId);

    Optional<Doctor> findByUsernameIgnoreCase(String username);

    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
