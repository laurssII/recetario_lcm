package es.uvigo.dagss.recetas.daos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.uvigo.dagss.recetas.entities.Prescription;

public interface PrescriptionDAO extends JpaRepository<Prescription, Long> {

    List<Prescription> findByPatientIdAndActiveOrderByStartDateAsc(Long patientId, boolean active);
}
