# Validaciones de datos de los formularios

Este documento recoge **todas las comprobaciones** que se aplican a los datos que se introducen en la aplicación (altas de pacientes, médicos, farmacias, centros, medicamentos, administradores, edición del perfil propio, prescripciones...).

> A diferencia del resto de ficheros de esta carpeta (enunciado original de la asignatura), este documento es propio del proyecto y describe el comportamiento implementado.

## 1. Dónde se aplican

Las reglas se comprueban **dos veces**, con las mismas condiciones:

| Capa | Para qué sirve | Dónde está |
|---|---|---|
| **Cliente** (`cliente-demo.html`) | Avisar al usuario al instante, campo a campo, antes de enviar nada. | Constante `RULES` y las listas `*_SPEC` (una por formulario), función `validateForm`. |
| **API** (Spring Boot) | Garantía real: cualquier cliente (Swagger, Postman, otra app) pasa por aquí. | Anotaciones de Bean Validation en los DTO (`dto/`), reglas y mensajes en `validation/ValidationRules.java`. |

Si cambias una regla, **cámbiala en los dos sitios** (ver §7).

Comportamiento del cliente: al pulsar *Guardar* se revisan todos los campos; los incorrectos se marcan en rojo con su motivo debajo y no se envía nada hasta que todo esté bien. Después de un primer intento fallido, los errores se actualizan mientras se escribe.

### Respuesta de la API ante datos incorrectos

`HTTP 400` con los mensajes (en español) y un mapa campo → mensaje:

```json
{
  "timestamp": "2026-10-27T02:10:00",
  "status": 400,
  "error": "Bad Request",
  "message": "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9; El código postal 36202 no corresponde a la provincia de Madrid (debería empezar por 28)",
  "fieldErrors": {
    "phone": "El teléfono debe tener 9 dígitos y empezar por 6, 7, 8 o 9",
    "address.postalCode": "El código postal 36202 no corresponde a la provincia de Madrid (debería empezar por 28)"
  }
}
```

Un JSON mal formado, o un valor de tipo erróneo (p. ej. una fecha que no sea `AAAA-MM-DD`), también devuelve `400` con un mensaje claro.

## Formato de fechas

- **API**: siempre ISO-8601 (`AAAA-MM-DD`, y `AAAA-MM-DDTHH:mm:ss` para fecha y hora). No cambia.
- **Cliente** (`cliente-demo.html`): el usuario ve y escribe **dd/mm/aaaa**, sea cual sea el idioma del navegador. Un `<input type="date">` nativo no permite forzar el formato (usa el del navegador, por eso aparecía mm/dd/aaaa), así que los campos de fecha son de texto con máscara (escribir `12041985` da `12/04/1985`) y un botón de calendario que abre el calendario del navegador. Al enviar se convierte a ISO.
- "Hoy" se calcula con la **fecha local** del usuario, no con UTC (`toISOString()` daría el día anterior entre las 00:00 y las 02:00 en España en horario de verano).
- Se rechazan fechas imposibles (`31/02/2000`) o incompletas.

## 2. Regla general: nada en blanco

Todos los campos de los formularios son **obligatorios**. Un campo vacío, o que solo contenga espacios, se rechaza con «*… es obligatorio*». La única excepción es el **usuario (login)** al crear paciente, médico o farmacia: si se deja vacío se usa un valor por defecto (ver §4); el dato que se guarda nunca queda en blanco.

Antes de validar se **recortan los espacios** de los extremos de todos los textos, y los códigos que se comparan literalmente (DNI, nº de colegiado, tarjeta sanitaria) se pasan a **mayúsculas**. Esto último importa porque la contraseña inicial es el DNI o el nº de colegiado, y el login compara las contraseñas tal cual.

## 3. Reglas por campo

| Campo | Regla | Ejemplo válido | Ejemplo inválido |
|---|---|---|---|
| **Nombre / apellidos** (también los del farmacéutico y el nombre del administrador) | Empieza por letra. Solo letras (con tildes, ñ...), espacios, guion, punto y apóstrofo. 2–60 caracteres. | `María José`, `O'Brien` | `J0hn`, `A`, `-Ana` |
| **DNI / NIE** (paciente y médico) | **DNI**: 8 dígitos + letra de control correcta (el número módulo 23 indica la letra). **NIE**: `X`, `Y` o `Z` + 7 dígitos + letra de control correcta. | `12345678Z`, `X1234567L` | `12345678A` (letra incorrecta), `1234567Z` |
| **DNI / NIF** (farmacia) | DNI o NIE como arriba, **o** un CIF con formato correcto (letra de organización + 7 dígitos + dígito/letra de control; el carácter de control del CIF *no* se verifica). | `12345678Z`, `B12345674` | `TAX-9988` |
| **Tarjeta sanitaria** | 4 letras mayúsculas + 10 dígitos (14 caracteres). | `ABCD1234567890` | `HC-000101`, `ABCD123456789` |
| **Nº Seguridad Social** | 12 dígitos (no se verifican los dígitos de control). | `360000010101` | `36000001010`, `SSN-0001` |
| **Nº colegiado** (médico y farmacéutico) | 4–15 caracteres: letras mayúsculas, dígitos o guiones. | `LIC-4501`, `280012345` | `A B`, `ABC` |
| **Teléfono** | Exactamente **9 dígitos** y empieza por 6, 7, 8 o 9. Solo dígitos: sin espacios, guiones ni prefijo `+34`. | `986123456`, `612345678` | `12345678`, `586123456`, `986 123 456` |
| **Email** | `usuario@dominio.ext`, con extensión de 2 o más letras. Máx. 100 caracteres. | `ana@correo.es` | `ana@correo`, `ana@correo.c` |
| **Usuario (login)** | 3–30 caracteres: letras, dígitos, punto, guion y guion bajo. | `xrodriguez`, `j.garcia` | `ab`, `con espacio` |
| **Domicilio** | 3–120 caracteres. | `20 Calle Príncipe, 3º B` | `ab` |
| **Localidad** | Empieza por letra. Letras, espacios, guion, punto, apóstrofo y barra. 2–60 caracteres. | `Vigo`, `Alicante/Alacant` | `Vigo 2` |
| **Código postal** | 5 dígitos; los dos primeros (01–52) son el código de provincia. Se guarda como **texto**, por lo que se conserva el cero inicial. | `36202`, `04001` | `3620`, `00001`, `53001` |
| **Provincia** | Una de las **52 provincias** reales. Se admiten variantes de grafía (`Orense`/`Ourense`, `Gerona`/`Girona`, `La Coruña`/`A Coruña`, `Vizcaya`/`Bizkaia`...) sin importar mayúsculas ni tildes; la API la guarda con su nombre canónico. En el cliente se elige de una lista desplegable. | `Pontevedra` | `Atlantis`, `Vigo` |
| **Código postal ↔ provincia** | El código postal tiene que pertenecer a la provincia indicada (`36xxx` → Pontevedra). | `36202` + `Pontevedra` | `36202` + `Madrid` |
| **Fecha de nacimiento** | Entre el 01/01/1900 y hoy (ambos incluidos). | `1985-04-12` | fecha futura, `1899-12-31` |
| **Contraseña inicial de administrador / nueva contraseña** | 4–64 caracteres. | `admin`, `20000001Y` | `abc` |
| **Nombre de centro / de establecimiento** | Máx. 100 caracteres (admite números y símbolos: «Farmacia 24h»). | `Centro de Salud Coia` | — |
| **Medicamento** | Nombre comercial, principio activo y fabricante: máx. 120 caracteres; familia: máx. 80. **Dosis por envase**: número entero entre 1 y 1000. | `Ibuprofeno Cinfa 600mg`, `30` | `0`, `5000` |
| **Prescripción** | **Dosis diaria**: mayor que 0 y como máximo 100. **Fecha de fin**: posterior a hoy. **Indicaciones**: obligatorias, máx. 255 caracteres. | `2`, `1 comprimido cada 12 h` | `0`, fecha pasada |

Otras comprobaciones:

- **Paciente**: el médico asignado tiene que trabajar en el centro de salud elegido (HU-A5). En el formulario, la lista de médicos solo muestra los del centro seleccionado.
- **Teléfono y código postal en el cliente**: son campos de texto (no numéricos) que solo aceptan dígitos —sin flechitas— y recortan al máximo (9 y 5). Al escribir el código postal se sugiere automáticamente la provincia (sin pisar una elección manual).
- **Búsqueda de recetas por tarjeta sanitaria** (farmacia): se valida el formato y se admiten minúsculas.

## 4. Contraseñas iniciales

Al dar de alta un usuario, su contraseña inicial es:

| Rol | Contraseña inicial | Origen |
|---|---|---|
| **Paciente** | Su **DNI** (en mayúsculas) | HU-A5 |
| **Médico** | Su **nº de colegiado** (en mayúsculas) — *no* el DNI | HU-A4 |
| **Farmacia** | El **nº de colegiado del farmacéutico** (en mayúsculas) | HU-A6 |
| **Administrador** | La que escribe quien lo da de alta (4–64 caracteres), en el cuerpo de la petición | HU-A2 |

Los formularios de alta lo indican en un recuadro informativo (que además muestra el valor concreto a medida que se escribe). Pacientes, médicos y farmacias pueden cambiarla después desde «Mi perfil».

Usuario (login) por defecto, si se deja vacío: DNI (paciente), nº de colegiado (médico) o DNI/NIF (farmacia). Lo aplica el **cliente** antes de enviar el formulario; la API siempre exige un usuario. Debe ser único (ver [`seguridad.md`](seguridad.md) §6): si el DNI ya es el usuario de otra persona, hay que escribir otro.

> Las contraseñas se guardan cifradas (BCrypt), no en claro; ver [`seguridad.md`](seguridad.md) §5.

## 5. Cambios de modelo que acompañan a estas reglas

- `phone` (teléfono) y `postalCode` (código postal) pasan de `int` a **`String`** en las entidades y en la API (JSON). Motivos: un `int` pierde el cero inicial de los códigos postales de las provincias 01–09 (Almería es `04001`, no `4001`) y no permite distinguir «vacío» de «0».
- Los datos de ejemplo (`DataInitializer`) se han adaptado a las nuevas reglas (DNI con letra correcta, tarjeta sanitaria de 4 letras + 10 dígitos, NIF de farmacéutico válido...). Por eso cambia la contraseña del paciente de prueba, que es su DNI.

| Rol | Usuario | Contraseña |
|---|---|---|
| Administrador | `admin` | `admin` |
| Médico | `lfernandez` | `LIC-4501` |
| Paciente | `xrodriguez` | `20000001Y` |
| Farmacia | `compostela` | `PLIC-778899` |

### Si ya tenías una base de datos MySQL creada

Con `ddl-auto=update` Hibernate **no cambia el tipo de columnas existentes**. Lo más simple es empezar de cero (`docker compose down -v`, o borrar y recrear la base de datos; los datos de ejemplo se vuelven a cargar). Si necesitas conservar datos:

```sql
ALTER TABLE Patient      MODIFY phone VARCHAR(9), MODIFY postalCode VARCHAR(5);
ALTER TABLE Doctor       MODIFY phone VARCHAR(9);
ALTER TABLE Pharmacy     MODIFY phone VARCHAR(9), MODIFY postalCode VARCHAR(5);
ALTER TABLE HealthCenter MODIFY phone VARCHAR(9), MODIFY postalCode VARCHAR(5);
-- Recupera los ceros iniciales que el tipo numérico había perdido:
UPDATE Patient      SET postalCode = LPAD(postalCode, 5, '0');
UPDATE Pharmacy     SET postalCode = LPAD(postalCode, 5, '0');
UPDATE HealthCenter SET postalCode = LPAD(postalCode, 5, '0');
```

Los datos antiguos que no cumplan las reglas (DNI sin letra correcta, tarjeta sanitaria con otro formato...) se seguirán mostrando, pero al editarlos habrá que corregirlos.

## 6. Qué no se comprueba (a propósito)

- **Unicidad**: sí se comprueba (DNI/NIF, usuario, tarjeta sanitaria, nº de colegiado), pero no es una regla de formato sino de datos ya guardados; está descrita en [`seguridad.md`](seguridad.md) §6 (error `409` con el campo afectado).
- **Dígitos de control** del nº de la Seguridad Social y del CIF.
- **Existencia real** de un teléfono, email, dirección o nº de colegiado (solo el formato).
- **Formato de la tarjeta sanitaria**: no hay un formato único en toda España (cada comunidad emite el suyo). Se ha adoptado una convención sencilla de 4 letras + 10 dígitos, inspirada en el código CIP autonómico; es una decisión de este proyecto, no una norma oficial.

## 7. Cómo cambiar una regla

1. **API**: edita la expresión regular y el mensaje en `src/main/java/es/uvigo/dagss/recetas/validation/ValidationRules.java` (todos los DTO las usan desde ahí). Las reglas con lógica (DNI/NIE/CIF en `SpanishIds`, provincias en `SpanishProvinces`, coherencia código postal ↔ provincia en `ConsistentAddress`) tienen su propia clase en ese paquete.
2. **Cliente**: edita la entrada correspondiente de `RULES` en `cliente-demo.html` y su texto en `STRINGS` (`es` y `en`). Para cambiar qué campos lleva un formulario, edita su lista `*_SPEC`.
3. Actualiza esta tabla y los tests.

> Convención de las expresiones regulares: todas aceptan la cadena vacía (empiezan por `|`), de modo que un campo vacío solo se reporta como «obligatorio» y no además como «formato incorrecto».

## 8. Tests

- `src/test/java/es/uvigo/dagss/recetas/validation/SpanishIdsTest` — DNI, NIE y CIF.
- `…/validation/SpanishProvincesTest` — las 52 provincias y sus variantes.
- `…/validation/DtoValidationTest` — las reglas aplicadas a los DTO con el motor real de Bean Validation (teléfono, DNI, tarjeta, código postal ↔ provincia, fecha de nacimiento...).

```bash
./mvnw test
```
