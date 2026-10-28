package es.uvigo.dagss.recetas.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.uvigo.dagss.recetas.daos.AdministratorDAO;
import es.uvigo.dagss.recetas.daos.DoctorDAO;
import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.daos.PharmacyDAO;
import es.uvigo.dagss.recetas.dto.LoginRequest;
import es.uvigo.dagss.recetas.dto.LoginResponseDTO;
import es.uvigo.dagss.recetas.entities.UserType;
import es.uvigo.dagss.recetas.exceptions.AuthenticationFailedException;
import es.uvigo.dagss.recetas.security.JwtService;

/**
 * Looks the username up (ignoring case) in the four user tables, since User uses TABLE_PER_CLASS and there is no
 * single users table. A wrong username, a wrong password and a deactivated account give the same error, and a hash
 * comparison is always done, even for unknown usernames, so neither the message nor the response time reveals
 * which usernames exist.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";

    private final AdministratorDAO administratorDAO;
    private final DoctorDAO doctorDAO;
    private final PatientDAO patientDAO;
    private final PharmacyDAO pharmacyDAO;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** A valid hash of a throw-away value, compared against when the username is unknown. */
    private final String dummyHash;

    public AuthService(AdministratorDAO administratorDAO, DoctorDAO doctorDAO, PatientDAO patientDAO,
                       PharmacyDAO pharmacyDAO, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.administratorDAO = administratorDAO;
        this.doctorDAO = doctorDAO;
        this.patientDAO = patientDAO;
        this.pharmacyDAO = pharmacyDAO;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    public LoginResponseDTO login(LoginRequest request) {
        String username = request.username();
        var admin = administratorDAO.findByUsernameIgnoreCase(username);
        if (admin.isPresent()) {
            return authenticate(admin.get(), request.password(), UserType.ADMINISTRATOR, null);
        }
        var doctor = doctorDAO.findByUsernameIgnoreCase(username);
        if (doctor.isPresent()) {
            return authenticate(doctor.get(), request.password(), UserType.DOCTOR, doctor.get().getLastName());
        }
        var patient = patientDAO.findByUsernameIgnoreCase(username);
        if (patient.isPresent()) {
            return authenticate(patient.get(), request.password(), UserType.PATIENT, patient.get().getLastName());
        }
        var pharmacy = pharmacyDAO.findByUsernameIgnoreCase(username);
        if (pharmacy.isPresent()) {
            return authenticate(pharmacy.get(), request.password(), UserType.PHARMACY, null);
        }
        passwordEncoder.matches(request.password(), dummyHash); // same work as for a known user
        throw new AuthenticationFailedException(INVALID_CREDENTIALS);
    }

    private LoginResponseDTO authenticate(es.uvigo.dagss.recetas.entities.User user, String password, UserType role,
                                          String lastName) {
        boolean passwordMatches = passwordEncoder.matches(password, user.getPassword());
        if (!passwordMatches || !Boolean.TRUE.equals(user.getActive())) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        JwtService.IssuedToken token = jwtService.issue(user.getId(), user.getUsername(), role);
        return new LoginResponseDTO(user.getId(), role, user.getName(), lastName, token.value(), token.expiresAt());
    }
}
