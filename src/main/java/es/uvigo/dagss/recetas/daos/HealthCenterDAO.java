package es.uvigo.dagss.recetas.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.HealthCenter;

public interface HealthCenterDAO extends JpaRepository<HealthCenter, Long> {
    List<HealthCenter> findByNameContaining(String name);

    List<HealthCenter> findByAddressCityContaining(String city);
}
