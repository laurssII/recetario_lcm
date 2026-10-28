package es.uvigo.dagss.recetas.flows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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

import es.uvigo.dagss.recetas.validation.SpanishIds;

/**
 * Happy paths of the central flows through the whole stack (filters, controllers, services, H2): booking an appointment and
 * attending it; prescribing, generating the refill plan, dispensing it and cancelling. It relies on the sample data
 * of DataInitializer (doctors lfernandez and ciglesias, pharmacy compostela, ibuprofen with 30 units per package).
 * Each test creates its own patients and books on days far in the future, so tests do not collide.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CentralFlowIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

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

    private Session doctor() throws Exception {
        return login("lfernandez", "LIC-4501");
    }

    private JsonNode getJson(Session session, String url) throws Exception {
        String body = mvc.perform(get(url).header("Authorization", session.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private ResultActions postJson(Session session, String url, Object body) throws Exception {
        return mvc.perform(post(url).header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode postJsonOk(Session session, String url, Object body, int expectedStatus) throws Exception {
        String response = postJson(session, url, body).andExpect(status().is(expectedStatus))
                .andReturn().getResponse().getContentAsString();
        return response.isBlank() ? null : json.readTree(response);
    }

    private static String dni(int number) {
        return String.format("%08d", number) + SpanishIds.dniControlLetter(number);
    }

    /** Creates a patient of Dr. Fernandez (initial password = DNI) and returns their session. */
    private Session newPatient(int number) throws Exception {
        Session admin = admin();
        JsonNode doctor = null;
        for (JsonNode d : getJson(admin, "/api/doctors")) {
            if ("lfernandez".equals(d.get("username").asText())) {
                doctor = d;
            }
        }
        assertThat(doctor).as("seeded doctor lfernandez").isNotNull();

        String username = "flujo.paciente" + number;
        ObjectNode body = json.createObjectNode();
        body.put("name", "Flujo").put("lastName", "Paciente").put("nationalId", dni(number))
                .put("healthCardNumber", healthCard(number)).put("socialSecurityNumber", "123456789012");
        body.putObject("address").put("street", "1 Test Street").put("city", "Vigo")
                .put("postalCode", "36201").put("province", "Pontevedra");
        body.put("phone", "986000000").put("email", username + "@example.com").put("birthDate", "1990-01-01")
                .put("healthCenterId", doctor.get("healthCenterId").asLong()).put("doctorId", doctor.get("id").asLong())
                .put("username", username);
        postJson(admin, "/api/patients", body).andExpect(status().isCreated());
        return login(username, dni(number));
    }

    /** 4 letters + 10 digits, as the health card format requires. */
    private static String healthCard(int number) {
        return "FLOW00" + number;
    }

    private long ibuprofenId(Session doctor) throws Exception {
        for (JsonNode medication : getJson(doctor, "/api/medications?brandName=Ibuprofen")) {
            if ("Ibuprofen Cinfa 600mg".equals(medication.get("brandName").asText())) {
                return medication.get("id").asLong();
            }
        }
        throw new AssertionError("the seeded Ibuprofen Cinfa 600mg is missing");
    }

    private ObjectNode prescriptionBody(long patientId, long doctorId, long medicationId, double dose, LocalDate endDate) {
        ObjectNode body = json.createObjectNode();
        body.put("patientId", patientId).put("doctorId", doctorId).put("medicationId", medicationId)
                .put("dailyDose", dose).put("instructions", "1 tablet every 12 hours").put("endDate", endDate.toString());
        return body;
    }

    private static List<String> texts(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            values.add(item.get(field).asText());
        }
        return values;
    }

    private JsonNode slots(Session patient, LocalDate day) throws Exception {
        return getJson(patient, "/api/patients/" + patient.id() + "/available-slots?day=" + day);
    }

    @Test
    void aPatientBooksAnAppointmentAndTheDoctorSeesItAndCompletesIt() throws Exception {
        Session patient = newPatient(42000001);
        Session other = newPatient(42000002);
        Session doctor = doctor();
        LocalDate day = LocalDate.now().plusDays(400);
        String nine = day + "T09:00:00";

        JsonNode free = slots(patient, day);
        assertThat(free.size()).isEqualTo(28);
        assertThat(free.get(0).get("start").asText()).isEqualTo(day + "T08:30:00");
        assertThat(free.get(27).get("start").asText()).isEqualTo(day + "T15:15:00");

        JsonNode booked = postJsonOk(patient, "/api/appointments",
                Map.of("patientId", patient.id(), "dateTime", nine), 201);
        assertThat(booked.get("status").asText()).isEqualTo("SCHEDULED");
        assertThat(booked.get("doctorId").asLong()).isEqualTo(doctor.id());
        assertThat(booked.get("patientId").asLong()).isEqualTo(patient.id());
        long appointmentId = booked.get("id").asLong();

        JsonNode after = slots(patient, day);
        assertThat(after.size()).isEqualTo(27);
        assertThat(texts(after, "start")).doesNotContain(nine);
        assertThat(texts(getJson(patient, "/api/patients/" + patient.id() + "/appointments"), "id"))
                .contains(String.valueOf(appointmentId));

        postJson(other, "/api/appointments", Map.of("patientId", other.id(), "dateTime", nine))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());

        JsonNode agenda = getJson(doctor, "/api/doctors/" + doctor.id() + "/schedule?day=" + day);
        assertThat(texts(agenda, "id")).contains(String.valueOf(appointmentId));

        JsonNode completed = postJsonOk(doctor, "/api/appointments/" + appointmentId + "/complete", Map.of(), 200);
        assertThat(completed.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(texts(getJson(patient, "/api/patients/" + patient.id() + "/appointments"), "id"))
                .doesNotContain(String.valueOf(appointmentId));
    }

    @Test
    void aPatientCanCancelAnAppointmentAndTheSlotBecomesFreeAgain() throws Exception {
        Session patient = newPatient(42000003);
        LocalDate day = LocalDate.now().plusDays(401);
        String slot = day + "T10:30:00";

        long id = postJsonOk(patient, "/api/appointments", Map.of("patientId", patient.id(), "dateTime", slot), 201)
                .get("id").asLong();
        assertThat(texts(slots(patient, day), "start")).doesNotContain(slot);

        JsonNode cancelled = postJsonOk(patient, "/api/appointments/" + id + "/cancel", Map.of(), 200);

        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(texts(slots(patient, day), "start")).contains(slot);
        assertThat(texts(getJson(patient, "/api/patients/" + patient.id() + "/appointments"), "id"))
                .doesNotContain(String.valueOf(id));
    }

    @Test
    void theDoctorCanMarkANoShow() throws Exception {
        Session patient = newPatient(42000004);
        Session doctor = doctor();
        LocalDate day = LocalDate.now().plusDays(402);

        long id = postJsonOk(patient, "/api/appointments",
                Map.of("patientId", patient.id(), "dateTime", day + "T11:00:00"), 201).get("id").asLong();

        JsonNode noShow = postJsonOk(doctor, "/api/appointments/" + id + "/no-show", Map.of(), 200);

        assertThat(noShow.get("status").asText()).isEqualTo("NO_SHOW");
    }

    @Test
    void timesOutsideOfficeHoursOrOffTheQuarterHourAreRejected() throws Exception {
        Session patient = newPatient(42000005);
        LocalDate day = LocalDate.now().plusDays(403);

        for (String time : List.of("07:00:00", "08:15:00", "09:10:00", "15:30:00", "16:00:00")) {
            postJson(patient, "/api/appointments", Map.of("patientId", patient.id(), "dateTime", day + "T" + time))
                    .andExpect(status().isBadRequest());
        }
        assertThat(slots(patient, day).size()).isEqualTo(28);
    }

    @Test
    void thePrescriptionGeneratesItsRefillPlanAndThePharmacyDispensesIt() throws Exception {
        Session patient = newPatient(42000011);
        Session doctor = doctor();
        Session pharmacy = login("compostela", "PLIC-778899");
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(60);

        // 30 units per package at 2 per day -> a box lasts 15 days -> 4 refills in 60 days
        JsonNode prescription = postJsonOk(doctor, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), ibuprofenId(doctor), 2.0, endDate), 201);
        assertThat(prescription.get("active").asBoolean()).isTrue();
        assertThat(prescription.get("startDate").asText()).isEqualTo(today.toString());
        assertThat(prescription.get("endDate").asText()).isEqualTo(endDate.toString());
        JsonNode plan = prescription.get("refills");
        assertThat(plan.size()).isEqualTo(4);
        assertThat(texts(plan, "validFrom")).containsExactly(today.toString(), today.plusDays(8).toString(),
                today.plusDays(23).toString(), today.plusDays(38).toString());
        assertThat(texts(plan, "validUntil")).containsExactly(today.plusDays(7).toString(), today.plusDays(22).toString(),
                today.plusDays(37).toString(), today.plusDays(52).toString());
        assertThat(texts(plan, "status")).containsOnly("SCHEDULED");

        JsonNode mine = getJson(patient, "/api/patients/" + patient.id() + "/prescriptions");
        assertThat(mine.size()).isEqualTo(1);
        assertThat(mine.get(0).get("refills").size()).isEqualTo(4);
        assertThat(getJson(patient, "/api/patients/" + patient.id() + "/refills").size()).isEqualTo(4);

        JsonNode found = getJson(pharmacy, "/api/refills?healthCardNumber=" + healthCard(42000011).toLowerCase());
        assertThat(found.size()).isEqualTo(4);
        long first = found.get(0).get("id").asLong();
        long second = found.get(1).get("id").asLong();

        postJson(pharmacy, "/api/refills/" + second + "/fill?pharmacyId=" + pharmacy.id(), Map.of())
                .andExpect(status().isBadRequest());

        JsonNode filled = postJsonOk(pharmacy, "/api/refills/" + first + "/fill?pharmacyId=" + pharmacy.id(), Map.of(), 200);
        assertThat(filled.get("status").asText()).isEqualTo("FILLED");
        assertThat(filled.get("pharmacyId").asLong()).isEqualTo(pharmacy.id());

        postJson(pharmacy, "/api/refills/" + first + "/fill?pharmacyId=" + pharmacy.id(), Map.of())
                .andExpect(status().isBadRequest());

        assertThat(getJson(patient, "/api/patients/" + patient.id() + "/refills").size()).isEqualTo(3);
        assertThat(getJson(pharmacy, "/api/refills?healthCardNumber=" + healthCard(42000011)).size()).isEqualTo(3);

        postJson(doctor, "/api/prescriptions/" + prescription.get("id").asLong() + "/cancel", Map.of())
                .andExpect(status().isNoContent());
        assertThat(getJson(patient, "/api/patients/" + patient.id() + "/prescriptions").size()).isEqualTo(0);
        assertThat(getJson(patient, "/api/patients/" + patient.id() + "/refills").size()).isEqualTo(0);
        assertThat(getJson(pharmacy, "/api/refills?healthCardNumber=" + healthCard(42000011)).size()).isEqualTo(0);
    }

    @Test
    void aShortTreatmentGetsASingleRefillCutAtTheEndOfTheTreatment() throws Exception {
        Session patient = newPatient(42000012);
        Session doctor = doctor();
        LocalDate endDate = LocalDate.now().plusDays(5);

        JsonNode plan = postJsonOk(doctor, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), ibuprofenId(doctor), 2.0, endDate), 201).get("refills");

        assertThat(plan.size()).isEqualTo(1);
        assertThat(plan.get(0).get("validFrom").asText()).isEqualTo(LocalDate.now().toString());
        assertThat(plan.get(0).get("validUntil").asText()).isEqualTo(endDate.toString());
    }

    @Test
    void aPrescriptionEndingTodayOrBeforeIsRejectedAndNothingIsCreated() throws Exception {
        Session patient = newPatient(42000013);
        Session doctor = doctor();
        long medication = ibuprofenId(doctor);

        postJson(doctor, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), medication, 2.0, LocalDate.now()))
                .andExpect(status().isBadRequest());
        postJson(doctor, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), medication, 2.0, LocalDate.now().minusDays(5)))
                .andExpect(status().isBadRequest());
        postJson(doctor, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), medication, 0, LocalDate.now().plusDays(10)))
                .andExpect(status().isBadRequest());

        assertThat(getJson(patient, "/api/patients/" + patient.id() + "/prescriptions").size()).isEqualTo(0);
    }

    @Test
    void aDoctorCannotPrescribeInTheNameOfAnotherDoctorNorCancelHisPrescriptions() throws Exception {
        Session patient = newPatient(42000014);
        Session doctor = doctor();
        Session colleague = login("ciglesias", "LIC-4502");
        long medication = ibuprofenId(doctor);

        postJson(colleague, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), medication, 2.0, LocalDate.now().plusDays(30)))
                .andExpect(status().isForbidden());

        long id = postJsonOk(doctor, "/api/prescriptions",
                prescriptionBody(patient.id(), doctor.id(), medication, 2.0, LocalDate.now().plusDays(30)), 201)
                .get("id").asLong();
        postJson(colleague, "/api/prescriptions/" + id + "/cancel", Map.of()).andExpect(status().isForbidden());

        assertThat(getJson(patient, "/api/patients/" + patient.id() + "/prescriptions").size()).isEqualTo(1);
    }
}
