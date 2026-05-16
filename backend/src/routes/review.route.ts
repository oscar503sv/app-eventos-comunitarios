import { Router } from 'express';
import { verifyFirebaseToken, attachDbUser } from '../middleware/auth.middleware.js';
import { createReview, getEventReviews } from '../controllers/review.controller.js';

const reviewRouter = Router();

/**
 * @openapi
 * /api/reviews/{eventId}:
 *   get:
 *     tags: [Reviews]
 *     summary: Obtener reseñas de un evento
 *     description: Devuelve todas las reseñas de un evento junto con estadísticas agregadas (promedio y conteo).
 *     parameters:
 *       - in: path
 *         name: eventId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: ID del evento
 *         example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *     responses:
 *       200:
 *         description: Lista de reseñas y estadísticas
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 reviews:
 *                   type: array
 *                   items:
 *                     allOf:
 *                       - $ref: '#/components/schemas/Review'
 *                       - type: object
 *                         properties:
 *                           user:
 *                             $ref: '#/components/schemas/UserPublic'
 *                 stats:
 *                   $ref: '#/components/schemas/ReviewStats'
 *             example:
 *               success: true
 *               reviews:
 *                 - id: "d4e5f6a7-b8c9-0123-def0-234567890123"
 *                   userId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   eventId: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                   rating: 4
 *                   comment: "Excelente taller, muy bien organizado."
 *                   createdAt: "2025-06-16T10:00:00.000Z"
 *                   user:
 *                     id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                     displayName: "Ana García"
 *               stats:
 *                 average: 4.2
 *                 count: 8
 *       400:
 *         description: ID de evento no proporcionado
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
 *
 *   post:
 *     tags: [Reviews]
 *     summary: Crear o actualizar una reseña
 *     description: >
 *       Crea una nueva reseña del usuario autenticado para el evento indicado.
 *       Si el usuario ya dejó una reseña para este evento, la actualiza (upsert).
 *       Solo se puede dejar una reseña por usuario por evento.
 *     parameters:
 *       - in: path
 *         name: eventId
 *         required: true
 *         schema:
 *           type: string
 *           format: uuid
 *         description: ID del evento a reseñar
 *         example: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [rating]
 *             properties:
 *               rating:
 *                 type: integer
 *                 minimum: 1
 *                 maximum: 5
 *                 description: Calificación del evento entre 1 y 5
 *               comment:
 *                 type: string
 *                 nullable: true
 *                 description: Comentario opcional sobre el evento
 *           example:
 *             rating: 4
 *             comment: "Excelente taller, muy bien organizado."
 *     responses:
 *       201:
 *         description: Reseña creada o actualizada
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 review:
 *                   $ref: '#/components/schemas/Review'
 *             example:
 *               success: true
 *               review:
 *                 id: "d4e5f6a7-b8c9-0123-def0-234567890123"
 *                 userId: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                 eventId: "b2c3d4e5-f6a7-8901-bcde-f12345678901"
 *                 rating: 4
 *                 comment: "Excelente taller, muy bien organizado."
 *                 createdAt: "2025-06-16T10:00:00.000Z"
 *       400:
 *         description: Rating fuera de rango o ID de evento faltante
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Rating debe ser entre 1 y 5"
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
reviewRouter.get('/:eventId', verifyFirebaseToken, getEventReviews);

reviewRouter.post('/:eventId', verifyFirebaseToken, attachDbUser, createReview);

export default reviewRouter;
