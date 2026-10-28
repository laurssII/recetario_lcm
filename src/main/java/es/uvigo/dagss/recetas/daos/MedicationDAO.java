package es.uvigo.dagss.recetas.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Medication;

public interface MedicationDAO extends JpaRepository<Medication, Long> {
    List<Medication> findByBrandNameContaining(String brandName);

    List<Medication> findByActiveIngredientContaining(String activeIngredient);

    List<Medication> findByManufacturerContaining(String manufacturer);

    List<Medication> findByCategoryContaining(String category);
}
