# Recetas — Sistema de receta electrónica
[![CI](https://github.com/laurssII/recetario_lcm/actions/workflows/ci.yml/badge.svg)](https://github.com/laurssII/recetario_lcm/actions/workflows/ci.yml)
API REST en **Java 17 + Spring Boot 3** para la gestión de citas médicas, prescripciones y planes de recetas electrónicas. Parte del enunciado de la asignatura *DAGSS* (Universidade de Vigo), cuyas historias de usuario están en [`doc/`](doc/).

El proyecto es el **backend**. Para poder probarlo sin más herramientas incluye [`cliente-demo.html`](cliente-demo.html), un cliente de demostración de un solo fichero: no es el frontend del proyecto (ver [Cliente de demostración](#cliente-de-demostración)).

---

## Qué hace

- **Médicos**: agenda diaria, atención de citas, creación de prescripciones con generación **automática** de un plan de recetas.
- **Pacientes**: consulta y anulación de citas, reserva de cita en huecos libres de su médico, consulta de prescripciones en vigor y recetas pendientes de recoger.
- **Farmacias**: localización de recetas en vigor de un paciente por su nº de tarjeta sanitaria, y registro de entrega ("servir") de una receta dentro de su ventana de validez.
- **Administración**: alta/baja/modificación de centros de salud, médicos, pacientes, farmacias, medicamentos y administradores.

## Arquitectura

```
web/          controladores REST (@RestController) — solo orquestan, sin lógica de negocio
services/     lógica de negocio (@Service, @Transactional)
dto/          DTOs (records) de entrada/salida — las entidades JPA nunca se serializan directamente
daos/         repositorios Spring Data JPA
entities/     entidades JPA
exceptions/   @RestControllerAdvice + excepciones de negocio -> respuestas de error en JSON
security/     JWT, permisos por registro (AccessPolicy) y respuestas 401/403
config/       Spring Security (SecurityConfig), OpenAPI (OpenApiConfig) y carga de datos de prueba (DataInitializer)
```

Pila técnica: Java 17 · Spring Boot 3.3 · Spring Security (JWT + BCrypt) · Spring Data JPA · Bean Validation · MySQL / H2 · springdoc-openapi (Swagger UI) · JUnit 5 + Mockito · Docker.

## Algunas imágenes de demo

### Pantalla de inicio de sesión
![Pantalla de inicio de sesión](<doc/images/Pantalla de inicio de sesión.png>)

### Vista principal centros (administrador)
![Vista principal centros (administrador)](<doc/images/Vista principal centros (administrador).png>)

### Vista principal pacientes (administrador
![Vista principal pacientes (administrador)](<doc/images/Vista principal pacientes (administrador).png>)

### Vista agenda (paciente)
![Vista agenda (paciente)](<doc/images/Vista agenda (paciente).png>)

### Vista agenda (médico)
![Vista agenda (médico)](<doc/images/Vista agenda (médico).png>)

### Vista recetas de un paciente (farmacia)
![Vista recetas de un paciente (farmacia)](<doc/images/Vista recetas de un paciente (farmacia).png>)

## Puesta en marcha

### Opción A — un solo comando, sin instalar nada (perfil H2)
Requiere solo tener **Java 17** instalado. La base de datos es en memoria: se rellena con datos de prueba en cada arranque y **los cambios que hagas (altas, bajas...) se pierden al parar la aplicación**. Ideal para probar rápido.

```bash
./mvnw spring-boot:run
```

La app queda escuchando en `http://localhost:8080`.

### Opción B — Docker Compose (MySQL persistente)
Requiere **Docker**. Levanta MySQL y la aplicación con un único comando:

```bash
docker compose up --build
```

Aquí **sí hay persistencia**: los datos se guardan en un volumen de Docker, de modo que altas, bajas, citas y prescripciones sobreviven a reinicios (los datos de prueba solo se cargan la primera vez). Para parar conservando los datos usa `docker compose down`; para borrarlo todo y empezar de cero, `docker compose down -v`.

### Opción C — MySQL instalado manualmente
```sql
CREATE DATABASE recetas;
CREATE USER dagss@localhost IDENTIFIED BY 'dagss';
GRANT ALL PRIVILEGES ON recetas.* TO dagss@localhost;
```
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```
Para ver las tablas: `mysql -u dagss -p recetas` y, dentro, `show tables;` o `describe nombre_tabla;`.

### Explorar la API
- **Swagger UI**: http://localhost:8080/swagger-ui.html. Ver [Documentación de la API](#documentación-de-la-api).
- **Cliente de demostración**: ver más abajo.
- **Consola H2**: desactivada por seguridad (saltaría los permisos de la API). Cómo activarla en local: [`doc/seguridad.md`](doc/seguridad.md) §9.
- Al arrancar, la consola imprime los ids de los datos de prueba creados (paciente, médico, etc.).

Usuarios de prueba (se crean al arrancar):

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `admin` |
| Médico | `lfernandez` | `LIC-4501` |
| Paciente | `xrodriguez` | `20000001Y` (su DNI) |
| Farmacia | `compostela` | `PLIC-778899` |

## Documentación de la API

La documentación se genera a partir del código con `springdoc-openapi`, así que no hay una tabla de endpoints que mantener a mano:

- **Swagger UI** (http://localhost:8080/swagger-ui.html): permite leer y probar cada endpoint desde el navegador. Casi todos exigen sesión: ejecuta `POST /api/auth/login`, copia el `token` de la respuesta, pulsa **Authorize** y pégalo. El token se conserva al recargar la página.
- **Documento OpenAPI** (http://localhost:8080/v3/api-docs, JSON): sirve para importar la API en Postman o Insomnia, o para generar un cliente.

Cada operación indica qué hace, qué rol puede llamarla y sus parámetros. Las respuestas de error (401, 403, 400, 404, 409) están documentadas con el formato común `ErrorResponse`, que incluye `fieldErrors` en los errores de validación y de duplicados. La matriz completa de permisos por rol y por registro está en [`doc/seguridad.md`](doc/seguridad.md).

## Cliente de demostración

[`cliente-demo.html`](cliente-demo.html) es un único fichero con una interfaz en React (cargada desde un CDN, sin proceso de build ni tests) que permite recorrer los flujos de cada rol contra la API: ábrelo directamente en el navegador con la aplicación en marcha. Tiene unas 2.200 líneas porque está pensado como demostración y no como frontend: no está organizado en componentes y módulos, no tiene tests propios y no es lo que se evalúa en este proyecto. Un frontend real sería un proyecto aparte.

Muestra las fechas como dd/mm/aaaa (la API sigue usando ISO) y tiene selector de idioma español/inglés.

## Tests

```bash
./mvnw test
```

Incluye:

- **Servicios** (`services/`, JUnit + Mockito): plan de recetas (HU-M5: número de recetas, fechas y márgenes de ±7 días, recorte al fin del tratamiento, duraciones que no son múltiplo de una caja, dosis o envase inválidos), anulación de prescripciones, reserva de citas (huecos de 15 min entre 8:30 y 15:30, hueco ocupado, horas fuera de horario, cambios de estado), dispensación de recetas (ventana de validez, estados, farmacia inexistente) y unicidad de DNI/usuario/tarjeta.
- **Flujos de extremo a extremo** (`flows/`, `MockMvc` + H2): el paciente reserva y el médico atiende; el médico prescribe, se genera el plan, la farmacia dispensa y el médico anula.
- **Documentación de la API** (`docs/`, `config/`): el documento OpenAPI generado es público, documenta todos los endpoints con resumen y etiqueta, declara la autenticación y los errores de cada operación, y el login es la única operación sin token.
- **Seguridad** (`security/`, `MockMvc`): login, permisos por rol y por registro, contraseñas con hash y unicidad.
- **Validación** (`validation/`): reglas de formato de DNI/NIE, teléfono, código postal, provincia, tarjeta sanitaria, contraseña y DTO.
- **Repositorios** (`daos/`, `@DataJpaTest`).

## Decisiones de diseño destacadas

- **Plan de recetas (HU-M5)**: cada receta cubre una caja del medicamento; los "días por caja" se calculan como `dosis del envase / dosis diaria`, y la duración total de la prescripción se reparte en tantas recetas como haga falta. Cada receta lleva un margen de ±7 días sobre su fecha "exacta" de recogida (salvo la primera, sin margen anterior). La propia historia de usuario contempla otras estrategias válidas; esta es la implementada.
- **Contraseñas iniciales**: el paciente se da de alta con su **DNI** como contraseña inicial (HU-A5); el médico, con su **nº de colegiado** (HU-A4); la farmacia, con el **nº de colegiado del farmacéutico** (HU-A6); y al administrador se la asigna manualmente quien lo crea (HU-A2). Los formularios de alta lo indican. Cada usuario puede cambiarla después desde «Mi perfil».
- **Validación de datos**: formularios y API comprueban formato y obligatoriedad de todos los campos (teléfono de 9 dígitos, DNI/NIE con letra de control, provincia real y código postal coherente con ella, etc.). Teléfono y código postal son `String`, no números. Todas las reglas están descritas en [`doc/validaciones.md`](doc/validaciones.md); las respuestas de error de la API incluyen el detalle por campo (`fieldErrors`). Las fechas se muestran y se escriben como **dd/mm/aaaa** en el cliente; la API sigue usando ISO (`AAAA-MM-DD`).
- **Seguridad**: login con JWT, contraseñas con BCrypt, permisos por rol (lo no declarado se deniega) y por registro (un paciente solo ve sus datos, un médico solo sus citas y prescripciones), y unicidad de DNI/NIF, usuario, tarjeta sanitaria y nº de colegiado. Detalle, matriz de permisos y límites conocidos en [`doc/seguridad.md`](doc/seguridad.md).
- **DTOs en vez de entidades**: evita bucles de serialización JSON en las relaciones y separa el contrato de la API del modelo de persistencia.
- **Código en inglés, producto en español**: los identificadores y los comentarios del código están en inglés (los comentarios solo explican decisiones que el código no deja ver); los textos de cara al usuario (mensajes de validación, documentación de la API, interfaz del cliente de demostración) están en español, y el cliente tiene selector de idioma.
- **Baja lógica**: ningún recurso maestro (centro, médico, paciente, farmacia, medicamento) se borra físicamente; se marca `activo=false`.
