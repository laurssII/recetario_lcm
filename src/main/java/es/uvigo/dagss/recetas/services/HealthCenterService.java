package es.uvigo.dagss.recetas.services;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.HealthCenterDAO;
import es.uvigo.dagss.recetas.dto.HealthCenterDTO;
import es.uvigo.dagss.recetas.entities.HealthCenter;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class HealthCenterService {

    private final HealthCenterDAO healthCenterDAO;

    public HealthCenterService(HealthCenterDAO healthCenterDAO) {
        this.healthCenterDAO = healthCenterDAO;
    }

    @Transactional(readOnly = true)
    public List<HealthCenterDTO> list(String name, String city) {
        List<HealthCenter> result;
        if (name != null && !name.isBlank()) {
            result = healthCenterDAO.findByNameContaining(name);
        } else if (city != null && !city.isBlank()) {
            result = healthCenterDAO.findByAddressCityContaining(city);
        } else {
            result = healthCenterDAO.findAll();
        }
        return result.stream().map(HealthCenterDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public HealthCenterDTO findById(Long id) {
        return HealthCenterDTO.from(getOrFail(id));
    }

    public HealthCenterDTO create(HealthCenterDTO dto) {
        HealthCenter center = new HealthCenter(dto.name(), dto.address().toEntity(), dto.phone(), dto.email());
        return HealthCenterDTO.from(healthCenterDAO.save(center));
    }

    public HealthCenterDTO update(Long id, HealthCenterDTO dto) {
        HealthCenter center = getOrFail(id);
        center.setName(dto.name());
        center.setAddress(dto.address().toEntity());
        center.setPhone(dto.phone());
        center.setEmail(dto.email());
        return HealthCenterDTO.from(healthCenterDAO.save(center));
    }

    public void deactivate(Long id) {
        HealthCenter center = getOrFail(id);
        center.setActive(false);
        healthCenterDAO.save(center);
    }

    private HealthCenter getOrFail(Long id) {
        return healthCenterDAO.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("HealthCenter", id));
    }
}
