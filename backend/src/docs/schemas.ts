/**
 * @openapi
 * components:
 *   schemas:
 *     UserPublic:
 *       type: object
 *       description: Versión reducida del usuario, usada en relaciones (organizer, attendances, reviews)
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *         displayName:
 *           type: string
 *           nullable: true
 *           example: "Ana García"
 *         email:
 *           type: string
 *           format: email
 *           example: "ana@example.com"
 *       required: [id]
 *
 *     User:
 *       type: object
 *       description: Usuario sincronizado en la base de datos
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *         firebaseUid:
 *           type: string
 *           example: "firebase_uid_abc123"
 *         email:
 *           type: string
 *           format: email
 *           example: "ana@example.com"
 *         displayName:
 *           type: string
 *           nullable: true
 *           example: "Ana García"
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2025-01-15T10:30:00.000Z"
 *         updatedAt:
 *           type: string
 *           format: date-time
 *           example: "2025-03-20T14:00:00.000Z"
 *       required: [id, firebaseUid, email, createdAt, updatedAt]
 *
 *     Event:
 *       type: object
 *       description: Evento comunitario
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *         title:
 *           type: string
 *           example: "Taller de fotografía urbana"
 *         description:
 *           type: string
 *           nullable: true
 *           example: "Aprende técnicas de fotografía callejera con fotógrafos locales."
 *         date:
 *           type: string
 *           format: date-time
 *           example: "2025-06-15T18:00:00.000Z"
 *         location:
 *           type: string
 *           example: "Plaza Mayor, Ciudad de México"
 *         organizerId:
 *           type: string
 *           format: uuid
 *           example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2025-05-01T09:00:00.000Z"
 *         updatedAt:
 *           type: string
 *           format: date-time
 *           example: "2025-05-10T11:00:00.000Z"
 *       required: [id, title, date, location, organizerId, createdAt, updatedAt]
 *
 *     EventWithRelations:
 *       allOf:
 *         - $ref: '#/components/schemas/Event'
 *         - type: object
 *           properties:
 *             organizer:
 *               $ref: '#/components/schemas/UserPublic'
 *             attendances:
 *               type: array
 *               items:
 *                 allOf:
 *                   - $ref: '#/components/schemas/EventAttendance'
 *                   - type: object
 *                     properties:
 *                       user:
 *                         $ref: '#/components/schemas/UserPublic'
 *             reviews:
 *               type: array
 *               items:
 *                 allOf:
 *                   - $ref: '#/components/schemas/Review'
 *                   - type: object
 *                     properties:
 *                       user:
 *                         $ref: '#/components/schemas/UserPublic'
 *
 *     EventSummary:
 *       allOf:
 *         - $ref: '#/components/schemas/Event'
 *         - type: object
 *           properties:
 *             organizer:
 *               $ref: '#/components/schemas/UserPublic'
 *             _count:
 *               type: object
 *               properties:
 *                 attendances:
 *                   type: integer
 *                   example: 12
 *                 reviews:
 *                   type: integer
 *                   example: 4
 *
 *     EventAttendance:
 *       type: object
 *       description: Registro de asistencia de un usuario a un evento
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "c3d4e5f6-a7b8-9012-cdef-123456789012"
 *         userId:
 *           type: string
 *           format: uuid
 *           example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *         eventId:
 *           type: string
 *           format: uuid
 *           example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *         status:
 *           type: string
 *           enum: [confirmed, cancelled]
 *           example: "confirmed"
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2025-05-15T08:00:00.000Z"
 *       required: [id, userId, eventId, status, createdAt]
 *
 *     Review:
 *       type: object
 *       description: Reseña de un usuario sobre un evento
 *       properties:
 *         id:
 *           type: string
 *           format: uuid
 *           example: "d4e5f6a7-b8c9-0123-def0-234567890123"
 *         userId:
 *           type: string
 *           format: uuid
 *           example: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *         eventId:
 *           type: string
 *           format: uuid
 *           example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *         rating:
 *           type: integer
 *           minimum: 1
 *           maximum: 5
 *           example: 4
 *         comment:
 *           type: string
 *           nullable: true
 *           example: "Excelente taller, muy bien organizado."
 *         createdAt:
 *           type: string
 *           format: date-time
 *           example: "2025-06-16T10:00:00.000Z"
 *       required: [id, userId, eventId, rating, createdAt]
 *
 *     ReviewStats:
 *       type: object
 *       description: Estadísticas agregadas de reseñas de un evento
 *       properties:
 *         average:
 *           type: number
 *           format: float
 *           example: 4.2
 *         count:
 *           type: integer
 *           example: 8
 *       required: [average, count]
 *
 *     ApiError:
 *       type: object
 *       description: Respuesta de error estándar
 *       properties:
 *         error:
 *           type: string
 *           example: "Evento no encontrado"
 *       required: [error]
 */

export {};
