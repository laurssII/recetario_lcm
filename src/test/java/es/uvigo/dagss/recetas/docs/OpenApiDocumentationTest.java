package es.uvigo.dagss.recetas.docs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Checks the OpenAPI document that springdoc generates from the real controllers (served at /v3/api-docs and
 * rendered by Swagger UI): that it is public, that every endpoint is documented, and that its security
 * and error information is consistent with what the API does.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    private JsonNode document() throws Exception {
        String body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private record Op(String method, String path, JsonNode node) {
        String id() {
            return method.toUpperCase() + " " + path;
        }
    }

    private List<Op> operations(JsonNode doc) {
        List<Op> result = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> paths = doc.get("paths").fields();
        while (paths.hasNext()) {
            Map.Entry<String, JsonNode> path = paths.next();
            for (String method : METHODS) {
                if (path.getValue().has(method)) {
                    result.add(new Op(method, path.getKey(), path.getValue().get(method)));
                }
            }
        }
        return result;
    }

    private Op operation(JsonNode doc, String method, String path) {
        return operations(doc).stream().filter(o -> o.method().equals(method) && o.path().equals(path))
                .findFirst().orElseThrow(() -> new AssertionError("not documented: " + method + " " + path));
    }

    private static List<String> names(JsonNode object) {
        List<String> names = new ArrayList<>();
        object.fieldNames().forEachRemaining(names::add);
        return names;
    }

    // ------------------------------------------------------------------ availability

    @Test
    void theDocumentIsPublicAndDescribesTheApi() throws Exception {
        JsonNode doc = document();

        assertThat(doc.get("openapi").asText()).startsWith("3.");
        assertThat(doc.get("info").get("title").asText()).isEqualTo("Prescriptions API");
        assertThat(doc.get("info").get("description").asText()).contains("Authorize");
    }

    @Test
    void swaggerUiIsServed() throws Exception {
        int page = mvc.perform(get("/swagger-ui.html")).andReturn().getResponse().getStatus();
        int resources = mvc.perform(get("/swagger-ui/index.html")).andReturn().getResponse().getStatus();
        assertThat(page).isBetween(200, 399);
        assertThat(resources).isBetween(200, 399);
    }

    // ------------------------------------------------------------------ coverage

    @Test
    void onlyTheApiIsDocumented() throws Exception {
        for (String path : names(document().get("paths"))) {
            assertThat(path).startsWith("/api/");
        }
    }

    @Test
    void everyKeyEndpointIsDocumented() throws Exception {
        JsonNode doc = document();
        List<String> documented = operations(doc).stream().map(Op::id).toList();

        assertThat(documented).contains(
                "POST /api/auth/login",
                "GET /api/health-centers", "POST /api/health-centers",
                "GET /api/medications", "POST /api/medications",
                "POST /api/administrators",
                "GET /api/doctors", "GET /api/doctors/{id}/schedule", "PATCH /api/doctors/{id}/password",
                "GET /api/patients", "POST /api/patients", "GET /api/patients/{id}/available-slots",
                "GET /api/patients/{id}/prescriptions", "GET /api/patients/{id}/refills", "PATCH /api/patients/{id}/profile",
                "GET /api/pharmacies", "PATCH /api/pharmacies/{id}/profile",
                "GET /api/appointments", "POST /api/appointments", "POST /api/appointments/{id}/cancel",
                "POST /api/appointments/{id}/no-show", "POST /api/appointments/{id}/complete",
                "POST /api/prescriptions", "POST /api/prescriptions/{id}/cancel",
                "GET /api/refills", "POST /api/refills/{id}/fill");
    }

    @Test
    void everyOperationHasASummaryAndExactlyOneKnownTag() throws Exception {
        JsonNode doc = document();
        List<String> declaredTags = new ArrayList<>();
        doc.get("tags").forEach(t -> declaredTags.add(t.get("name").asText()));

        for (Op op : operations(doc)) {
            assertThat(op.node().path("summary").asText()).as(op.id() + " summary").isNotBlank();
            assertThat(op.node().path("tags")).as(op.id() + " tags").hasSize(1);
            assertThat(declaredTags).as(op.id() + " tag").contains(op.node().get("tags").get(0).asText());
        }
    }

    @Test
    void everyDeclaredTagHasAtLeastOneOperationAndADescription() throws Exception {
        JsonNode doc = document();
        Set<String> used = new java.util.HashSet<>();
        operations(doc).forEach(op -> used.add(op.node().get("tags").get(0).asText()));

        for (JsonNode tag : doc.get("tags")) {
            assertThat(used).as("operations tagged " + tag.get("name").asText()).contains(tag.get("name").asText());
            assertThat(tag.path("description").asText()).isNotBlank();
        }
    }

    // ------------------------------------------------------------------ security

    @Test
    void bearerAuthenticationIsDeclaredAndRequiredByDefault() throws Exception {
        JsonNode doc = document();

        JsonNode scheme = doc.get("components").get("securitySchemes").get("bearerAuth");
        assertThat(scheme.get("type").asText()).isEqualTo("http");
        assertThat(scheme.get("scheme").asText()).isEqualTo("bearer");
        assertThat(doc.get("security").get(0).has("bearerAuth")).isTrue();
    }

    @Test
    void loginIsTheOnlyOperationThatDoesNotNeedAToken() throws Exception {
        for (Op op : operations(document())) {
            boolean publicOperation = op.node().has("security") && op.node().get("security").isEmpty();
            assertThat(publicOperation).as(op.id() + " is public").isEqualTo(op.id().equals("POST /api/auth/login"));
        }
    }

    // ------------------------------------------------------------------ responses

    @Test
    void securedOperationsDocumentAuthenticationAndAuthorizationErrors() throws Exception {
        for (Op op : operations(document())) {
            if (op.id().equals("POST /api/auth/login")) {
                continue;
            }
            JsonNode responses = op.node().get("responses");
            assertThat(responses.has("401")).as(op.id() + " documents 401").isTrue();
            assertThat(responses.has("403")).as(op.id() + " documents 403").isTrue();
        }
    }

    @Test
    void errorResponsesUseTheSharedErrorSchema() throws Exception {
        JsonNode doc = document();

        JsonNode schema = doc.get("components").get("schemas").get("ErrorResponse");
        assertThat(names(schema.get("properties"))).contains("timestamp", "status", "error", "message", "fieldErrors");
        JsonNode conflict = operation(doc, "post", "/api/patients").node().get("responses").get("409");
        assertThat(conflict.get("content").get("application/json").get("schema").get("$ref").asText())
                .endsWith("/ErrorResponse");
    }

    @Test
    void userCreationDocumentsDuplicatesButReadsDoNot() throws Exception {
        JsonNode doc = document();

        for (String path : List.of("/api/patients", "/api/doctors", "/api/pharmacies", "/api/administrators")) {
            assertThat(operation(doc, "post", path).node().get("responses").has("409")).as("POST " + path).isTrue();
            assertThat(operation(doc, "get", path).node().get("responses").has("409")).as("GET " + path).isFalse();
        }
    }

    @Test
    void operationsOnASingleRecordDocumentNotFound() throws Exception {
        JsonNode doc = document();

        assertThat(operation(doc, "get", "/api/patients/{id}").node().get("responses").has("404")).isTrue();
        assertThat(operation(doc, "post", "/api/refills/{id}/fill").node().get("responses").has("404")).isTrue();
        assertThat(operation(doc, "post", "/api/prescriptions").node().get("responses").has("404")).isTrue();
    }

    @Test
    void successfulResponsesUseTheCodesTheApiReturns() throws Exception {
        JsonNode doc = document();

        JsonNode createPrescription = operation(doc, "post", "/api/prescriptions").node().get("responses");
        assertThat(createPrescription.has("201")).isTrue();
        assertThat(createPrescription.has("200")).isFalse();
        assertThat(operation(doc, "post", "/api/appointments").node().get("responses").has("201")).isTrue();
        assertThat(operation(doc, "post", "/api/prescriptions/{id}/cancel").node().get("responses").has("204")).isTrue();
        assertThat(operation(doc, "delete", "/api/patients/{id}").node().get("responses").has("204")).isTrue();
        assertThat(operation(doc, "post", "/api/appointments/{id}/cancel").node().get("responses").has("200")).isTrue();
    }

    // ------------------------------------------------------------------ models

    @Test
    void requestSchemasShowTheValidationRules() throws Exception {
        JsonNode prescription = document().get("components").get("schemas").get("NewPrescriptionRequest");

        List<String> required = new ArrayList<>();
        prescription.get("required").forEach(n -> required.add(n.asText()));
        assertThat(required).contains("patientId", "doctorId", "medicationId", "instructions", "endDate");
        assertThat(prescription.get("properties").get("instructions").get("maxLength").asInt()).isEqualTo(255);
    }

    @Test
    void queryParametersAreDescribed() throws Exception {
        JsonNode doc = document();

        for (Op op : operations(doc)) {
            for (JsonNode parameter : op.node().path("parameters")) {
                if ("query".equals(parameter.get("in").asText())) {
                    assertThat(parameter.path("description").asText())
                            .as(op.id() + " parameter " + parameter.get("name").asText()).isNotBlank();
                }
            }
        }
    }
}
