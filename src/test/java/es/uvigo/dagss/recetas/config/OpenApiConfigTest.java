package es.uvigo.dagss.recetas.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.tags.Tag;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    private Operation operation() {
        return new Operation().responses(new ApiResponses().addApiResponse("200", new ApiResponse().description("OK")));
    }

    private ApiResponses customize(String path, HttpMethod method, Operation operation) {
        PathItem item = new PathItem();
        item.operation(method, operation);
        OpenAPI api = new OpenAPI().paths(new Paths().addPathItem(path, item));
        config.standardErrorResponses().customise(api);
        return operation.getResponses();
    }

    private List<String> codes(ApiResponses responses) {
        return new ArrayList<>(responses.keySet());
    }

    @Test
    void theCustomizerRegistersTheErrorSchemaWhenTheDocumentLacksIt() {
        OpenAPI api = new OpenAPI().paths(new Paths());

        config.standardErrorResponses().customise(api);

        assertThat(api.getComponents().getSchemas()).containsKey("ErrorResponse");
        assertThat(api.getComponents().getSchemas().get("ErrorResponse").getProperties())
                .containsKeys("timestamp", "status", "error", "message", "fieldErrors");
    }

    @Test
    void theCustomizerKeepsAnExistingErrorSchema() {
        OpenAPI api = config.prescriptionsOpenApi().paths(new Paths());
        Object original = api.getComponents().getSchemas().get("ErrorResponse");

        config.standardErrorResponses().customise(api);

        assertThat(api.getComponents().getSchemas().get("ErrorResponse")).isSameAs(original);
    }

    @Test
    void aSecuredReadOnlyEndpointDocumentsOnlyAuthenticationErrors() {
        ApiResponses responses = customize("/api/health-centers", HttpMethod.GET, operation());

        assertThat(codes(responses)).containsExactly("200", "401", "403");
    }

    @Test
    void aReadWithQueryParametersCanAlsoFailWithABadRequest() {
        Operation withParameter = operation().addParametersItem(new QueryParameter().name("day"));

        assertThat(codes(customize("/api/appointments", HttpMethod.GET, withParameter)))
                .containsExactly("200", "400", "401", "403");
    }

    @Test
    void anEndpointWithAnIdInThePathCanAlsoReturnNotFound() {
        assertThat(codes(customize("/api/patients/{id}", HttpMethod.GET, operation())))
                .containsExactly("200", "401", "403", "404");
    }

    @Test
    void creatingAUserDocumentsValidationAndDuplicateErrorsInOrder() {
        ApiResponses responses = customize("/api/patients", HttpMethod.POST, operation());

        assertThat(codes(responses)).containsExactly("201", "400", "401", "403", "409");
    }

    @Test
    void creationsAreDocumentedAs201KeepingTheGeneratedBody() {
        Operation create = operation();
        create.getResponses().get("200").setContent(new Content().addMediaType("application/json", new MediaType()));

        ApiResponses responses = customize("/api/prescriptions", HttpMethod.POST, create);

        assertThat(responses).doesNotContainKey("200");
        assertThat(responses.get("201").getContent()).containsKey("application/json");
    }

    @Test
    void deletionsCancellationsAndPasswordChangesAreDocumentedAs204() {
        assertThat(codes(customize("/api/patients/{id}", HttpMethod.DELETE, operation()))).contains("204").doesNotContain("200");
        assertThat(codes(customize("/api/prescriptions/{id}/cancel", HttpMethod.POST, operation()))).contains("204").doesNotContain("200");
        assertThat(codes(customize("/api/doctors/{id}/password", HttpMethod.PATCH, operation()))).contains("204").doesNotContain("200");
    }

    @Test
    void otherEndpointsKeepTheir200() {
        assertThat(codes(customize("/api/appointments/{id}/cancel", HttpMethod.POST, operation()))).contains("200");
        assertThat(codes(customize("/api/patients/{id}/profile", HttpMethod.PATCH, operation()))).contains("200");
        assertThat(codes(customize("/api/auth/login", HttpMethod.POST, operation().security(List.of())))).contains("200");
    }

    @Test
    void replacingAUserAlsoDocumentsDuplicates() {
        assertThat(codes(customize("/api/doctors/{id}", HttpMethod.PUT, operation())))
                .containsExactly("200", "400", "401", "403", "404", "409");
    }

    @Test
    void ownProfileUpdatesDoNotDocumentDuplicates() {
        assertThat(codes(customize("/api/patients/{id}/profile", HttpMethod.PATCH, operation())))
                .doesNotContain("409");
    }

    @Test
    void creatingAnAppointmentOrAPrescriptionCanFailBecauseARelatedRecordDoesNotExist() {
        assertThat(codes(customize("/api/appointments", HttpMethod.POST, operation()))).contains("404");
        assertThat(codes(customize("/api/prescriptions", HttpMethod.POST, operation()))).contains("404");
        assertThat(codes(customize("/api/medications", HttpMethod.POST, operation()))).doesNotContain("404");
    }

    @Test
    void aPublicOperationOnlyDocumentsWrongCredentials() {
        Operation login = operation().security(List.of());

        ApiResponses responses = customize("/api/auth/login", HttpMethod.POST, login);

        assertThat(codes(responses)).containsExactly("200", "400", "401");
        assertThat(responses.get("401").getDescription()).contains("incorrectos");
    }

    @Test
    void errorResponsesPointToTheSharedErrorSchema() {
        ApiResponses responses = customize("/api/patients", HttpMethod.POST, operation());

        String ref = responses.get("409").getContent().get("application/json").getSchema().get$ref();
        assertThat(ref).isEqualTo("#/components/schemas/ErrorResponse");
    }

    @Test
    void responsesThatAreAlreadyDocumentedAreKept() {
        Operation documented = operation();
        documented.getResponses().addApiResponse("400", new ApiResponse().description("Custom description"));

        ApiResponses responses = customize("/api/patients", HttpMethod.POST, documented);

        assertThat(responses.get("400").getDescription()).isEqualTo("Custom description");
    }

    @Test
    void theApiDeclaresBearerAuthenticationByDefaultTheErrorSchemaAndAllTheTags() {
        OpenAPI api = config.prescriptionsOpenApi();

        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
        assertThat(api.getComponents().getSecuritySchemes().get("bearerAuth").getScheme()).isEqualTo("bearer");
        assertThat(api.getComponents().getSchemas()).containsKey("ErrorResponse");
        assertThat(api.getComponents().getSchemas().get("ErrorResponse").getProperties())
                .containsKeys("timestamp", "status", "error", "message", "fieldErrors");
        assertThat(api.getSecurity()).hasSize(1);
        assertThat(api.getSecurity().get(0)).containsKey("bearerAuth");
        List<String> tags = api.getTags().stream().map(Tag::getName).toList();
        assertThat(tags).containsExactly("Autenticación", "Centros de salud", "Medicamentos", "Administradores", "Médicos",
                "Pacientes", "Farmacias", "Citas", "Prescripciones", "Recetas");
    }
}
