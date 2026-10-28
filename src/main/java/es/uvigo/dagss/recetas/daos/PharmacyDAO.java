package es.uvigo.dagss.recetas.daos;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Pharmacy;

public interface PharmacyDAO extends JpaRepository<Pharmacy, Long> {
    List<Pharmacy> findByNameContaining(String name);

    List<Pharmacy> findByAddressCityContaining(String city);

    Optional<Pharmacy> findByUsernameIgnoreCase(String username);

    boolean existsByTaxId(String taxId);

    boolean existsByTaxIdAndIdNot(String taxId, Long id);

    boolean existsByPharmacistLicenseNumber(String pharmacistLicenseNumber);

    boolean existsByPharmacistLicenseNumberAndIdNot(String pharmacistLicenseNumber, Long id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
