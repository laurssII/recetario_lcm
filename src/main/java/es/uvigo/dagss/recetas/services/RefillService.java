package es.uvigo.dagss.recetas.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.PharmacyDAO;
import es.uvigo.dagss.recetas.daos.RefillDAO;
import es.uvigo.dagss.recetas.dto.RefillDTO;
import es.uvigo.dagss.recetas.entities.Pharmacy;
import es.uvigo.dagss.recetas.entities.Refill;
import es.uvigo.dagss.recetas.entities.RefillStatus;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;
import es.uvigo.dagss.recetas.validation.InputNormalizer;

@Service
@Transactional
public class RefillService {

    private final RefillDAO refillDAO;
    private final PharmacyDAO pharmacyDAO;

    public RefillService(RefillDAO refillDAO, PharmacyDAO pharmacyDAO) {
        this.refillDAO = refillDAO;
        this.pharmacyDAO = pharmacyDAO;
    }

    @Transactional(readOnly = true)
    public List<RefillDTO> pendingForPatient(Long patientId) {
        return refillDAO.findByPrescriptionPatientIdAndStatusOrderByValidFromAsc(patientId, RefillStatus.SCHEDULED)
                .stream().map(RefillDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public List<RefillDTO> pendingByHealthCardNumber(String healthCardNumber) {
        return refillDAO.findByPrescriptionPatientHealthCardNumberAndStatusOrderByValidFromAsc(
                        InputNormalizer.upper(healthCardNumber), RefillStatus.SCHEDULED)
                .stream().map(RefillDTO::from).toList();
    }

    public RefillDTO fill(Long refillId, Long pharmacyId) {
        Refill refill = refillDAO.findById(refillId)
                .orElseThrow(() -> ResourceNotFoundException.of("Refill", refillId));
        if (refill.getStatus() != RefillStatus.SCHEDULED) {
            throw new InvalidOperationException("Only refills with status SCHEDULED can be filled");
        }
        LocalDate today = LocalDate.now();
        if (today.isBefore(refill.getValidFrom()) || today.isAfter(refill.getValidUntil())) {
            throw new InvalidOperationException("The refill is outside its validity window ("
                    + refill.getValidFrom() + " to " + refill.getValidUntil() + ")");
        }
        Pharmacy pharmacy = pharmacyDAO.findById(pharmacyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", pharmacyId));

        refill.setPharmacy(pharmacy);
        refill.setStatus(RefillStatus.FILLED);
        return RefillDTO.from(refillDAO.save(refill));
    }
}
