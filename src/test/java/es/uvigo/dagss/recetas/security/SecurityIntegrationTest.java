package es.uvigo.dagss.recetas.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import es.uvigo.dagss.recetas.daos.PatientDAO;
import es.uvigo.dagss.recetas.validation.SpanishIds;

/**
 * End-to-end checks of authentication, authorization and uniqueness through the whole stack (filters, controllers,
 * services, H2). They rely on the sample data of DataInitializer: admin/admin, lfernandez/LIC-4501 and
 * ciglesias/LIC-4502 (doctors), xrodriguez/20000001Y (patient) and compostela/PLIC-778899 (pharmacy).
 * Tests that create records use their own DNIs, health cards and usernames, so they are independent.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private PatientDAO patientDAO;

    private record Session(long id, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    private Session login(String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("username", username, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = json.readTree(body);
        return new Session(node.get("id").asLong(), node.get("token").asText());
    }

    private Session admin() throws Exception {
        return login("admin", "admin");
    }

    private JsonNode getJson(Session session, String url) throws Exception {
        String body = mvc.perform(get(url).header("Authorization", session.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    /** A valid DNI built from a number (8 digits + the right control letter). */
    private static String dni(int number) {
        return String.format("%08d", number) + SpanishIds.dniControlLetter(number);
    }

    /** Request body of a new patient assigned to the given (seeded) doctor, at that doctor's health center. */
    private ObjectNode patientBody(Session admin, String doctorUsername, String nationalId, String healthCard, String username)
            throws Exception {
        JsonNode doctor = null;
        for (JsonNode d : getJson(admin, "/api/doctors")) {
            if (doctorUsername.equals(d.get("username").asText())) {
                doctor = d;
            }
        }
        assertThat(doctor).as("seeded doctor " + doctorUsername).isNotNull();
        ObjectNode body = json.createObjectNode();
        body.put("name", "Test").put("lastName", "Paciente").put("nationalId", nationalId)
                .put("healthCardNumber", healthCard).put("socialSecurityNumber", "123456789012");
        body.putObject("address").put("street", "1 Test Street").put("city", "Vigo")
                .put("postalCode", "36201").put("province", "Pontevedra");
        body.put("phone", "986000000").put("email", "test@example.com").put("birthDate", "1990-01-01")
                .put("healthCenterId", doctor.get("healthCenterId").asLong()).put("doctorId", doctor.get("id").asLong())
                .put("username", username);
        return body;
    }

    private ResultActions postPatient(Session admin, ObjectNode body) throws Exception {
        return mvc.perform(post("/api/patients").header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private long createPatient(Session admin, String doctorUsername, String nationalId, String healthCard, String username)
            throws Exception {
        String response = postPatient(admin, patientBody(admin, doctorUsername, nationalId, healthCard, username))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asLong();
    }

    @Test
    void loginReturnsATokenAndTheRoleOfTheUser() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"xrodriguez\",\"password\":\"20000001Y\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("PATIENT"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void loginIgnoresTheCaseOfTheUsername() throws Exception {
        login("XRodriguez", "20000001Y");
    }

    @Test
    void wrongPasswordOrUnknownUserGetTheSame401() throws Exception {
        for (String[] credentials : new String[][] {{"xrodriguez", "wrong-password"}, {"nobody", "20000001Y"}}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(Map.of("username", credentials[0], "password", credentials[1]))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Usuario o contraseña incorrectos"));
        }
    }

    @Test
    void passwordsAreStoredHashedNeverInPlainText() {
        String stored = patientDAO.findByUsernameIgnoreCase("xrodriguez").orElseThrow().getPassword();
        assertThat(stored).isNotEqualTo("20000001Y").startsWith("$2");
    }

    @Test
    void requestsWithoutAValidTokenAreRejectedWithJson401() throws Exception {
        mvc.perform(get("/api/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").isNotEmpty());
        mvc.perform(get("/api/patients").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/patients").header("Authorization", "Bearer " + admin().token() + "tampered"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theRepositoriesAreNotExposedOutsideTheApi() throws Exception {
        // Spring Data REST used to publish /patients, /doctors... (password included) with no authentication
        mvc.perform(get("/patients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/patients").header("Authorization", admin().bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiDocumentationAndPreflightRequestsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(options("/api/patients/1/profile")
                        .header("Origin", "http://localhost:3000")
                        .header("Access-Control-Request-Method", "PATCH")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Access-Control-Allow-Origin"));
    }

    @Test
    void administrationEndpointsAreOnlyForAdministrators() throws Exception {
        Session patient = login("xrodriguez", "20000001Y");
        Session doctor = login("lfernandez", "LIC-4501");
        Session pharmacy = login("compostela", "PLIC-778899");
        for (Session s : new Session[] {patient, doctor, pharmacy}) {
            mvc.perform(get("/api/administrators").header("Authorization", s.bearer())).andExpect(status().isForbidden());
            mvc.perform(get("/api/patients").header("Authorization", s.bearer())).andExpect(status().isForbidden());
            mvc.perform(get("/api/doctors").header("Authorization", s.bearer())).andExpect(status().isForbidden());
            mvc.perform(get("/api/appointments?day=2030-01-01").header("Authorization", s.bearer()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/patients").header("Authorization", admin().bearer())).andExpect(status().isOk());
    }

    @Test
    void anyLoggedInUserCanReadHealthCentersAndMedicationsButOnlyAdminsCanChangeThem() throws Exception {
        Session patient = login("xrodriguez", "20000001Y");
        mvc.perform(get("/api/health-centers").header("Authorization", patient.bearer())).andExpect(status().isOk());
        mvc.perform(get("/api/medications").header("Authorization", patient.bearer())).andExpect(status().isOk());
        mvc.perform(post("/api/health-centers").header("Authorization", patient.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPatientCanReadOnlyTheirOwnRecordAndItNeverContainsAPassword() throws Exception {
        Session me = login("xrodriguez", "20000001Y");
        Session other = admin();
        long otherPatientId = createPatient(other, "lfernandez", dni(41000001), "ABCD0000410001", "otro.paciente1");

        mvc.perform(get("/api/patients/" + me.id()).header("Authorization", me.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/patients/" + otherPatientId).header("Authorization", me.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/" + otherPatientId + "/appointments").header("Authorization", me.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/" + otherPatientId + "/refills").header("Authorization", me.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/" + me.id() + "/refills").header("Authorization", me.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void aDoctorSeesOnlyTheirOwnPatientsAndTheirOwnProfile() throws Exception {
        Session admin = admin();
        long patientId = createPatient(admin, "lfernandez", dni(41000002), "ABCD0000410002", "otro.paciente2");
        Session assignedDoctor = login("lfernandez", "LIC-4501");
        Session otherDoctor = login("ciglesias", "LIC-4502");

        mvc.perform(get("/api/patients/" + patientId).header("Authorization", assignedDoctor.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/patients/" + patientId + "/prescriptions").header("Authorization", assignedDoctor.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/patients/" + patientId).header("Authorization", otherDoctor.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/patients/" + patientId + "/prescriptions").header("Authorization", otherDoctor.bearer()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/doctors/" + assignedDoctor.id()).header("Authorization", assignedDoctor.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/doctors/" + assignedDoctor.id()).header("Authorization", otherDoctor.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/doctors/" + assignedDoctor.id() + "/password").header("Authorization", otherDoctor.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"LIC-4501\",\"newPassword\":\"hacked-pass\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPatientCannotBookAnAppointmentForSomeoneElse() throws Exception {
        Session me = login("xrodriguez", "20000001Y");
        long otherPatientId = createPatient(admin(), "lfernandez", dni(41000003), "ABCD0000410003", "otro.paciente3");
        mvc.perform(post("/api/appointments").header("Authorization", me.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"patientId\":" + otherPatientId + ",\"dateTime\":\"2030-01-01T10:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aPharmacyCannotActAsAnotherPharmacyOrPrescribe() throws Exception {
        Session pharmacy = login("compostela", "PLIC-778899");
        mvc.perform(post("/api/refills/1/fill?pharmacyId=" + (pharmacy.id() + 1)).header("Authorization", pharmacy.bearer()))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/prescriptions").header("Authorization", pharmacy.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void aNewPatientCanLogInWithTheirDniAndThenChangeThePassword() throws Exception {
        Session admin = admin();
        String nationalId = dni(41000004);
        createPatient(admin, "lfernandez", nationalId, "ABCD0000410004", "otro.paciente4");

        Session me = login("otro.paciente4", nationalId);
        mvc.perform(patch("/api/patients/" + me.id() + "/password").header("Authorization", me.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"nueva-clave\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/patients/" + me.id() + "/password").header("Authorization", me.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + nationalId + "\",\"newPassword\":\"nueva-clave\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"otro.paciente4\",\"password\":\"" + nationalId + "\"}"))
                .andExpect(status().isUnauthorized());
        login("otro.paciente4", "nueva-clave");
    }

    @Test
    void administratorsAreCreatedWithThePasswordInTheBody() throws Exception {
        Session admin = admin();
        mvc.perform(post("/api/administrators").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Nueva Admin\",\"email\":\"nueva@example.com\",\"username\":\"nueva.admin\",\"password\":\"clave-admin\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.password").doesNotExist());
        login("nueva.admin", "clave-admin");
        // the password is mandatory: there is no default one any more
        mvc.perform(post("/api/administrators").header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Otra Admin\",\"email\":\"otra@example.com\",\"username\":\"otra.admin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aDuplicateDniIsRejectedWith409AndTheOffendingField() throws Exception {
        Session admin = admin();
        createPatient(admin, "lfernandez", dni(41000005), "ABCD0000410005", "otro.paciente5");

        postPatient(admin, patientBody(admin, "lfernandez", dni(41000005), "ABCD0000410006", "otro.paciente6"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.nationalId").isNotEmpty());
    }

    @Test
    void aDuplicateHealthCardIsRejectedWith409() throws Exception {
        Session admin = admin();
        createPatient(admin, "lfernandez", dni(41000007), "ABCD0000410007", "otro.paciente7");

        postPatient(admin, patientBody(admin, "lfernandez", dni(41000008), "ABCD0000410007", "otro.paciente8"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.healthCardNumber").isNotEmpty());
    }

    @Test
    void aUsernameCannotBeRepeatedAcrossRolesNorWithDifferentCase() throws Exception {
        Session admin = admin();
        postPatient(admin, patientBody(admin, "lfernandez", dni(41000009), "ABCD0000410009", "XRodriguez"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
        postPatient(admin, patientBody(admin, "lfernandez", dni(41000010), "ABCD0000410010", "lfernandez"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
        postPatient(admin, patientBody(admin, "lfernandez", dni(41000011), "ABCD0000410011", "admin"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.username").isNotEmpty());
    }

    @Test
    void invalidDataStillGets400NotAConflict() throws Exception {
        Session admin = admin();
        ObjectNode body = patientBody(admin, "lfernandez", "12345678A", "ABCD0000410012", "otro.paciente12");   // wrong DNI letter
        postPatient(admin, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nationalId").isNotEmpty());
    }
}
