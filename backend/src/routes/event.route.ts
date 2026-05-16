import { Router } from 'express';
import { verifyFirebaseToken, attachDbUser } from '../middleware/auth.middleware.js';
import {
  createEvent,
  getEvents,
  getEventById,
  attendEvent,
  cancelAttendance,
  getUserEvents,
  updateEvent,
} from '../controllers/event.controller.js';

const eventRouter = Router();

/**
 * @openapi
 * /api/events:
 *   get:
 *     tags: [Events]
 *     summary: Listar todos los eventos
 *     description: Devuelve todos los eventos ordenados por fecha ascendente, incluyendo el organizador y conteo de asistencias y reseñas.
 *     responses:
 *       200:
 *         description: Lista de eventos
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 events:
 *                   type: array
 *                   items:
 *                     $ref: '#/components/schemas/EventSummary'
 *             example:
 *               success: true
 *               events:
 *                 - id: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                   title: "Taller de fotografía urbana"
 *                   description: "Aprende técnicas de fotografía callejera con fotógrafos locales."
 *                   date: "2025-06-15T18:00:00.000Z"
 *                   location: "Plaza Mayor, Ciudad de México"
 *                   organizerId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   createdAt: "2025-05-01T09:00:00.000Z"
 *                   updatedAt: "2025-05-10T11:00:00.000Z"
 *                   organizer:
 *                     id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                     displayName: "Ana García"
 *                     email: "ana@example.com"
 *                   _count:
 *                     attendances: 12
 *                     reviews: 4
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No token provided"
 *
 *   post:
 *     tags: [Events]
 *     summary: Crear un nuevo evento
 *     description: Crea un evento con el usuario autenticado como organizador.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [title, date, location]
 *             properties:
 *               title:
 *                 type: string
 *                 description: Título del evento
 *               description:
 *                 type: string
 *                 nullable: true
 *                 description: Descripción detallada del evento
 *               date:
 *                 type: string
 *                 format: date-time
 *                 description: Fecha y hora del evento (ISO 8601)
 *               location:
 *                 type: string
 *                 description: Lugar donde se realizará el evento
 *           example:
 *             title: "Taller de fotografía urbana"
 *             description: "Aprende técnicas de fotografía callejera con fotógrafos locales."
 *             date: "2025-06-15T18:00:00.000Z"
 *             location: "Plaza Mayor, Ciudad de México"
 *     responses:
 *       201:
 *         description: Evento creado exitosamente
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 event:
 *                   $ref: '#/components/schemas/Event'
 *             example:
 *               success: true
 *               event:
 *                 id: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                 title: "Taller de fotografía urbana"
 *                 description: "Aprende técnicas de fotografía callejera con fotógrafos locales."
 *                 date: "2025-06-15T18:00:00.000Z"
 *                 location: "Plaza Mayor, Ciudad de México"
 *                 organizerId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                 createdAt: "2025-05-01T09:00:00.000Z"
 *                 updatedAt: "2025-05-01T09:00:00.000Z"
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No autenticado"
 *       404:
 *         description: Usuario no encontrado en la base de datos (llamar a /api/users/sync primero)
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Usuario no encontrado"
 */
eventRouter.get('/', verifyFirebaseToken, getEvents);

/**
 * @openapi
 * /api/events/my-events:
 *   get:
 *     tags: [Events]
 *     summary: Historial de eventos del usuario
 *     description: Devuelve los eventos que el usuario autenticado ha organizado y a los que ha confirmado asistencia.
 *     responses:
 *       200:
 *         description: Historial de eventos del usuario
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 organized:
 *                   type: array
 *                   description: Eventos que el usuario organizó
 *                   items:
 *                     $ref: '#/components/schemas/Event'
 *                 attended:
 *                   type: array
 *                   description: Registros de asistencia del usuario con el evento incluido
 *                   items:
 *                     allOf:
 *                       - $ref: '#/components/schemas/EventAttendance'
 *                       - type: object
 *                         properties:
 *                           event:
 *                             $ref: '#/components/schemas/Event'
 *             example:
 *               success: true
 *               organized:
 *                 - id: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                   title: "Taller de fotografía urbana"
 *                   date: "2025-06-15T18:00:00.000Z"
 *                   location: "Plaza Mayor, Ciudad de México"
 *                   organizerId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   createdAt: "2025-05-01T09:00:00.000Z"
 *                   updatedAt: "2025-05-01T09:00:00.000Z"
 *               attended:
 *                 - id: "c3d4e5f6-a7b8-9012-cdef-123456789012"
 *                   userId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   eventId: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                   status: "confirmed"
 *                   createdAt: "2025-05-15T08:00:00.000Z"
 *                   event:
 *                     id: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                     title: "Taller de fotografía urbana"
 *                     date: "2025-06-15T18:00:00.000Z"
 *                     location: "Plaza Mayor, Ciudad de México"
 *                     organizerId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                     createdAt: "2025-05-01T09:00:00.000Z"
 *                     updatedAt: "2025-05-01T09:00:00.000Z"
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No autenticado"
 *       404:
 *         description: Usuario no encontrado en la base de datos
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Usuario no encontrado"
 */
eventRouter.get('/my-events', verifyFirebaseToken, attachDbUser, getUserEvents);

/**
 * @openapi
 * /api/events/{id}:
 *   get:
 *     tags: [Events]
 *     summary: Obtener un evento por ID
 *     description: Devuelve el detalle completo de un evento, incluyendo organizador, asistentes y reseñas.
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: ID del evento
 *         example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *     responses:
 *       200:
 *         description: Detalle del evento
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 event:
 *                   $ref: '#/components/schemas/EventWithRelations'
 *             example:
 *               success: true
 *               event:
 *                 id: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                 title: "Taller de fotografía urbana"
 *                 description: "Aprende técnicas de fotografía callejera con fotógrafos locales."
 *                 date: "2025-06-15T18:00:00.000Z"
 *                 location: "Plaza Mayor, Ciudad de México"
 *                 organizerId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                 createdAt: "2025-05-01T09:00:00.000Z"
 *                 updatedAt: "2025-05-10T11:00:00.000Z"
 *                 organizer:
 *                   id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   displayName: "Ana García"
 *                   email: "ana@example.com"
 *                 attendances:
 *                   - id: "c3d4e5f6-a7b8-9012-cdef-123456789012"
 *                     userId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                     eventId: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                     status: "confirmed"
 *                     createdAt: "2025-05-15T08:00:00.000Z"
 *                     user:
 *                       id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                       displayName: "Ana García"
 *                 reviews: []
 *       400:
 *         description: ID no proporcionado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "ID de evento requerido"
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No token provided"
 *       404:
 *         description: Evento no encontrado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Evento no encontrado"
 *
 *   put:
 *     tags: [Events]
 *     summary: Actualizar un evento
 *     description: >
 *       Actualiza los datos de un evento. Solo el organizador original puede
 *       modificar el evento; cualquier otro usuario recibirá un error 403.
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: ID del evento a actualizar
 *         example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [title, date, location]
 *             properties:
 *               title:
 *                 type: string
 *               description:
 *                 type: string
 *                 nullable: true
 *               date:
 *                 type: string
 *                 format: date-time
 *               location:
 *                 type: string
 *           example:
 *             title: "Taller de fotografía urbana (actualizado)"
 *             description: "Sesión ampliada con práctica en exteriores."
 *             date: "2025-06-16T18:00:00.000Z"
 *             location: "Parque Lincoln, Ciudad de México"
 *     responses:
 *       200:
 *         description: Evento actualizado
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 event:
 *                   $ref: '#/components/schemas/Event'
 *             example:
 *               success: true
 *               event:
 *                 id: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                 title: "Taller de fotografía urbana (actualizado)"
 *                 description: "Sesión ampliada con práctica en exteriores."
 *                 date: "2025-06-16T18:00:00.000Z"
 *                 location: "Parque Lincoln, Ciudad de México"
 *                 organizerId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                 createdAt: "2025-05-01T09:00:00.000Z"
 *                 updatedAt: "2025-05-20T15:00:00.000Z"
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No autenticado"
 *       403:
 *         description: El usuario no es el organizador del evento
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Solo el organizador puede editar el evento"
 *       404:
 *         description: Evento no encontrado
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Evento no encontrado"
 */
eventRouter.get('/:id', verifyFirebaseToken, getEventById);

eventRouter.post('/', verifyFirebaseToken, attachDbUser, createEvent);

eventRouter.put('/:id', verifyFirebaseToken, attachDbUser, updateEvent);

/**
 * @openapi
 * /api/events/{id}/attend:
 *   post:
 *     tags: [Events]
 *     summary: Confirmar asistencia a un evento (RSVP)
 *     description: >
 *       Confirma la asistencia del usuario autenticado al evento. Si ya existía
 *       un registro (por ejemplo, cancelado previamente), lo actualiza a "confirmed".
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: ID del evento
 *         example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *     responses:
 *       200:
 *         description: Asistencia confirmada
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 attendance:
 *                   $ref: '#/components/schemas/EventAttendance'
 *             example:
 *               success: true
 *               attendance:
 *                 id: "c3d4e5f6-a7b8-9012-cdef-123456789012"
 *                 userId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                 eventId: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                 status: "confirmed"
 *                 createdAt: "2025-05-15T08:00:00.000Z"
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No autenticado"
 *       404:
 *         description: Usuario no encontrado en la base de datos
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Usuario no encontrado"
 *
 * /api/events/{id}/cancel:
 *   post:
 *     tags: [Events]
 *     summary: Cancelar asistencia a un evento
 *     description: Actualiza el estado de la asistencia del usuario a "cancelled". Requiere que el usuario haya confirmado asistencia previamente.
 *     parameters:
 *       - in: path
 *         name: id
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: ID del evento
 *         example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *     responses:
 *       200:
 *         description: Asistencia cancelada
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 attendance:
 *                   $ref: '#/components/schemas/EventAttendance'
 *             example:
 *               success: true
 *               attendance:
 *                 id: "c3d4e5f6-a7b8-9012-cdef-123456789012"
 *                 userId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                 eventId: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                 status: "cancelled"
 *                 createdAt: "2025-05-15T08:00:00.000Z"
 *       401:
 *         description: Token no proporcionado o inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "No autenticado"
 *       404:
 *         description: Usuario no encontrado en la base de datos
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Usuario no encontrado"
 */
eventRouter.post('/:id/attend', verifyFirebaseToken, attachDbUser, attendEvent);
eventRouter.post('/:id/cancel', verifyFirebaseToken, attachDbUser, cancelAttendance);

export default eventRouter;
