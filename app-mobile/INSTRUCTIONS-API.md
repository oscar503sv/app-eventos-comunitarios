# INSTRUCTIONS-API.md

Guía para consumir el backend de **Eventos Comunitarios** desde una app Android nativa en **Kotlin**.

Esta guía es autosuficiente: copia los snippets, ajusta el `BASE_URL` y empieza a consumir la API.

---

## Tabla de contenido

1. [Introducción](#1-introducción)
2. [Configuración base](#2-configuración-base)
3. [Autenticación con Firebase](#3-autenticación-con-firebase)
4. [Modelos de datos (data classes Kotlin)](#4-modelos-de-datos-data-classes-kotlin)
5. [Catálogo de endpoints](#5-catálogo-de-endpoints)
6. [Setup completo de Retrofit](#6-setup-completo-de-retrofit)
7. [Reglas de negocio importantes](#7-reglas-de-negocio-importantes-gotchas)
8. [Checklist rápido](#8-checklist-rápido)

---

## 1. Introducción

Esta API expone los siguientes recursos:

- **Eventos** — crear, listar, ver detalle, actualizar, eliminar.
- **Asistencias (RSVP)** — confirmar o cancelar asistencia a un evento.
- **Reseñas** — calificar (1–5) y comentar eventos.
- **Historial** — eventos que el usuario organizó o a los que asistió.
- **Perfil de usuario** — datos básicos + conteos agregados (organizados, asistidos, reseñas).
- **Health check** — estado público del servicio.

**Stack del backend** (solo informativo):

- Node.js + Express + TypeScript
- Prisma ORM + MySQL
- Firebase Admin SDK para autenticación

**Documentación interactiva (Swagger):** con el servidor corriendo, abre `http://localhost:3000/api-docs` para explorar/probar cada endpoint en vivo. La especificación OpenAPI está en `http://localhost:3000/api-docs.json`.

---

## 2. Configuración base

### URL base

| Entorno | URL |
|---|---|
| Emulador Android (dev) | `http://10.0.2.2:3000` |
| Dispositivo físico en LAN (dev) | `http://<IP_DE_TU_PC>:3000` |
| Host local (Postman, etc.) | `http://localhost:3000` |
| Producción | `<API_PUBLIC_URL>` — pendiente de configurar |

> En Android, `localhost` apunta al propio dispositivo. Para alcanzar tu PC desde el emulador usa `10.0.2.2`.

### Cleartext traffic (HTTP en desarrollo)

Android 9+ bloquea HTTP por defecto. Para desarrollo añade en `AndroidManifest.xml`:

```xml
<application
    android:usesCleartextTraffic="true"
    ... >
```

O mejor, restringe a tu dominio dev con `res/xml/network_security_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">10.0.2.2</domain>
        <domain includeSubdomains="true">localhost</domain>
    </domain-config>
</network-security-config>
```

Y referéncialo:

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    ... >
```

### Formato general

- `Content-Type`: `application/json`
- Fechas: **ISO 8601 UTC** (`2025-06-15T18:00:00.000Z`)
- IDs: **UUID v4** (string)
- Sin paginación, sin rate limiting, CORS abierto
- Encoding: UTF-8

### Forma estándar de respuesta

**Éxito** — siempre incluye `success: true`:

```json
{ "success": true, "<recurso>": { ... } }
```

**Error** — payload uniforme con la llave `error`:

```json
{ "error": "Mensaje descriptivo" }
```

---

## 3. Autenticación con Firebase

Todos los endpoints **excepto `GET /health`** requieren un **Firebase ID Token** en el header:

```
Authorization: Bearer <FIREBASE_ID_TOKEN>
```

El backend valida el token con `admin.auth().verifyIdToken(token)`. **No emite tokens propios**: la fuente de verdad es Firebase.

### 3.1 Setup en el proyecto Android

**`app/build.gradle.kts`** (Kotlin DSL):

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

dependencies {
    // Firebase BoM — alinea versiones
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-auth-ktx")

    // Coroutines + helpers
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1") // para .await()
}
```

**`build.gradle.kts`** (proyecto):

```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

> Coloca el `google-services.json` en `app/` (el mismo proyecto Firebase que el backend).

### 3.2 Login y obtención del ID Token

```kotlin
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

suspend fun signIn(email: String, password: String) {
    FirebaseAuth.getInstance()
        .signInWithEmailAndPassword(email, password)
        .await()
}

suspend fun currentIdToken(forceRefresh: Boolean = false): String? {
    return FirebaseAuth.getInstance().currentUser
        ?.getIdToken(forceRefresh)?.await()?.token
}
```

### 3.3 AuthInterceptor (inyecta token en cada request)

```kotlin
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class FirebaseAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { currentIdToken(forceRefresh = false) }
        val request = chain.request().newBuilder().apply {
            token?.let { header("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}
```

### 3.4 Authenticator (refresca token automáticamente en 401)

```kotlin
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class FirebaseAuthAuthenticator : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        // Evita bucle infinito: si ya reintentamos, rendirse.
        if (response.request.header("X-Retry") != null) return null

        val freshToken = runBlocking { currentIdToken(forceRefresh = true) } ?: return null

        return response.request.newBuilder()
            .header("Authorization", "Bearer $freshToken")
            .header("X-Retry", "1")
            .build()
    }
}
```

### 3.5 Sincronización automática de usuario

**No hay endpoint de registro/signup.** La primera vez que el cliente llame a un endpoint protegido, el backend:

1. Verifica el token de Firebase.
2. Busca al usuario en la BD por `firebaseUid`, luego por `email`.
3. Si no existe, lo **crea** con `firebaseUid`, `email` y `displayName` extraídos del token.
4. Si existe, actualiza `email` si cambió. **`displayName` NO se sobrescribe desde el token** — solo se inicializa la primera vez que el usuario aparece en la BD. Después de eso, el usuario controla su nombre vía `PUT /api/users/profile`.

> Esta semántica es importante: los tokens de Firebase cachean las claims (incluido `name`) por ~1 hora, así que si el middleware sobrescribiera `displayName` desde el token en cada request, cualquier edición vía PUT se revertiría hasta que el token caducara.

Recomendación: tras hacer login con Firebase, dispara cualquier request autenticado (p. ej. `GET /api/events`) para forzar la sincronización antes de que el usuario intente crear/asistir/reseñar.

---

## 4. Modelos de datos (data classes Kotlin)

Todas las fechas se serializan como `String` ISO 8601. Recomendado mapearlas a `java.time.Instant` con un Moshi adapter (ver sección 6).

> **Convención usada en este documento:** las data classes asumen **Moshi** (sin anotaciones especiales porque los nombres coinciden). Si usas Gson, agrega `@SerializedName("nombre_del_json")` en cualquier campo que renombres.

### 4.1 Usuario

```kotlin
data class User(
    val id: String,
    val firebaseUid: String,
    val email: String,
    val displayName: String?,
    val createdAt: String,   // ISO 8601
    val updatedAt: String    // ISO 8601
)

/** Versión reducida que aparece anidada en respuestas. */
data class UserPublic(
    val id: String,
    val firebaseUid: String? = null,
    val displayName: String?,
    val email: String? = null
)

/** Conteos agregados que aparecen en GET /api/users/profile. */
data class ProfileCounts(
    val organized: Int,   // eventos organizados por el usuario
    val attended: Int,    // asistencias confirmadas (status='confirmed')
    val reviews: Int      // reseñas escritas por el usuario
)

/** Forma que devuelve GET /api/users/profile. */
data class UserProfile(
    val user: User,
    val counts: ProfileCounts
)
```

### 4.2 Evento

```kotlin
data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,         // ISO 8601
    val location: String,
    val category: String,     // CULTURA | MUSICA | DEPORTE | EDUCACION | GASTRONOMIA | SALUD | OTRO
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String
)

data class EventCounts(
    val attendances: Int,
    val reviews: Int
)

/** Forma que devuelve GET /api/events */
data class EventSummary(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String,
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String,
    val organizer: UserPublic,
    val _count: EventCounts
)

/** Forma que devuelve GET /api/events/:id */
data class EventDetail(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String,
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String,
    val organizer: UserPublic,
    val attendances: List<EventAttendanceWithUser>,
    val reviews: List<ReviewWithUser>
)
```

> `category` se modela como `String` para tolerar valores futuros. Constantes sugeridas:
>
> ```kotlin
> object EventCategory {
>     const val CULTURA = "CULTURA"
>     const val MUSICA = "MUSICA"
>     const val DEPORTE = "DEPORTE"
>     const val EDUCACION = "EDUCACION"
>     const val GASTRONOMIA = "GASTRONOMIA"
>     const val SALUD = "SALUD"
>     const val OTRO = "OTRO"
> }
> ```

### 4.3 Asistencia

```kotlin
data class EventAttendance(
    val id: String,
    val userId: String,
    val eventId: String,
    val status: String,       // "confirmed" | "cancelled"
    val createdAt: String
)

data class EventAttendanceWithUser(
    val id: String,
    val userId: String,
    val eventId: String,
    val status: String,
    val createdAt: String,
    val user: UserPublic
)

/** Item de "attended" en GET /api/events/my-events */
data class AttendedEvent(
    val id: String,
    val userId: String,
    val eventId: String,
    val status: String,
    val createdAt: String,
    val event: Event
)
```

> `status` se modela como `String` (no enum) para tolerar valores futuros. Comparar con las constantes:
>
> ```kotlin
> object AttendanceStatus {
>     const val CONFIRMED = "confirmed"
>     const val CANCELLED = "cancelled"
> }
> ```

### 4.4 Reseña

```kotlin
data class Review(
    val id: String,
    val userId: String,
    val eventId: String,
    val rating: Int,          // 1..5
    val comment: String?,
    val createdAt: String
)

data class ReviewWithUser(
    val id: String,
    val userId: String,
    val eventId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val user: UserPublic
)

data class ReviewStats(
    val average: Double,
    val count: Int
)
```

### 4.5 Health

```kotlin
data class HealthResponse(
    val status: String,            // "ok" | "degraded"
    val uptime: Double,
    val timestamp: String,
    val version: String,
    val environment: String,
    val services: HealthServices
)

data class HealthServices(
    val database: String           // "ok" | "error"
)
```

### 4.6 Wrappers de respuesta

```kotlin
data class Pagination(
    val page: Int,
    val limit: Int,
    val total: Int,        // total de elementos en toda la colección
    val totalPages: Int,
    val hasMore: Boolean   // true si existe al menos una página adicional
)

data class EventsResponse(
    val success: Boolean,
    val events: List<EventSummary>,
    val pagination: Pagination
)
data class EventDetailResponse(val success: Boolean, val event: EventDetail)
data class EventResponse(val success: Boolean, val event: Event)
data class AttendanceResponse(val success: Boolean, val attendance: EventAttendance)
data class MyEventsResponse(
    val success: Boolean,
    val organized: List<Event>,
    val attended: List<AttendedEvent>
)
data class ReviewsResponse(
    val success: Boolean,
    val reviews: List<ReviewWithUser>,
    val stats: ReviewStats
)
data class ReviewResponse(val success: Boolean, val review: Review)
data class ProfileResponse(val success: Boolean, val profile: UserProfile)
data class SuccessResponse(val success: Boolean, val message: String)

data class ApiError(val error: String)
```

### 4.7 Request bodies

```kotlin
data class CreateEventRequest(
    val title: String,
    val description: String?,
    val date: String,        // ISO 8601
    val location: String,
    val category: String? = null  // opcional; null o ausente → "OTRO"
)

data class UpdateEventRequest(
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String? = null  // opcional; null o ausente → no se modifica
)

data class CreateReviewRequest(
    val rating: Int,         // 1..5
    val comment: String?
)

data class UpdateProfileRequest(
    val displayName: String  // requerido, 1..80 caracteres (se trimea en backend)
)
```

---

## 5. Catálogo de endpoints

Resumen rápido:

| # | Método | Ruta | Auth | Descripción |
|---|---|---|---|---|
| 5.1 | GET | `/health` | No | Estado del servicio |
| 5.2 | GET | `/api/events` | Sí | Listar eventos |
| 5.3 | GET | `/api/events/my-events` | Sí | Historial del usuario |
| 5.4 | GET | `/api/events/{id}` | Sí | Detalle de evento |
| 5.5 | POST | `/api/events` | Sí | Crear evento |
| 5.6 | PUT | `/api/events/{id}` | Sí | Actualizar evento (solo organizador) |
| 5.7 | DELETE | `/api/events/{id}` | Sí | Eliminar evento (solo organizador) |
| 5.8 | POST | `/api/events/{id}/attend` | Sí | Confirmar asistencia |
| 5.9 | POST | `/api/events/{id}/cancel` | Sí | Cancelar asistencia |
| 5.10 | GET | `/api/reviews/{eventId}` | Sí | Reseñas + estadísticas |
| 5.11 | POST | `/api/reviews/{eventId}` | Sí | Crear/actualizar reseña |
| 5.12 | GET | `/api/users/profile` | Sí | Perfil del usuario + conteos |
| 5.13 | PUT | `/api/users/profile` | Sí | Actualizar perfil (displayName, sync con Firebase) |

---

### 5.1 `GET /health` — público

Estado del servicio. No requiere token. Útil para validar conectividad desde la app.

**Response 200**:

```json
{
  "status": "ok",
  "uptime": 1234.56,
  "timestamp": "2026-05-17T12:34:56.789Z",
  "version": "0.0.1",
  "environment": "development",
  "services": { "database": "ok" }
}
```

**Response 503** (cuando la BD falla):

```json
{
  "status": "degraded",
  "uptime": 1234.56,
  "timestamp": "2026-05-17T12:34:56.789Z",
  "version": "0.0.1",
  "environment": "development",
  "services": { "database": "error" }
}
```

**Retrofit:**

```kotlin
@GET("health")
suspend fun health(): HealthResponse
```

---

### 5.2 `GET /api/events`

Lista los eventos ordenados por **fecha ascendente** (próximos primero), incluyendo organizador y conteos. **Paginado**.

**Query params**:

| Campo | Tipo | Requerido | Default | Notas |
|---|---|---|---|---|
| `page` | integer | ❌ | `1` | 1-indexed. Debe ser ≥ 1. |
| `limit` | integer | ❌ | `20` | Cantidad por página. Rango **1–50**. |

> Ejemplo: `GET /api/events?page=2&limit=10`

**Response 200**:

```json
{
  "success": true,
  "events": [
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "title": "Taller de fotografía urbana",
      "description": "Aprende técnicas de fotografía callejera con fotógrafos locales.",
      "date": "2025-06-15T18:00:00.000Z",
      "location": "Plaza Mayor, Ciudad de México",
      "category": "CULTURA",
      "organizerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "createdAt": "2025-05-01T09:00:00.000Z",
      "updatedAt": "2025-05-10T11:00:00.000Z",
      "organizer": {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "displayName": "Ana García",
        "email": "ana@example.com"
      },
      "_count": { "attendances": 12, "reviews": 4 }
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 47,
    "totalPages": 3,
    "hasMore": true
  }
}
```

> `hasMore` es `true` cuando `page < totalPages`. Si la colección está vacía, `total` y `totalPages` valen `0` y `hasMore` es `false`.

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "\"page\" debe ser un entero mayor o igual a 1" }` o `{ "error": "\"limit\" debe ser un entero entre 1 y 50" }` |
| 401 | `{ "error": "No token provided" }` |
| 500 | `{ "error": "Error al obtener eventos" }` |

**Retrofit:**

```kotlin
@GET("api/events")
suspend fun getEvents(
    @Query("page") page: Int = 1,
    @Query("limit") limit: Int = 20
): EventsResponse
```

> **Patrón de scroll infinito sugerido:** llama con `page = 1` al cargar; cuando el usuario llegue al final y `pagination.hasMore == true`, pide `page + 1` y concatena `events` a la lista existente.

---

### 5.3 `GET /api/events/my-events`

Historial del usuario autenticado: eventos que **organizó** + registros de **asistencia** (con el evento embebido).

> Esta ruta debe ir **antes** de `/api/events/{id}` en cualquier cliente que use coincidencia por orden — pero como usas Retrofit con paths literales, no hay problema.

`organized` viene ordenado por `date` desc. `attended` por `createdAt` desc.

**Response 200**:

```json
{
  "success": true,
  "organized": [
    {
      "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "title": "Taller de fotografía urbana",
      "description": "Aprende técnicas de fotografía callejera...",
      "date": "2025-06-15T18:00:00.000Z",
      "location": "Plaza Mayor, Ciudad de México",
      "organizerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "createdAt": "2025-05-01T09:00:00.000Z",
      "updatedAt": "2025-05-01T09:00:00.000Z"
    }
  ],
  "attended": [
    {
      "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "eventId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "status": "confirmed",
      "createdAt": "2025-05-15T08:00:00.000Z",
      "event": {
        "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "title": "Taller de fotografía urbana",
        "description": "...",
        "date": "2025-06-15T18:00:00.000Z",
        "location": "Plaza Mayor, Ciudad de México",
        "organizerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "createdAt": "2025-05-01T09:00:00.000Z",
        "updatedAt": "2025-05-01T09:00:00.000Z"
      }
    }
  ]
}
```

> Importante: `attended` incluye registros con `status: "cancelled"`. Filtra en cliente si solo quieres confirmados.

**Errores**:

| Status | Body |
|---|---|
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al obtener historial" }` |

**Retrofit:**

```kotlin
@GET("api/events/my-events")
suspend fun getMyEvents(): MyEventsResponse
```

---

### 5.4 `GET /api/events/{id}`

Detalle completo: organizador, asistentes (con info de usuario) y reseñas (con info de usuario).

**Path params:** `id` — UUID del evento.

**Response 200**:

```json
{
  "success": true,
  "event": {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "title": "Taller de fotografía urbana",
    "description": "Aprende técnicas de fotografía callejera con fotógrafos locales.",
    "date": "2025-06-15T18:00:00.000Z",
    "location": "Plaza Mayor, Ciudad de México",
    "organizerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "createdAt": "2025-05-01T09:00:00.000Z",
    "updatedAt": "2025-05-10T11:00:00.000Z",
    "organizer": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "firebaseUid": "firebase_uid_abc123",
      "displayName": "Ana García",
      "email": "ana@example.com"
    },
    "attendances": [
      {
        "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
        "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "eventId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
        "status": "confirmed",
        "createdAt": "2025-05-15T08:00:00.000Z",
        "user": {
          "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
          "firebaseUid": "firebase_uid_abc123",
          "displayName": "Ana García"
        }
      }
    ],
    "reviews": []
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "ID de evento requerido" }` |
| 401 | `{ "error": "No token provided" }` |
| 404 | `{ "error": "Evento no encontrado" }` |
| 500 | `{ "error": "Error al obtener evento" }` |

**Retrofit:**

```kotlin
@GET("api/events/{id}")
suspend fun getEventById(@Path("id") id: String): EventDetailResponse
```

---

### 5.5 `POST /api/events`

Crea un evento. El usuario autenticado queda como **organizador**.

**Request body**:

```json
{
  "title": "Taller de fotografía urbana",
  "description": "Aprende técnicas de fotografía callejera con fotógrafos locales.",
  "date": "2025-06-15T18:00:00.000Z",
  "location": "Plaza Mayor, Ciudad de México",
  "category": "CULTURA"
}
```

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `title` | string | ✅ | |
| `description` | string \| null | ❌ | `null` permitido |
| `date` | string ISO 8601 | ✅ | |
| `location` | string | ✅ | |
| `category` | string | ❌ | Uno de: `CULTURA`, `MUSICA`, `DEPORTE`, `EDUCACION`, `GASTRONOMIA`, `SALUD`, `OTRO`. Si se omite o llega `null`, se guarda como `"OTRO"`. |

**Response 201**:

```json
{
  "success": true,
  "event": {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "title": "Taller de fotografía urbana",
    "description": "Aprende técnicas de fotografía callejera con fotógrafos locales.",
    "date": "2025-06-15T18:00:00.000Z",
    "location": "Plaza Mayor, Ciudad de México",
    "category": "CULTURA",
    "organizerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "createdAt": "2025-05-01T09:00:00.000Z",
    "updatedAt": "2025-05-01T09:00:00.000Z"
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "Categoría inválida. Valores permitidos: CULTURA, MUSICA, DEPORTE, EDUCACION, GASTRONOMIA, SALUD, OTRO" }` |
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al crear evento" }` |

**Retrofit:**

```kotlin
@POST("api/events")
suspend fun createEvent(@Body body: CreateEventRequest): EventResponse
```

---

### 5.6 `PUT /api/events/{id}`

Actualiza un evento. **Solo el organizador original puede modificarlo** — otros usuarios reciben 403.

**Path params:** `id` — UUID del evento.

**Request body:** mismo shape que `CreateEventRequest`. El campo `category` es opcional; si no se envía, se conserva la categoría existente.

**Response 200**:

```json
{
  "success": true,
  "event": {
    "id": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "title": "Taller de fotografía urbana (actualizado)",
    "description": "Sesión ampliada con práctica en exteriores.",
    "date": "2025-06-16T18:00:00.000Z",
    "location": "Parque Lincoln, Ciudad de México",
    "category": "CULTURA",
    "organizerId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "createdAt": "2025-05-01T09:00:00.000Z",
    "updatedAt": "2025-05-20T15:00:00.000Z"
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "ID de evento requerido" }` o `{ "error": "Categoría inválida. Valores permitidos: ..." }` |
| 401 | `{ "error": "No autenticado" }` |
| 403 | `{ "error": "Solo el organizador puede editar el evento" }` |
| 404 | `{ "error": "Evento no encontrado" }` |
| 500 | `{ "error": "Error al actualizar evento" }` |

**Retrofit:**

```kotlin
@PUT("api/events/{id}")
suspend fun updateEvent(
    @Path("id") id: String,
    @Body body: UpdateEventRequest
): EventResponse
```

---

### 5.7 `DELETE /api/events/{id}`

Elimina un evento permanentemente. **Solo el organizador original puede eliminarlo** — otros usuarios reciben 403. Las **asistencias** y **reseñas** asociadas al evento se borran **en cascada** automáticamente.

**Path params:** `id` — UUID del evento.

**No envía body.**

**Response 200**:

```json
{
  "success": true,
  "message": "Evento eliminado"
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "ID de evento requerido" }` |
| 401 | `{ "error": "No autenticado" }` |
| 403 | `{ "error": "Solo el organizador puede eliminar el evento" }` |
| 404 | `{ "error": "Evento no encontrado" }` |
| 500 | `{ "error": "Error al eliminar evento" }` |

**Retrofit:**

```kotlin
@DELETE("api/events/{id}")
suspend fun deleteEvent(@Path("id") id: String): SuccessResponse
```

> Necesitarás añadir el wrapper:
>
> ```kotlin
> data class SuccessResponse(val success: Boolean, val message: String)
> ```

---

### 5.8 `POST /api/events/{id}/attend`

Confirma asistencia (RSVP). Comportamiento **upsert**: si ya había un registro (incluso `cancelled`), lo actualiza a `confirmed`. **No envía body.**

**Path params:** `id` — UUID del evento.

**Response 200**:

```json
{
  "success": true,
  "attendance": {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "eventId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "status": "confirmed",
    "createdAt": "2025-05-15T08:00:00.000Z"
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "ID de evento requerido" }` |
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al confirmar asistencia" }` |

**Retrofit:**

```kotlin
@POST("api/events/{id}/attend")
suspend fun attendEvent(@Path("id") id: String): AttendanceResponse
```

---

### 5.9 `POST /api/events/{id}/cancel`

Cancela una asistencia previa (cambia `status` a `cancelled`). **Requiere haber confirmado antes** — si no existe el registro, devolverá 500.

**Path params:** `id` — UUID del evento.

**Response 200**:

```json
{
  "success": true,
  "attendance": {
    "id": "c3d4e5f6-a7b8-9012-cdef-123456789012",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "eventId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "status": "cancelled",
    "createdAt": "2025-05-15T08:00:00.000Z"
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "ID de evento requerido" }` |
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al cancelar asistencia" }` |

**Retrofit:**

```kotlin
@POST("api/events/{id}/cancel")
suspend fun cancelAttendance(@Path("id") id: String): AttendanceResponse
```

---

### 5.10 `GET /api/reviews/{eventId}`

Lista las reseñas de un evento (con usuario) + estadísticas agregadas. Ordenadas por `createdAt` desc.

**Path params:** `eventId` — UUID del evento.

**Response 200**:

```json
{
  "success": true,
  "reviews": [
    {
      "id": "d4e5f6a7-b8c9-0123-def0-234567890123",
      "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "eventId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
      "rating": 4,
      "comment": "Excelente taller, muy bien organizado.",
      "createdAt": "2025-06-16T10:00:00.000Z",
      "user": {
        "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "displayName": "Ana García"
      }
    }
  ],
  "stats": { "average": 4.2, "count": 8 }
}
```

> Si no hay reseñas, `stats` viene como `{ "average": 0, "count": 0 }`.

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "ID de evento requerido" }` |
| 401 | `{ "error": "No token provided" }` |
| 500 | `{ "error": "Error al obtener reviews" }` |

**Retrofit:**

```kotlin
@GET("api/reviews/{eventId}")
suspend fun getReviews(@Path("eventId") eventId: String): ReviewsResponse
```

---

### 5.11 `POST /api/reviews/{eventId}`

Crea o actualiza una reseña (**upsert** por `(userId, eventId)`). Un usuario solo puede tener **una** reseña por evento.

**Path params:** `eventId` — UUID del evento.

**Request body**:

```json
{
  "rating": 4,
  "comment": "Excelente taller, muy bien organizado."
}
```

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `rating` | integer | ✅ | Rango **1–5 inclusive** |
| `comment` | string \| null | ❌ | `null` permitido |

**Response 201**:

```json
{
  "success": true,
  "review": {
    "id": "d4e5f6a7-b8c9-0123-def0-234567890123",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "eventId": "b2c3d4e5-f6a7-8901-bcde-f12345678901",
    "rating": 4,
    "comment": "Excelente taller, muy bien organizado.",
    "createdAt": "2025-06-16T10:00:00.000Z"
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "Rating debe ser entre 1 y 5" }` o `{ "error": "ID de evento requerido" }` |
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al crear review" }` |

**Retrofit:**

```kotlin
@POST("api/reviews/{eventId}")
suspend fun createReview(
    @Path("eventId") eventId: String,
    @Body body: CreateReviewRequest
): ReviewResponse
```

---

### 5.12 `GET /api/users/profile`

Devuelve los datos básicos del usuario autenticado más conteos agregados: eventos organizados, asistencias confirmadas y reseñas escritas. **No envía body, no recibe query params.**

> Los conteos vienen calculados al momento — no hay caché — así que reflejan el estado actual de la BD.

**Response 200**:

```json
{
  "success": true,
  "profile": {
    "user": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "firebaseUid": "firebase_uid_abc123",
      "email": "ana@example.com",
      "displayName": "Ana García",
      "createdAt": "2025-01-15T10:30:00.000Z",
      "updatedAt": "2025-03-20T14:00:00.000Z"
    },
    "counts": {
      "organized": 5,
      "attended": 12,
      "reviews": 3
    }
  }
}
```

| Campo | Tipo | Notas |
|---|---|---|
| `profile.user` | object | Datos del usuario en BD (mismo `User` que en sección 4.1). |
| `profile.counts.organized` | integer | Eventos en los que el usuario es organizador. |
| `profile.counts.attended` | integer | Asistencias **confirmadas** (no incluye `cancelled`). |
| `profile.counts.reviews` | integer | Reseñas que el usuario ha escrito. |

**Errores**:

| Status | Body |
|---|---|
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al obtener perfil" }` |

**Retrofit:**

```kotlin
@GET("api/users/profile")
suspend fun getProfile(): ProfileResponse
```

---

### 5.13 `PUT /api/users/profile`

Actualiza el `displayName` del usuario autenticado. El backend escribe el nuevo nombre tanto en la **base de datos** (fuente de verdad para la app) como en **Firebase Auth** (para que `currentUser.displayName` también se actualice tras refrescar). Devuelve el perfil actualizado en el mismo formato que `GET /api/users/profile`.

> **Por qué se sincroniza Firebase:** la BD es la fuente de verdad para la app (el middleware nunca sobrescribe `displayName` desde el token, ver sección 3.5), pero actualizar Firebase mantiene `FirebaseUser.displayName` alineado para cualquier código que lo lea directamente. El backend hace primero la llamada a Firebase Admin y luego la BD.

**Request body**:

```json
{
  "displayName": "Ana García López"
}
```

| Campo | Tipo | Requerido | Notas |
|---|---|---|---|
| `displayName` | string | ✅ | 1–80 caracteres. Los espacios extremos se trimean en backend. No se permite `null` ni cadena vacía. |

**Response 200**:

```json
{
  "success": true,
  "profile": {
    "user": {
      "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
      "firebaseUid": "firebase_uid_abc123",
      "email": "ana@example.com",
      "displayName": "Ana García López",
      "createdAt": "2025-01-15T10:30:00.000Z",
      "updatedAt": "2026-05-22T16:00:00.000Z"
    },
    "counts": {
      "organized": 5,
      "attended": 12,
      "reviews": 3
    }
  }
}
```

**Errores**:

| Status | Body |
|---|---|
| 400 | `{ "error": "\"displayName\" es requerido y debe ser un string" }` o `{ "error": "\"displayName\" debe tener entre 1 y 80 caracteres" }` |
| 401 | `{ "error": "No autenticado" }` |
| 404 | `{ "error": "Usuario no encontrado" }` |
| 500 | `{ "error": "Error al actualizar perfil" }` (incluye fallos al sincronizar con Firebase) |

**Retrofit:**

```kotlin
@PUT("api/users/profile")
suspend fun updateProfile(@Body body: UpdateProfileRequest): ProfileResponse
```

> **Tip cliente:** después de un PUT exitoso, el `displayName` viejo seguirá en el `FirebaseUser` local hasta que llames a `currentUser?.reload()` o renueves el token con `getIdToken(forceRefresh = true)`. Como ya tienes el nuevo nombre en la respuesta, lo más simple es usar `profile.user.displayName` en tu UI.

---

## 6. Setup completo de Retrofit

### 6.1 Dependencias Gradle

```kotlin
dependencies {
    // Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.moshi:moshi-adapters:1.15.1")

    // OkHttp logging
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

### 6.2 API interface completa

```kotlin
import retrofit2.http.*

interface EventsApi {

    // --- Health ---
    @GET("health")
    suspend fun health(): HealthResponse

    // --- Eventos ---
    @GET("api/events")
    suspend fun getEvents(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): EventsResponse

    @GET("api/events/my-events")
    suspend fun getMyEvents(): MyEventsResponse

    @GET("api/events/{id}")
    suspend fun getEventById(@Path("id") id: String): EventDetailResponse

    @POST("api/events")
    suspend fun createEvent(@Body body: CreateEventRequest): EventResponse

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: String,
        @Body body: UpdateEventRequest
    ): EventResponse

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): SuccessResponse

    @POST("api/events/{id}/attend")
    suspend fun attendEvent(@Path("id") id: String): AttendanceResponse

    @POST("api/events/{id}/cancel")
    suspend fun cancelAttendance(@Path("id") id: String): AttendanceResponse

    // --- Reseñas ---
    @GET("api/reviews/{eventId}")
    suspend fun getReviews(@Path("eventId") eventId: String): ReviewsResponse

    @POST("api/reviews/{eventId}")
    suspend fun createReview(
        @Path("eventId") eventId: String,
        @Body body: CreateReviewRequest
    ): ReviewResponse

    // --- Usuario ---
    @GET("api/users/profile")
    suspend fun getProfile(): ProfileResponse

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ProfileResponse
}
```

### 6.3 Cliente Retrofit (singleton)

```kotlin
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Cámbialo según entorno (idealmente vía BuildConfig)
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        // En release usa Level.NONE o Level.BASIC
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttp: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(FirebaseAuthInterceptor())
        .authenticator(FirebaseAuthAuthenticator())
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: EventsApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EventsApi::class.java)
    }
}
```

> **Sugerido:** define `BASE_URL` por `buildConfigField` para alternar dev/staging/prod sin tocar código:
>
> ```kotlin
> // app/build.gradle.kts
> android {
>     buildFeatures.buildConfig = true
>     buildTypes {
>         debug   { buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/\"") }
>         release { buildConfigField("String", "BASE_URL", "\"https://tu-dominio.com/\"") }
>     }
> }
> ```
>
> Luego: `private val BASE_URL = BuildConfig.BASE_URL`.

### 6.4 Resultado tipado + Repository

```kotlin
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
}

import com.squareup.moshi.Moshi
import retrofit2.HttpException
import java.io.IOException

class EventsRepository(
    private val api: EventsApi = RetrofitClient.api,
    moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
) {
    private val errorAdapter = moshi.adapter(ApiError::class.java)

    suspend fun getEvents(page: Int = 1, limit: Int = 20): ApiResult<EventsResponse> = safeCall {
        api.getEvents(page, limit)
    }

    suspend fun createEvent(req: CreateEventRequest): ApiResult<Event> = safeCall {
        api.createEvent(req).event
    }

    suspend fun attend(eventId: String): ApiResult<EventAttendance> = safeCall {
        api.attendEvent(eventId).attendance
    }

    suspend fun reviewsFor(eventId: String): ApiResult<ReviewsResponse> = safeCall {
        api.getReviews(eventId)
    }

    suspend fun profile(): ApiResult<UserProfile> = safeCall {
        api.getProfile().profile
    }

    suspend fun updateDisplayName(name: String): ApiResult<UserProfile> = safeCall {
        api.updateProfile(UpdateProfileRequest(displayName = name)).profile
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val parsed = runCatching { errorAdapter.fromJson(body) }.getOrNull()
        ApiResult.Error(e.code(), parsed?.error ?: "HTTP ${e.code()}")
    } catch (e: IOException) {
        ApiResult.Error(-1, "Sin conexión: ${e.message}")
    } catch (e: Exception) {
        ApiResult.Error(-2, e.message ?: "Error desconocido")
    }
}
```

### 6.5 Uso desde un ViewModel

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class EventsViewModel(
    private val repo: EventsRepository = EventsRepository()
) : ViewModel() {

    val events = MutableStateFlow<List<EventSummary>>(emptyList())
    val error  = MutableStateFlow<String?>(null)

    private var page = 1
    private var hasMore = true

    /** Carga la primera página (resetea el estado). */
    fun load() {
        page = 1
        hasMore = true
        viewModelScope.launch {
            when (val result = repo.getEvents(page = 1)) {
                is ApiResult.Success -> {
                    events.value = result.data.events
                    hasMore = result.data.pagination.hasMore
                }
                is ApiResult.Error -> error.value = result.message
            }
        }
    }

    /** Llámalo al llegar al final de la lista (scroll infinito). */
    fun loadMore() {
        if (!hasMore) return
        viewModelScope.launch {
            when (val result = repo.getEvents(page = page + 1)) {
                is ApiResult.Success -> {
                    events.value = events.value + result.data.events
                    page = result.data.pagination.page
                    hasMore = result.data.pagination.hasMore
                }
                is ApiResult.Error -> error.value = result.message
            }
        }
    }
}
```

---

## 7. Reglas de negocio importantes (gotchas)

| # | Regla | Status que devuelve si la violas |
|---|---|---|
| 1 | Solo el organizador puede editar un evento. | 403 |
| 2 | Solo el organizador puede eliminar un evento. Al hacerlo, sus asistencias y reseñas se borran en cascada. | 403 |
| 3 | Un usuario solo puede tener **una** reseña por evento (POST hace upsert silencioso). | — (upsert) |
| 4 | Un usuario solo puede tener **un** registro de asistencia por evento (attend tras cancel reactiva el mismo registro). | — (upsert) |
| 5 | `rating` debe ser entero **1–5** inclusive. | 400 |
| 6 | `description` y `comment` son nullable; envía `null` si no aplican. | — |
| 7 | `category` es opcional al crear/editar: si no se envía, en `POST` se guarda como `"OTRO"` y en `PUT` se conserva la actual. Solo se aceptan los 7 valores del enum. | 400 si valor inválido |
| 8 | `GET /api/events` viene **paginado** (`page` y `limit`, defaults `1` y `20`, `limit` máx 50) y ordenado por `date` ascendente (próximos primero). Usa `pagination.hasMore` para decidir si pedir la siguiente página. | 400 si page/limit inválidos |
| 9 | El usuario en BD se crea automáticamente en la primera request autenticada (no hay endpoint de signup). | — |
| 10 | Cancelar asistencia requiere haber confirmado antes; si no existe el registro, devuelve 500. | 500 |
| 11 | `attendances` en `GET /api/events/my-events` incluye registros `cancelled` — filtra en cliente si solo quieres confirmados. | — |
| 12 | Todas las fechas son **ISO 8601 UTC**; convierte a hora local en el cliente. | — |
| 13 | La BD es la **fuente de verdad** para `displayName`. El middleware solo lo inicializa desde el token de Firebase cuando el usuario aún no existe en BD; después nunca lo sobrescribe. Usa `PUT /api/users/profile` para editarlo. | — |
| 14 | Tras un `PUT /api/users/profile` exitoso, el `FirebaseUser` local conserva el nombre viejo hasta que llames `currentUser?.reload()` o `getIdToken(forceRefresh = true)`. Para la UI, prefiere `profile.user.displayName` que viene en la respuesta. | — |

---

## 8. Checklist rápido

Antes de tu primer request:

- [ ] `google-services.json` colocado en `app/` (mismo proyecto Firebase que el backend).
- [ ] Plugins `com.google.gms.google-services` aplicados en `build.gradle.kts`.
- [ ] Dependencias de Retrofit + Moshi + OkHttp logging + Firebase Auth añadidas.
- [ ] `usesCleartextTraffic="true"` o `network_security_config.xml` configurado (para dev con HTTP).
- [ ] `BASE_URL` apuntando a `http://10.0.2.2:3000/` para emulador.
- [ ] `FirebaseAuthInterceptor` añadido al `OkHttpClient`.
- [ ] `FirebaseAuthAuthenticator` añadido para refresco automático en 401.
- [ ] Login con Firebase funciona — `FirebaseAuth.getInstance().currentUser` no es null.
- [ ] Primer smoke test: `RetrofitClient.api.health()` devuelve `status: "ok"`.
- [ ] Segundo smoke test (autenticado): `RetrofitClient.api.getEvents()` devuelve lista (puede estar vacía).

Si los dos smoke tests pasan, estás listo para integrar el resto de pantallas.
