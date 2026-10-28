package es.uvigo.dagss.recetas.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.PharmacyDAO;
import es.uvigo.dagss.recetas.dto.PharmacyDTO;
import es.uvigo.dagss.recetas.entities.Pharmacy;
import es.uvigo.dagss.recetas.exceptions.InvalidOperationException;
import es.uvigo.dagss.recetas.exceptions.ResourceNotFoundException;
import es.uvigo.dagss.recetas.validation.PasswordPolicy;

@Service
@Transactional
public class PharmacyService {

    private final PharmacyDAO pharmacyDAO;
    private final UniquenessChecker uniqueness;
    private final PasswordEncoder passwordEncoder;

    public PharmacyService(PharmacyDAO pharmacyDAO, UniquenessChecker uniqueness, PasswordEncoder passwordEncoder) {
        this.pharmacyDAO = pharmacyDAO;
        this.uniqueness = uniqueness;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<PharmacyDTO> list(String name) {
        List<Pharmacy> result = (name != null && !name.isBlank())
                ? pharmacyDAO.findByNameContaining(name)
                : pharmacyDAO.findAll();
        return result.stream().map(PharmacyDTO::from).toList();
    }

    @Transactional(readOnly = true)
    public PharmacyDTO findById(Long id) {
        return PharmacyDTO.from(getOrFail(id));
    }

    public PharmacyDTO create(PharmacyDTO dto) {
        uniqueness.checkUsername(dto.username(), null);
        uniqueness.checkPharmacy(dto.taxId(), dto.pharmacistLicenseNumber(), null);
        Pharmacy pharmacy = new Pharmacy(dto.pharmacistFirstName(), dto.pharmacistLastName(), dto.taxId(),
                dto.pharmacistLicenseNumber(), dto.address() != null ? dto.address().toEntity() : null,
                dto.phone(), dto.username(), passwordEncoder.encode(dto.pharmacistLicenseNumber()), dto.name(), dto.email());
        return PharmacyDTO.from(pharmacyDAO.save(pharmacy));
    }

    public PharmacyDTO update(Long id, PharmacyDTO dto) {
        Pharmacy pharmacy = getOrFail(id);
        uniqueness.checkPharmacy(dto.taxId(), dto.pharmacistLicenseNumber(), id);
        pharmacy.setName(dto.name());
        pharmacy.setPharmacistFirstName(dto.pharmacistFirstName());
        pharmacy.setPharmacistLastName(dto.pharmacistLastName());
        pharmacy.setTaxId(dto.taxId());
        pharmacy.setPharmacistLicenseNumber(dto.pharmacistLicenseNumber());
        if (dto.address() != null) pharmacy.setAddress(dto.address().toEntity());
        pharmacy.setPhone(dto.phone());
        pharmacy.setEmail(dto.email());
        return PharmacyDTO.from(pharmacyDAO.save(pharmacy));
    }

    public void deactivate(Long id) {
        Pharmacy pharmacy = getOrFail(id);
        pharmacy.deactivate();
        pharmacyDAO.save(pharmacy);
    }

    public PharmacyDTO updateOwnProfile(Long id, es.uvigo.dagss.recetas.dto.PharmacyProfileUpdateRequest request) {
        Pharmacy pharmacy = getOrFail(id);
        pharmacy.setName(request.name());
        pharmacy.setPharmacistFirstName(request.pharmacistFirstName());
        pharmacy.setPharmacistLastName(request.pharmacistLastName());
        if (request.address() != null) pharmacy.setAddress(request.address().toEntity());
        pharmacy.setPhone(request.phone());
        pharmacy.setEmail(request.email());
        return PharmacyDTO.from(pharmacyDAO.save(pharmacy));
    }

    public void changeOwnPassword(Long id, es.uvigo.dagss.recetas.dto.PasswordChangeRequest request) {
        Pharmacy pharmacy = getOrFail(id);
        if (!passwordEncoder.matches(request.currentPassword(), pharmacy.getPassword())) {
            throw new InvalidOperationException("La contraseña actual no es correcta");
        }
        PasswordPolicy.check(request.newPassword());
        pharmacy.setPassword(passwordEncoder.encode(request.newPassword()));
        pharmacyDAO.save(pharmacy);
    }

    private Pharmacy getOrFail(Long id) {
        return pharmacyDAO.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Pharmacy", id));
    }
}
