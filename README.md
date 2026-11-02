# OpenSpot — Backend API

API REST para la gestión de espacios de coworking: sedes, pisos, espacios alquilables, disponibilidad, reservas, equipos y amenities.

> **Nombre del producto:** OpenSpot. El código conserva identificadores legacy como `com.cowork.backend`, `cowork-backend` y la base `cowork` por compatibilidad. No es necesario renombrarlos para trabajar en el proyecto.

---

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 3.3.5 |
| Build | Maven 3.9.9 (wrapper incluido) |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | PostgreSQL 16 |
| Migraciones | Flyway 10.22 |
| Seguridad | Spring Security + JWT (JJWT 0.12.6) |
| Login social | Google Identity (ID token) |
| Validación | Jakarta Bean Validation |
| Utilidades | Lombok, `springboot3-dotenv` |

---

## Requisitos

- Java 21
- Docker y Docker Compose (para la base local)
- No hace falta instalar Maven: usá `./mvnw`

---

## Inicio rápido

### 1. Levantar PostgreSQL

```bash
docker compose up -d db
```

El contenedor se llama `cowork-db` y expone PostgreSQL en `localhost:5432` con:

```text
Base:     cowork
Usuario:  cowork
Password: cowork
```

### 2. Configurar variables (opcional para desarrollo local)

Sin `.env`, la aplicación usa los valores por defecto de `application.yml` y funciona contra la base del paso 1. Para habilitar el login con Google, creá un `.env` en la raíz del backend:

```env
APP_GOOGLE_CLIENT_ID=tu-client-id.apps.googleusercontent.com
```

También podés sobrescribir cualquiera de estas variables:

```env
DB_URL=jdbc:postgresql://localhost:5432/cowork
DB_USERNAME=cowork
DB_PASSWORD=cowork
PORT=8080
APP_JWT_SECRET=cambiame-por-un-secreto-largo-de-32-bytes-o-mas
CORS_ORIGINS=http://localhost:5173
```

> El `.env` está ignorado por Git. No subas secretos al repositorio.

### 3. Ejecutar la API

```bash
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8080/api`.

Flyway aplica automáticamente las migraciones al iniciar.

---

## Variables de entorno

| Variable | Default | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cowork` | URL JDBC de PostgreSQL |
| `DB_USERNAME` | `cowork` | Usuario de la base |
| `DB_PASSWORD` | `cowork` | Password de la base |
| `PORT` | `8080` | Puerto HTTP |
| `APP_JWT_SECRET` | Secreto de desarrollo | Firma de JWT. **En producción debe definirse y tener al menos 32 bytes** |
| `CORS_ORIGINS` | `http://localhost:5173` | Orígenes permitidos, separados por coma |
| `APP_GOOGLE_CLIENT_ID` | vacío | Client ID OAuth. Si está vacío, el login con Google responde 403 |

> El `APP_GOOGLE_CLIENT_ID` del backend debe ser el mismo Client ID que usa el frontend en `VITE_GOOGLE_CLIENT_ID`.

---

## Comandos

| Comando | Descripción |
|---|---|
| `docker compose up -d db` | Levanta PostgreSQL en segundo plano |
| `docker compose down` | Detiene los contenedores |
| `./mvnw spring-boot:run` | Ejecuta la API en modo desarrollo |
| `./mvnw test` | Ejecuta los tests |
| `./mvnw clean package` | Genera el JAR en `target/` |
| `java -jar target/cowork-backend-0.0.1-SNAPSHOT.jar` | Ejecuta el JAR compilado |

---

## Arquitectura

Organización **por feature** (no por capa global):

```text
src/main/java/com/cowork/backend/
├── CoworkApplication.java
├── user/           # Usuarios y autenticación
├── location/       # Sedes / edificios
├── workspace/      # Pisos y espacios alquilables
├── reservation/    # Reservas y disponibilidad
├── team/           # Equipos e invitaciones
├── amenity/        # Catálogo de amenities
├── security/       # JWT, Google, configuración de Spring Security
└── common/         # ApiError, excepciones y handler global
```

Flujo de una request:

```text
HTTP → Controller → Service → Repository → Entity → PostgreSQL
                       ↓
                      DTO
```

Convenciones:

- **Controller:** rutas bajo `/api`, validación con `@Valid`, sin lógica de negocio.
- **Service:** reglas de negocio, permisos y transacciones (`@Transactional`).
- **Repository:** Spring Data JPA; consultas derivadas o JPQL.
- **DTO:** `record` inmutables; los requests llevan validaciones Jakarta.
- **Errores:** excepciones de `common` traducidas por `GlobalExceptionHandler`.
- **Permisos:** se validan en los servicios comparando el usuario autenticado con el dueño del recurso.

---

## Modelo de dominio

```text
User (OWNER)
  └── Location (sede / edificio)
        ├── Floor (piso)
        │     └── Workspace (espacio alquilable)
        └── Workspace
              ├── Amenity (N:M)
              └── Reservation

User (TENANT) ── TeamMember ──> Team ──> Reservation (opcional)
```

### Advertencia de nombres

La migración `V5` renombró las tablas históricas:

```text
workspaces (edificio)  →  locations
resources  (espacio)   →  workspaces
```

Por eso:

- **`Location` = sede/edificio.**
- **`Workspace` = espacio alquilable** (sala, escritorio, oficina).
- `Reservation.workspaceId` apunta al espacio, no al edificio.
- `Floor.locationId` y `Workspace.locationId` apuntan a la sede.

### Enums

```text
AccountType:       OWNER, TENANT
WorkspaceType:     HOT_DESK, PRIVATE_OFFICE, MEETING_ROOM
RentalType:        HORA, DIA, MES
ReservationStatus: CONFIRMADA, CANCELADA, COMPLETADA
TeamMemberStatus:  PENDING, ACTIVE
InvitationStatus:  PENDING, ACCEPTED, DECLINED, EXPIRED
```

---

## API

Base: `/api`. Todo requiere `Authorization: Bearer <JWT>` excepto `/api/auth/**`.

| Área | Endpoints principales |
|---|---|
| Autenticación | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/google` |
| Usuario | `GET /api/users/me` |
| Sedes | `GET /api/locations/mine`, `GET /api/locations/{id}`, `POST /api/locations`, `PUT /api/locations/{id}` |
| Pisos | `GET/POST /api/locations/{locationId}/floors`, `PUT/DELETE /api/floors/{id}`, `PUT /api/floors/{id}/map` |
| Espacios | `GET /api/workspaces`, `GET /api/workspaces/{id}`, `GET/POST /api/locations/{locationId}/workspaces`, `PUT/DELETE /api/workspaces/{id}` |
| Reservas | `POST /api/reservations`, `GET /api/reservations/mine`, `GET /api/reservations/location/{locationId}`, `POST /api/reservations/{id}/cancel` |
| Disponibilidad | `GET /api/locations/{locationId}/availability?start=...&end=...&floorId=...` |
| Equipos | `POST /api/teams`, `GET /api/teams/mine`, `GET /api/teams/{id}/members`, `POST /api/teams/{id}/invite` |
| Invitaciones | `GET /api/invitations/mine`, `POST /api/invitations/{token}/accept`, `POST /api/invitations/{token}/decline` |
| Amenities | `GET /api/amenities` |

### Ejemplos

**Crear una sede:**

```http
POST /api/locations
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "nombre": "Sede Palermo",
  "address": "Av. Santa Fe 1234",
  "city": "CABA",
  "province": "Buenos Aires",
  "zipCode": "1425",
  "contactEmail": "hola@openspot.app",
  "contactPhone": "+54 11 0000 0000",
  "isActive": true
}
```

Al crear la sede se crea automáticamente el “Piso 1”.

**Crear una reserva:**

`POST /api/reservations`

```json
{
  "workspaceId": 1,
  "startTime": "2026-11-02T12:00:00Z",
  "endTime": "2026-11-02T14:00:00Z",
  "rentalType": "HORA",
  "teamId": null
}
```

Las fechas se manejan en ISO-8601 UTC.

**Guardar el plano de un piso:**

`PUT /api/floors/{floorId}/map` recibe `mapStructure` como **string JSON**:

```json
{
  "mapStructure": "{\"elements\":[{\"workspaceId\":1,\"x\":40,\"y\":40,\"w\":120,\"h\":70,\"rotation\":0}],\"objects\":[]}"
}
```

---

## Autenticación y permisos

- Sesión **stateless**: no hay cookies ni sesión HTTP.
- El login principal es con **Google ID token** (`POST /api/auth/google`).
- También existen `register` y `login` con email/password, aunque el frontend actual usa solo Google.
- El JWT dura **24 horas** y lleva `sub` (email), `uid` y `type` (`OWNER` o `TENANT`).
- El filtro JWT autentica cada request con el header `Authorization: Bearer <token>`.
- `AuthHelper.currentUser()` busca el usuario en la base a partir del email del token.
- Los permisos de negocio se validan en los servicios:
  - Solo `OWNER` crea sedes.
  - Solo el dueño de la sede administra pisos y espacios.
  - Solo `TENANT` crea equipos.
  - Solo el dueño del equipo invita miembros.
  - Pueden cancelar una reserva: el creador, un miembro activo del equipo o el dueño de la sede.

> En producción definí `APP_JWT_SECRET` y `CORS_ORIGINS`. El secreto por defecto es solo para desarrollo.

---

## Errores

Formato uniforme:

```json
{
  "timestamp": "2026-11-01T12:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "El espacio ya está reservado en ese horario",
  "path": "/api/reservations"
}
```

| Excepción | HTTP |
|---|---|
| `NotFoundException` | 404 |
| `BadRequestException` | 400 |
| `ConflictException` | 409 |
| `ForbiddenException` | 403 |
| Error de validación | 400 |
| Error no controlado | 500 |

---

## Base de datos y migraciones

- Hibernate usa `ddl-auto: none`: el esquema lo controla Flyway.
- Las migraciones viven en `src/main/resources/db/migration/`.
- Al iniciar la aplicación, Flyway aplica las migraciones pendientes.
- **Nunca edites una migración ya aplicada.** Para un cambio de esquema, agregá `V6__descripcion.sql` (y así sucesivamente).
- La migración `V1` crea la extensión `btree_gist`, necesaria para la constraint de exclusión que evita reservas solapadas.
- Si el usuario de la base no puede crear extensiones, creá `btree_gist` manualmente antes de levantar la aplicación.

Migraciones actuales:

| Versión | Contenido |
|---|---|
| `V1` | Usuarios, equipos, invitaciones, edificios, espacios y reservas. Constraint anti-solapamiento |
| `V2` | Pisos, precios por hora/día/mes y moneda |
| `V3` | Login con Google (`google_sub`, password nullable) |
| `V4` | Amenities |
| `V5` | Modelo jerárquico: `locations`, `floors`, `workspaces`; modalidad y precio total de reservas |

---

## Tests

```bash
./mvnw test
```

Actualmente hay tests unitarios del login con Google en `AuthServiceGoogleTest`. Al agregar lógica nueva, sumá tests siguiendo el estilo existente (JUnit 5 + Mockito). No hay tests de integración ni de reservas todavía.

---

## Despliegue

Checklist mínimo:

- [ ] Java 21 en el host.
- [ ] PostgreSQL accesible y credenciales en `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
- [ ] `APP_JWT_SECRET` definido con al menos 32 bytes.
- [ ] `CORS_ORIGINS` apuntando al dominio del frontend.
- [ ] `APP_GOOGLE_CLIENT_ID` configurado y coincidente con el frontend.
- [ ] `PORT` definido si la plataforma lo requiere (Render, Railway, etc.).
- [ ] Extensión `btree_gist` disponible en la base.

Build:

```bash
./mvnw clean package
java -jar target/cowork-backend-0.0.1-SNAPSHOT.jar
```

No hay `Dockerfile` en el repositorio; el `docker-compose.yml` solo levanta PostgreSQL para desarrollo.

---

## Problemas comunes

- **No conecta a PostgreSQL:** verificá que el contenedor esté arriba con `docker compose ps` y volvé a ejecutar `docker compose up -d db`.
- **Flyway falla por `btree_gist`:** creá la extensión con un usuario que tenga permisos (`CREATE EXTENSION IF NOT EXISTS btree_gist;`).
- **El login con Google responde 403:** falta `APP_GOOGLE_CLIENT_ID` o no coincide con el `VITE_GOOGLE_CLIENT_ID` del frontend.
- **Errores de CORS en el navegador:** agregá el origen del frontend a `CORS_ORIGINS`.
- **El JWT no es válido después de reiniciar:** si cambiaste `APP_JWT_SECRET`, los tokens emitidos antes dejan de servir.

---

## Documentación adicional

- **Contexto completo del proyecto (back + front):** `../CONTEXTO-OPENSPOT.md`, en la carpeta raíz que contiene ambos repositorios.
- **Frontend:** [github.com/LeandroRossetti/Frontend-OpenSpot](https://github.com/LeandroRossetti/Frontend-OpenSpot)

---

## Estado actual y limitaciones conocidas

- No hay OpenAPI/Swagger ni colección de Postman versionada.
- No hay CI ni pipeline de despliegue.
- No hay endpoint para borrar sedes ni equipos.
- `GET /api/workspaces` devuelve todos los espacios, incluidos los inactivos, sin filtros ni paginación.
- Las reservas no validan que la fecha de inicio sea futura.
- El estado `COMPLETADA` de una reserva no se asigna automáticamente.
- Las invitaciones a equipos no envían emails reales: el token queda disponible vía API.
- No hay refresh token; el JWT vence a las 24 horas.
- `DELETE /api/workspaces/{id}` puede fallar si el espacio tiene reservas asociadas.
