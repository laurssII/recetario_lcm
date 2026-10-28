package es.uvigo.dagss.recetas.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Patient;

public interface PatientDAO extends JpaRepository<Patient, Long> {
    List<Patient> findByNameContaining(String name);

    List<Patient> findByAddressCityContaining(String city);

    List<Patient> findByHealthCenterId(Long healthCenterId);

    List<Patient> findByDoctorId(Long doctorId);

    Optional<Patient> findByUsernameIgnoreCase(String username);

    boolean existsByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    boolean existsByHealthCardNumber(String healthCardNumber);

    boolean existsByHealthCardNumberAndIdNot(String healthCardNumber, Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
