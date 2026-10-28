package es.uvigo.dagss.recetas.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Refill;
import es.uvigo.dagss.recetas.entities.RefillStatus;

public interface RefillDAO extends JpaRepository<Refill, Long> {

    List<Refill> findByPrescriptionPatientIdAndStatusOrderByValidFromAsc(Long patientId, RefillStatus status);

    List<Refill> findByPrescriptionPatientHealthCardNumberAndStatusOrderByValidFromAsc(String healthCardNumber, RefillStatus status);

    List<Refill> findByPrescriptionId(Long prescriptionId);
}
