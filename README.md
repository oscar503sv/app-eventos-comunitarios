# Aplicación de Gestión de Eventos Comunitarios

## Segundo Proyecto DSM941

**Materia:** Desarrollo de Software para Móviles - DSM941  
**Ciclo:** I-2026  
**Repositorio:** https://github.com/oscar503sv/app-eventos-comunitarios.git  
**Diseño UX/UI en Figma:** https://www.figma.com/design/9gaP6mCS2g9YmuQ5W0nh9Y/eventos-cumunidad?node-id=0-1&p=f&t=kiDnWKa7cEUPgSLM-0  
**API desplegada:** https://app-eventos-comunitarios-production.up.railway.app/  
**Documentación de API:** https://app-eventos-comunitarios-production.up.railway.app/api-docs/

---

## Descripción del proyecto

La Aplicación de Gestión de Eventos Comunitarios es una aplicación móvil desarrollada en Android con Kotlin y Jetpack Compose. Su finalidad es permitir que una comunidad pueda organizar, consultar y participar en diferentes eventos o actividades comunitarias.

La aplicación permite que los usuarios puedan registrarse, iniciar sesión, visualizar eventos disponibles, consultar el detalle de cada evento, confirmar asistencia, revisar sus eventos y visualizar su historial de participación.

El proyecto también cuenta con una API REST desplegada en Railway, la cual permite gestionar usuarios, eventos, asistencias, perfiles y demás información necesaria para el funcionamiento de la aplicación.

---

## Objetivo general

Desarrollar una aplicación móvil para la gestión de eventos comunitarios, permitiendo a los usuarios registrarse, iniciar sesión, consultar eventos, confirmar asistencia y revisar su historial de participación dentro de la comunidad.

---

## Objetivos específicos

- Implementar autenticación de usuarios mediante correo, contraseña y redes sociales.
- Permitir la visualización de eventos comunitarios disponibles.
- Facilitar la creación, edición y eliminación de eventos.
- Permitir que los usuarios confirmen o cancelen su asistencia a eventos.
- Mostrar eventos relacionados con cada usuario.
- Presentar el historial de participación del usuario.
- Mostrar estadísticas básicas de participación.
- Implementar una interfaz clara, amigable y fácil de utilizar.
- Documentar correctamente el proyecto, sus enlaces principales y la licencia utilizada.

---

## Integrantes del equipo

| Nombre | Carnet |
|---|---|
| María Sandra Palacios Ramírez | PR243125 |
| María Norma Palacios Ramírez | PR242879 |
| Oscar Mauricio Aragon Hernández | AH100129 |
| Raquel Sugey Saenz Guevara | SG142513 |
| Enrique Alexander Flores Cazun | FC243028 |
| Bryan Rubén De Paz Rivera | DR202095 |

---

## Tecnologías utilizadas

### Aplicación móvil

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Retrofit
- Moshi
- Firebase Authentication
- Google Sign-In
- Facebook Login
- Gradle

### Backend / API

- Node.js
- Express
- MySQL
- Railway
- Swagger / OpenAPI

### Diseño y colaboración

- Figma
- Git
- GitHub

---

## Funcionalidades principales

### Autenticación

La aplicación permite que los usuarios puedan registrarse e iniciar sesión utilizando correo y contraseña. También se integran opciones de autenticación social con Google y Facebook.

### Gestión de eventos

La aplicación permite visualizar eventos comunitarios con información como título, descripción, ubicación, fecha y categoría.

Los organizadores pueden crear, editar y eliminar eventos según corresponda.

### Confirmación de asistencia

Los usuarios pueden confirmar o cancelar su asistencia a un evento. Esta información se guarda en la API y posteriormente puede consultarse desde la pantalla de historial.

### Mis eventos

La aplicación cuenta con una sección donde el usuario puede visualizar los eventos relacionados con su cuenta, ya sea porque los organizó o porque confirmó asistencia.

### Historial de participación

La pantalla de historial permite visualizar los eventos en los que el usuario ha confirmado asistencia. Esta sección consume datos reales desde la API y muestra la información registrada para el usuario autenticado.

También se presenta información importante de cada evento, como el nombre, descripción, ubicación, fecha y estado de asistencia.

### Estadísticas de participación

La sección de historial incluye estadísticas básicas como:

- Total de eventos registrados.
- Eventos con asistencia confirmada.
- Eventos en historial.
- Porcentaje aproximado de participación.

Estas estadísticas permiten que el usuario pueda conocer de forma rápida su nivel de participación dentro de la comunidad.

### Validaciones y mensajes amigables

Se agregaron validaciones en el formulario de eventos para mejorar la experiencia del usuario y evitar que se registren eventos con información incompleta o incorrecta.

Entre las validaciones implementadas se encuentran:

- El título del evento es obligatorio.
- El título debe tener al menos 4 caracteres.
- La ubicación del evento es obligatoria.
- La descripción del evento es obligatoria.
- La descripción debe tener al menos 10 caracteres.
- La fecha y hora del evento no pueden ser anteriores al momento actual.
- Se muestran mensajes amigables cuando ocurre un error al guardar el evento.

### Perfil de usuario

La aplicación permite consultar la información del usuario y actualizar su nombre de perfil.

---

## Estructura general del proyecto

```text
app-eventos-comunitarios
├── app-mobile
│   ├── app
│   │   ├── src
│   │   │   ├── main
│   │   │   │   ├── java/com/eventos/comunitarios
│   │   │   │   │   ├── data
│   │   │   │   │   │   ├── model
│   │   │   │   │   │   ├── network
│   │   │   │   │   │   └── repository
│   │   │   │   │   ├── ui
│   │   │   │   │   │   ├── auth
│   │   │   │   │   │   ├── events
│   │   │   │   │   │   ├── main
│   │   │   │   │   │   ├── onboarding
│   │   │   │   │   │   ├── profile
│   │   │   │   │   │   ├── splash
│   │   │   │   │   │   └── theme
│   │   │   │   │   └── util
│   │   │   │   └── res
│   │   └── build.gradle.kts
│   ├── local.properties
│   └── settings.gradle.kts
│
├── backend
│   ├── src
│   ├── prisma
│   ├── package.json
│   └── .env
│
└── README.md
```

---

## Configuración de la aplicación móvil

Para ejecutar correctamente la aplicación móvil, se debe abrir la carpeta `app-mobile` desde Android Studio y configurar los archivos necesarios para que la app pueda conectarse con Firebase, Facebook Login y la API desplegada en Railway.

### 1. Abrir el proyecto en Android Studio

Se debe abrir únicamente la carpeta:

```text
app-mobile
```

No se debe abrir solamente la carpeta `backend`, ya que la aplicación Android se encuentra dentro de `app-mobile`.

---

### 2. Agregar el archivo `google-services.json`

El archivo `google-services.json` debe colocarse dentro de la siguiente ruta:

```text
app-mobile/app/google-services.json
```

Este archivo es necesario para que Firebase Authentication funcione correctamente dentro de la aplicación.

---

### 3. Configurar el archivo `local.properties`

Dentro de la carpeta `app-mobile`, se debe abrir el archivo:

```text
local.properties
```

En ese archivo se deben agregar las siguientes variables de configuración:

```properties
API_BASE_URL=https://app-eventos-comunitarios-production.up.railway.app/

GOOGLE_WEB_CLIENT_ID=824033087574-qjsii3n8ghvgb8b5bo13vqfi2g2g2bhg.apps.googleusercontent.com
FACEBOOK_APP_ID=364310207302809
FB_LOGIN_PROTOCOL_SCHEME=fb364310207302809
FACEBOOK_CLIENT_TOKEN=55e273e3746b7981359af6d8e7ac4c75
```

La variable `API_BASE_URL` apunta a la API desplegada en Railway.


---

### 4. Sincronizar Gradle

Después de configurar `local.properties`, se debe sincronizar el proyecto desde Android Studio usando la opción:

```text
Sync Now
```

o desde el menú:

```text
File > Sync Project with Gradle Files
```

---

### 5. Ejecutar la aplicación

Finalmente, se debe seleccionar un emulador o dispositivo físico y presionar el botón de ejecución:

```text
Run
```

Con esta configuración, la aplicación podrá conectarse con la API, Firebase Authentication y los servicios de inicio de sesión social.

---

## Ejecución del proyecto móvil

1. Clonar el repositorio:

```bash
git clone https://github.com/oscar503sv/app-eventos-comunitarios.git
```

2. Entrar a la carpeta del proyecto móvil:

```bash
cd app-eventos-comunitarios/app-mobile
```

3. Abrir la carpeta `app-mobile` en Android Studio.

4. Sincronizar Gradle.

5. Colocar el archivo `google-services.json` dentro de `app-mobile/app/`.

6. Configurar las variables necesarias en `local.properties`.

7. Ejecutar la aplicación en un emulador o dispositivo físico.

---

## API del proyecto

La aplicación consume una API REST desplegada en Railway.

**URL base de la API:**

```text
https://app-eventos-comunitarios-production.up.railway.app/
```

**Documentación Swagger/OpenAPI:**

```text
https://app-eventos-comunitarios-production.up.railway.app/api-docs/
```

Algunos endpoints utilizados por la aplicación son:

```text
GET    /api/events
GET    /api/events/{id}
POST   /api/events
PUT    /api/events/{id}
DELETE /api/events/{id}
POST   /api/events/{id}/attend
POST   /api/events/{id}/cancel
GET    /api/events/my-events
GET    /api/users/profile
PUT    /api/users/profile
```

---

## Diseño UX/UI

El diseño visual de la aplicación fue trabajado en Figma. Este diseño sirvió como guía para la creación de las pantallas principales de la aplicación móvil.

**Enlace al diseño en Figma:**

```text
https://www.figma.com/design/9gaP6mCS2g9YmuQ5W0nh9Y/eventos-cumunidad?node-id=0-1&p=f&t=kiDnWKa7cEUPgSLM-0
```

El diseño contempla pantallas como:

- Pantalla de bienvenida.
- Inicio de sesión.
- Registro de usuario.
- Listado de eventos.
- Detalle de evento.
- Mis eventos.
- Historial.
- Perfil de usuario.

---

## Flujo general de uso

1. El usuario abre la aplicación.
2. El usuario se registra o inicia sesión.
3. La aplicación muestra el listado de eventos disponibles.
4. El usuario selecciona un evento para ver su detalle.
5. El usuario confirma su asistencia.
6. El evento confirmado se registra en su historial.
7. El usuario puede consultar sus estadísticas de participación.
8. El usuario puede cerrar sesión desde la aplicación.

---
## Licencia Creative Commons

Este proyecto implementa la licencia:

**Creative Commons Atribución-NoComercial-CompartirIgual 4.0 Internacional**

También conocida como:

```text
CC BY-NC-SA 4.0
```

Más información sobre la licencia:

```text
https://creativecommons.org/licenses/by-nc-sa/4.0/deed.es
```

---

## Estado actual del proyecto

Actualmente el proyecto cuenta con las siguientes funcionalidades:

- Registro de usuarios.
- Inicio de sesión con correo y contraseña.
- Inicio de sesión social con Google y Facebook.
- Listado de eventos.
- Detalle de eventos.
- Creación de eventos.
- Edición de eventos.
- Eliminación de eventos.
- Confirmación de asistencia.
- Cancelación de asistencia.
- Pantalla de mis eventos.
- Pantalla de historial.
- Estadísticas básicas de participación.
- Validaciones y mensajes amigables en el formulario de eventos.
- Perfil de usuario.
- Consumo de API desplegada en Railway.

---

## Conclusión

La Aplicación de Gestión de Eventos Comunitarios permite organizar y consultar actividades dentro de una comunidad local. Además, facilita la participación de los usuarios por medio de la confirmación de asistencia y el historial personal.

La implementación del historial, las estadísticas, las validaciones y los mensajes amigables permite que el usuario tenga una mejor experiencia dentro de la aplicación, haciendo que el proyecto sea más completo, claro y útil para la comunidad.