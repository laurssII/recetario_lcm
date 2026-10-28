package es.uvigo.dagss.recetas.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.MedicationDAO;
import es.uvigo.dagss.recetas.dto.MedicationDTO;
import es.uvigo.dagss.recetas.entities.Medication;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class MedicationService {

    private final MedicationDAO medicationDAO;

    public MedicationService(MedicationDAO medicationDAO) {
        this.medicationDAO = medicationDAO;
    }

    @Transactional(readOnly = true)
    public List<MedicationDTO> search(String brandName, String activeIngredient, String manufacturer, String category) {
        List<Medication> result;
        if (brandName != null && !brandName.isBlank()) {
            result = medicationDAO.findByBrandNameContaining(brandName);
        } else if (activeIngredient != null && !activeIngredient.isBlank()) {
            result = medicationDAO.findByActiveIngredientContaining(activeIngredient);
        } else if (manufacturer != null && !manufacturer.isBlank()) {
            result = medicationDAO.findByManufacturerContaining(manufacturer);
        } else if (category != null && !category.isBlank()) {
            result = medicationDAO.findByCategoryContaining(category);
        } else {
            result = medicationDAO.findAll();
        }
        return result.stream().map(MedicationDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public MedicationDTO findById(Long id) {
        return MedicationDTO.from(getOrFail(id));
    }

    public MedicationDTO create(MedicationDTO dto) {
        Medication medication = new Medication(dto.brandName(), dto.activeIngredient(),
                dto.manufacturer(), dto.category(), dto.unitsPerPackage(), true);
        return MedicationDTO.from(medicationDAO.save(medication));
    }

    public MedicationDTO update(Long id, MedicationDTO dto) {
        Medication medication = getOrFail(id);
        medication.setBrandName(dto.brandName());
        medication.setActiveIngredient(dto.activeIngredient());
        medication.setManufacturer(dto.manufacturer());
        medication.setCategory(dto.category());
        medication.setUnitsPerPackage(dto.unitsPerPackage());
        return MedicationDTO.from(medicationDAO.save(medication));
    }

    public void deactivate(Long id) {
        Medication medication = getOrFail(id);
        medication.setActive(false);
        medicationDAO.save(medication);
    }

    private Medication getOrFail(Long id) {
        return medicationDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Medication", id));
    }
}
