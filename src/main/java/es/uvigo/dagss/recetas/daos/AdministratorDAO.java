package es.uvigo.dagss.recetas.daos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Administrator;

public interface AdministratorDAO extends JpaRepository<Administrator, Long> {
    Optional<Administrator> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);
}
