# Seguridad: autenticación, permisos y unicidad

Este documento describe cómo se protege la API y qué reglas de acceso se aplican. Como `validaciones.md`, es propio del proyecto (no forma parte del enunciado original).

## 1. Resumen

| Aspecto | Qué se hace | Dónde |
|---|---|---|
| **Contraseñas** | Se guardan con **BCrypt** (hash con sal), nunca en claro. Ni la API ni los logs las devuelven. | `config/SecurityConfig` (`PasswordEncoder`), servicios de usuarios |
| **Autenticación** | `POST /api/auth/login` devuelve un **token JWT** (HS256) firmado. Hay que enviarlo en cada petición: `Authorization: Bearer <token>`. Sin sesiones ni cookies. | `services/AuthService`, `security/JwtService` |
| **Permisos por rol** | Cada endpoint declara qué roles pueden llamarlo; lo que no está declarado se **deniega**. | `config/SecurityConfig` (`securityFilterChain`) |
| **Permisos por registro** | Un paciente solo ve *sus* datos; un médico solo cierra *sus* citas y anula *sus* prescripciones; etc. | `security/AccessPolicy` |
| **Unicidad** | No se pueden repetir DNI/NIF, usuario, tarjeta sanitaria ni nº de colegiado. | `services/UniquenessChecker` + restricciones `UNIQUE` en las entidades |
| **Errores** | `401` (sin sesión/token caducado), `403` (sin permiso), `409` (valor duplicado), todos en JSON como el resto de errores. | `security/RestSecurityErrorHandler`, `exceptions/GlobalExceptionHandler` |

## 2. Flujo de autenticación

1. `POST /api/auth/login` con `{"username": "...", "password": "..."}`.
2. Respuesta `200`:
   ```json
   { "id": 5, "role": "PATIENT", "name": "Xoan", "lastName": "Rodriguez Iglesias",
     "token": "eyJhbGciOiJIUzI1NiJ9...", "expiresAt": "2026-10-27T04:30:00Z" }
   ```
3. En las siguientes llamadas: cabecera `Authorization: Bearer <token>`.
4. El token contiene el usuario (`sub`), su id (`uid`) y su rol (`role`) y **caduca a los 120 minutos** (configurable). Pasado ese tiempo, o si está manipulado, la API responde `401` y hay que volver a iniciar sesión (el cliente de demostración lo hace solo).

Detalles del login:
- El usuario no distingue mayúsculas de minúsculas (`XRodriguez` = `xrodriguez`).
- Usuario inexistente, contraseña incorrecta y cuenta dada de baja devuelven **el mismo** error (`Usuario o contraseña incorrectos`), y se hace el mismo trabajo de hash en los tres casos, para no revelar qué usuarios existen.
- Una contraseña guardada sin hash (p. ej. una base de datos antigua) **nunca** se acepta (ver §7).

### Probarlo en Swagger UI
`http://localhost:8080/swagger-ui.html` → ejecuta `POST /api/auth/login` → copia el valor de `token` → botón **Authorize** → pégalo (sin la palabra `Bearer`).

## 3. Permisos por rol

`A` = administrador, `M` = médico, `P` = paciente, `F` = farmacia. «propio» = solo sobre su propio registro (se comprueba además en `AccessPolicy`, ver §4).

| Endpoint | A | M | P | F |
|---|:-:|:-:|:-:|:-:|
| `POST /api/auth/login`, Swagger (`/swagger-ui.html`, `/v3/api-docs`) | público |||
| `/api/administrators/**` | Sí | | | |
| `GET /api/health-centers`, `GET /api/medications` (y `/{id}`) | Sí | Sí | Sí | Sí |
| `POST/PUT/DELETE` de centros y medicamentos | Sí | | | |
| `GET /api/doctors` (listado), `POST/PUT/DELETE /api/doctors/**` | Sí | | | |
| `GET /api/doctors/{id}`, `GET /api/doctors/{id}/schedule` | Sí | propio | | |
| `PATCH /api/doctors/{id}/profile` · `/password` | | propio | | |
| `GET /api/patients` (listado), `POST/PUT/DELETE /api/patients/**` | Sí | | | |
| `GET /api/patients/{id}` | Sí | sus pacientes | propio | |
| `GET /api/patients/{id}/prescriptions` | | sus pacientes | propio | |
| `GET /api/patients/{id}/appointments` | Sí | | propio | |
| `GET /api/patients/{id}/available-slots` · `/refills` | | | propio | |
| `PATCH /api/patients/{id}/profile` · `/password` | | | propio | |
| `GET /api/pharmacies` (listado), `POST/PUT/DELETE /api/pharmacies/**` | Sí | | | |
| `GET /api/pharmacies/{id}` | Sí | | | propio |
| `PATCH /api/pharmacies/{id}/profile` · `/password` | | | | propio |
| `GET /api/appointments?day=` (agenda general) | Sí | | | |
| `POST /api/appointments` (reservar) | | | para sí mismo | |
| `POST /api/appointments/{id}/cancel` | Sí | de sus citas | de sus citas | |
| `POST /api/appointments/{id}/no-show` · `/complete` | | de sus citas | | |
| `POST /api/prescriptions` | | como él mismo | | |
| `POST /api/prescriptions/{id}/cancel` | | las que emitió | | |
| `GET /api/refills?healthCardNumber=` | | | | Sí |
| `POST /api/refills/{id}/fill?pharmacyId=` | | | | como ella misma |

Cualquier otra ruta (incluida cualquier ruta que no exista) se deniega. Un rol no permitido recibe `403`; una petición sin token válido, `401`.

## 4. Permisos por registro (`AccessPolicy`)

El filtro de URL solo conoce el rol. `AccessPolicy` comprueba además *a quién* pertenece el dato y lanza `403` si no corresponde:

| Comprobación | Quién pasa |
|---|---|
| `requireSelf(rol, id)` | Solo el usuario de ese rol y ese id (perfil, contraseña, reservar cita, prescribir, servir receta). |
| `requireAdminOrSelf(rol, id)` | Un administrador, o el propio usuario. |
| `requirePatientRecordAccess(id)` | Un administrador, el propio paciente, o **su médico asignado**. |
| `requireAppointmentParticipant(id)` | Un administrador, o el paciente o el médico de esa cita (cancelar). |
| `requireAppointmentDoctor(id)` | Solo el médico de esa cita (marcarla como atendida / no presentado). |
| `requirePrescriptionDoctor(id)` | Solo el médico que emitió la prescripción (anularla). |

Cuando el recurso no existe se devuelve `404`.

## 5. Contraseñas

- **Almacenamiento**: BCrypt (coste por defecto de Spring Security). Cada alta y cada cambio de contraseña pasa por `PasswordEncoder`.
- **Contraseñas iniciales** (sin cambios respecto a antes, ver `validaciones.md` §4): paciente = DNI, médico = nº de colegiado, farmacia = nº de colegiado del farmacéutico. Ahora se guardan *ya con hash*. El **administrador** recibe la que escribe quien lo crea: viaja en el **cuerpo** de la petición (`{"name","email","username","password"}`), ya no por la URL (donde acababa en logs e historial) y **ya no existe una contraseña por defecto** («changeme»).
- **Cambio de contraseña**: exige la actual; la nueva debe tener 4–64 caracteres y no superar **72 bytes en UTF-8**, porque BCrypt solo lee los 72 primeros bytes (con letras acentuadas cada una ocupa 2).

## 6. Unicidad

| Dato | Debe ser único entre… | Campo del error `409` |
|---|---|---|
| Usuario (login) | **todos** los usuarios (admins, médicos, pacientes y farmacias), sin distinguir mayúsculas | `username` |
| DNI/NIE | los pacientes · y, por separado, los médicos | `nationalId` |
| Tarjeta sanitaria | los pacientes | `healthCardNumber` |
| Nº de colegiado | los médicos | `licenseNumber` |
| DNI/NIF de la farmacia | las farmacias | `taxId` |
| Nº de colegiado del farmacéutico | las farmacias | `pharmacistLicenseNumber` |

- El usuario es único *entre todos los roles* porque el login lo busca en las cuatro tablas a la vez; si hubiera dos iguales no se sabría quién entra.
- El DNI se permite repetido entre roles distintos a propósito: un médico también puede ser paciente. Si el usuario por defecto (el DNI) ya está cogido por la ficha de médico, el formulario avisa y hay que elegir otro usuario.
- Los registros **dados de baja** siguen reservando sus valores (la baja es lógica): el mensaje lo indica.
- Se comprueba antes de guardar (`UniquenessChecker`) y, como última barrera frente a dos peticiones simultáneas, con restricciones `UNIQUE` en la base de datos (cuyo error también se traduce a `409`). Al **editar**, un registro no choca consigo mismo.

```json
HTTP 409
{ "status": 409, "error": "Conflict",
  "message": "Ya existe un paciente con ese DNI/NIE (puede estar dado de baja)",
  "fieldErrors": { "nationalId": "Ya existe un paciente con ese DNI/NIE (puede estar dado de baja)" } }
```

## 7. Si ya tenías una base de datos MySQL con datos

Las contraseñas antiguas están en claro y la aplicación ya **no las acepta**, así que nadie podría iniciar sesión. Lo más simple es empezar de cero (`docker compose down -v`: los datos de ejemplo se vuelven a cargar con las contraseñas ya cifradas). Si quieres conservar datos, hay que reescribir cada contraseña con su hash BCrypt (por ejemplo con un pequeño script que use `BCryptPasswordEncoder`) y comprobar antes que no hay duplicados de usuario, DNI, tarjeta o colegiado, porque `ddl-auto=update` intentará crear las restricciones `UNIQUE` y fallará si los hay.

## 8. Configuración

| Propiedad | Variable de entorno | Por defecto | Para qué |
|---|---|---|---|
| `app.security.jwt-secret` | `JWT_SECRET` | *(vacío)* | Clave con la que se firman los tokens (≥ 32 caracteres). Si está vacía se genera una **aleatoria en cada arranque** (y se avisa en el log): los tokens dejan de valer al reiniciar. Pon una fija en cualquier entorno real, p. ej. `JWT_SECRET=$(openssl rand -hex 32)`. |
| `app.security.jwt-ttl-minutes` | `JWT_TTL_MINUTES` | `120` | Duración de un token. |
| `app.cors.allowed-origins` | `CORS_ALLOWED_ORIGINS` | `*` | Orígenes de navegador permitidos (separados por comas). `*` vale para la demo (el cliente se abre como `file://`); en un despliegue real, el origen de tu frontend. |

`docker-compose.yml` pasa `JWT_SECRET` a la aplicación si lo defines en tu entorno o en un fichero `.env`.

## 9. Otros cambios de seguridad

- **Eliminado `spring-boot-starter-data-rest`.** Sin configuración adicional publicaba *todos* los repositorios (`/patients`, `/doctors`...) con sus campos —contraseñas incluidas— y con permiso de escritura, **sin autenticación**. El proyecto no lo usaba (la API son los controladores de `/api`).
- **Consola H2 desactivada** (`spring.h2.console.enabled=false`): permitía ver y modificar toda la base de datos sin pasar por la API. Para inspeccionar tablas en local, actívala en `application-h2.properties` y permite `/h2-console/**` en `SecurityConfig` (además de `headers.frameOptions` en modo `sameOrigin`).
- **CORS** pasa a definirse en `SecurityConfig` (y permite `PATCH`, que antes faltaba y rompía desde el navegador el cambio de perfil y de contraseña).
- **CSRF desactivado** a propósito: la API no usa cookies ni sesión (el token va en una cabecera), así que no hay nada que un sitio ajeno pueda aprovechar.
- **Cliente de demostración**: guarda el token **solo en memoria** (recargar la página cierra la sesión) y, ante un `401`, vuelve al login con un aviso.

## 10. Límites conocidos (posibles mejoras)

- **Sin revocación**: un token sigue siendo válido hasta que caduca, aunque el usuario se dé de baja o cambie su contraseña. Mitigación: duración corta; mejora: comprobar el estado del usuario en cada petición o llevar una lista de tokens revocados.
- **Sin limitación de intentos de login** (fuerza bruta). Mejora: bloqueo temporal por usuario/IP o `bucket4j`.
- **Sin refresh tokens**: al caducar hay que volver a iniciar sesión.
- **Contraseña inicial = dato conocido** (DNI / nº de colegiado), por requisito del enunciado. Mejora: obligar a cambiarla en el primer acceso.
- **Solo HTTP en local**: en un despliegue real debe servirse por HTTPS (los tokens viajan en una cabecera).
- **Secreto simétrico** (HS256): válido para una sola aplicación. Con varios servicios que validen tokens convendría firma asimétrica (RS256).

## 11. Tests

- `src/test/java/es/uvigo/dagss/recetas/security/SecurityIntegrationTest` — de extremo a extremo con `MockMvc` y los datos de ejemplo: login, `401`/`403` por rol y por registro, contraseñas con hash, CORS, Swagger público, repositorios ya no expuestos, cambio de contraseña, alta de administrador y los `409` por duplicados.
- `…/security/AccessPolicyTest` — las reglas de §4 (Mockito, sin Spring).
- `…/services/UniquenessCheckerTest` — las reglas de §6 (Mockito, sin Spring).
- `…/validation/PasswordPolicyTest` — longitud y límite de 72 bytes.
