package es.uvigo.dagss.recetas.config;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    static final String BEARER = "bearerAuth";
    static final String ERROR_SCHEMA = "ErrorResponse";

    private static final Set<String> CREATION_PATHS = Set.of("/api/health-centers", "/api/medications", "/api/administrators",
            "/api/doctors", "/api/patients", "/api/pharmacies", "/api/appointments", "/api/prescriptions");
    private static final Pattern USER_COLLECTION = Pattern.compile("^/api/(patients|doctors|pharmacies|administrators)$");
    private static final Pattern USER_ITEM = Pattern.compile("^/api/(patients|doctors|pharmacies|administrators)/\\{id}$");

    @Bean
    public OpenAPI prescriptionsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Prescriptions API")
                        .version("v1")
                        .description("""
                                API REST de receta electrónica (proyecto de la asignatura DAGSS 24/25, Universidade de Vigo): \
                                citas médicas, prescripciones y planes de recetas.

                                Para probarla desde esta página: ejecuta `POST /api/auth/login`, copia el `token` de la \
                                respuesta y pulsa **Authorize**. Usuarios de prueba (perfil h2): `admin`/`admin`, \
                                `lfernandez`/`LIC-4501` (médico), `xrodriguez`/`20000001Y` (paciente) y \
                                `compostela`/`PLIC-778899` (farmacia).

                                Los errores se devuelven siempre como JSON con el formato `ErrorResponse`. \
                                Las fechas se envían en formato ISO (`AAAA-MM-DD`, y `AAAA-MM-DDTHH:mm:ss` para fecha y hora)."""))
                .components(new Components()
                        .addSecuritySchemes(BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Token devuelto por POST /api/auth/login"))
                        .addSchemas(ERROR_SCHEMA, errorSchema()))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .tags(List.of(
                        tag("Autenticación", "Inicio de sesión"),
                        tag("Centros de salud", "Alta, consulta y baja de centros de salud"),
                        tag("Medicamentos", "Catálogo de medicamentos y búsqueda"),
                        tag("Administradores", "Gestión de administradores"),
                        tag("Médicos", "Gestión de médicos, agenda y datos propios"),
                        tag("Pacientes", "Gestión de pacientes, sus citas, prescripciones y recetas"),
                        tag("Farmacias", "Gestión de farmacias y datos propios"),
                        tag("Citas", "Reserva, cancelación y atención de citas"),
                        tag("Prescripciones", "Creación de prescripciones y generación del plan de recetas"),
                        tag("Recetas", "Búsqueda y dispensación de recetas en farmacia")));
    }

    /**
     * Completes the generated document: springdoc cannot see the status code a method puts in its ResponseEntity
     * (it always documents 200), and the error responses are the same for every endpoint, so they are added here
     * instead of being repeated on each controller method. The shared error schema is registered here too because
     * springdoc drops schemas declared on the base document that no generated endpoint references.
     */
    @Bean
    public OpenApiCustomizer standardErrorResponses() {
        return openApi -> {
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }
            if (openApi.getComponents().getSchemas() == null
                    || !openApi.getComponents().getSchemas().containsKey(ERROR_SCHEMA)) {
                openApi.getComponents().addSchemas(ERROR_SCHEMA, errorSchema());
            }
            openApi.getPaths().forEach((path, item) ->
                    item.readOperationsMap().forEach((method, operation) -> addErrorResponses(path, method, operation)));
        };
    }

    private static void addErrorResponses(String path, HttpMethod method, Operation operation) {
        replaceSuccessCode(path, method, operation);
        boolean secured = operation.getSecurity() == null || !operation.getSecurity().isEmpty();
        if (secured) {
            add(operation, "401", "Falta el token, no es válido o ha caducado");
            add(operation, "403", "El rol o el usuario no tiene permiso sobre este recurso");
        } else {
            add(operation, "401", "Usuario o contraseña incorrectos");
        }
        if (method != HttpMethod.GET || hasQueryParameters(operation)) {
            add(operation, "400", "Datos no válidos u operación no permitida en el estado actual; fieldErrors indica los campos");
        }
        boolean referencesOtherRecords = method == HttpMethod.POST
                && (path.equals("/api/prescriptions") || path.equals("/api/appointments"));
        if (path.contains("{id}") || referencesOtherRecords) {
            add(operation, "404", "El recurso indicado no existe");
        }
        boolean createsOrReplacesUser = (method == HttpMethod.POST && USER_COLLECTION.matcher(path).matches())
                || (method == HttpMethod.PUT && USER_ITEM.matcher(path).matches());
        if (createsOrReplacesUser) {
            add(operation, "409", "Ya existe otro registro con el mismo DNI/NIF, usuario, tarjeta sanitaria o nº de colegiado; fieldErrors indica el campo");
        }
        sortByCode(operation);
    }

    private static void replaceSuccessCode(String path, HttpMethod method, Operation operation) {
        String code = successCode(path, method);
        if (code == null || operation.getResponses() == null) {
            return;
        }
        ApiResponse generated = operation.getResponses().remove("200");
        if (operation.getResponses().containsKey(code)) {
            return;
        }
        ApiResponse response = new ApiResponse().description("201".equals(code) ? "Creado" : "Sin contenido");
        if ("201".equals(code) && generated != null) {
            response.setContent(generated.getContent());
        }
        operation.getResponses().addApiResponse(code, response);
    }

    private static String successCode(String path, HttpMethod method) {
        if (method == HttpMethod.POST && CREATION_PATHS.contains(path)) {
            return "201";
        }
        boolean noContent = method == HttpMethod.DELETE
                || (method == HttpMethod.POST && path.equals("/api/prescriptions/{id}/cancel"))
                || (method == HttpMethod.PATCH && path.endsWith("/password"));
        return noContent ? "204" : null;
    }

    private static boolean hasQueryParameters(Operation operation) {
        if (operation.getParameters() == null) {
            return false;
        }
        for (Parameter parameter : operation.getParameters()) {
            if ("query".equals(parameter.getIn())) {
                return true;
            }
        }
        return false;
    }

    private static void add(Operation operation, String code, String description) {
        if (operation.getResponses() == null) {
            operation.setResponses(new ApiResponses());
        }
        if (operation.getResponses().containsKey(code)) {
            return;
        }
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA);
        operation.getResponses().addApiResponse(code, new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json", new MediaType().schema(ref))));
    }

    private static void sortByCode(Operation operation) {
        ApiResponses sorted = new ApiResponses();
        operation.getResponses().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> sorted.addApiResponse(e.getKey(), e.getValue()));
        operation.setResponses(sorted);
    }

    private static Schema<?> errorSchema() {
        Schema<Object> error = new ObjectSchema();
        error.setDescription("Formato de todos los errores de la API");
        error.addProperty("timestamp", new DateTimeSchema());
        error.addProperty("status", new IntegerSchema());
        error.addProperty("error", new StringSchema());
        error.addProperty("message", new StringSchema());
        MapSchema fieldErrors = new MapSchema();
        fieldErrors.setAdditionalProperties(new StringSchema());
        fieldErrors.setDescription("Solo en errores de validación (400) y de duplicados (409): campo -> mensaje");
        error.addProperty("fieldErrors", fieldErrors);
        return error;
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
