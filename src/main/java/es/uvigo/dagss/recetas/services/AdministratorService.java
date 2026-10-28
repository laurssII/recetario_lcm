package es.uvigo.dagss.recetas.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.AdministratorDAO;
import es.uvigo.dagss.recetas.dto.AdministratorDTO;
import es.uvigo.dagss.recetas.entities.Administrator;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;
import es.uvigo.dagss.recetas.validation.PasswordPolicy;

@Service
@Transactional
public class AdministratorService {

    private final AdministratorDAO administratorDAO;
    private final UniquenessChecker uniqueness;
    private final PasswordEncoder passwordEncoder;

    public AdministratorService(AdministratorDAO administratorDAO, UniquenessChecker uniqueness,
                                PasswordEncoder passwordEncoder) {
        this.administratorDAO = administratorDAO;
        this.uniqueness = uniqueness;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdministratorDTO> list() {
        return administratorDAO.findAll().stream().map(AdministratorDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public AdministratorDTO findById(Long id) {
        return AdministratorDTO.from(getOrFail(id));
    }

    public AdministratorDTO create(AdministratorDTO dto, String initialPassword) {
        PasswordPolicy.check(initialPassword);
        uniqueness.checkUsername(dto.username(), null);
        Administrator admin = new Administrator();
        admin.setName(dto.name());
        admin.setEmail(dto.email());
        admin.setUsername(dto.username());
        admin.setPassword(passwordEncoder.encode(initialPassword));
        return AdministratorDTO.from(administratorDAO.save(admin));
    }

    public AdministratorDTO update(Long id, AdministratorDTO dto) {
        Administrator admin = getOrFail(id);
        admin.setName(dto.name());
        admin.setEmail(dto.email());
        return AdministratorDTO.from(administratorDAO.save(admin));
    }

    public void deactivate(Long id) {
        Administrator admin = getOrFail(id);
        admin.deactivate();
        administratorDAO.save(admin);
    }

    private Administrator getOrFail(Long id) {
        return administratorDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Administrator", id));
    }
}
