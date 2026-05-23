import { Router } from 'express';
import { verifyFirebaseToken, attachDbUser } from '../middleware/auth.middleware.js';
import { getProfile, updateProfile } from '../controllers/user.controller.js';

const userRouter = Router();

/**
 * @openapi
 * /api/users/profile:
 *   get:
 *     tags: [Users]
 *     summary: Perfil del usuario autenticado
 *     description: >
 *       Devuelve los datos básicos del usuario autenticado junto con conteos agregados:
 *       eventos organizados, asistencias confirmadas y reseñas escritas.
 *     responses:
 *       200:
 *         description: Perfil con conteos
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 profile:
 *                   $ref: '#/components/schemas/UserProfile'
 *             example:
 *               success: true
 *               profile:
 *                 user:
 *                   id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   firebaseUid: "firebase_uid_abc123"
 *                   email: "ana@example.com"
 *                   displayName: "Ana García"
 *                   createdAt: "2025-01-15T10:30:00.000Z"
 *                   updatedAt: "2025-03-20T14:00:00.000Z"
 *                 counts:
 *                   organized: 5
 *                   attended: 12
 *                   reviews: 3
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
userRouter.get('/profile', verifyFirebaseToken, attachDbUser, getProfile);

/**
 * @openapi
 * /api/users/profile:
 *   put:
 *     tags: [Users]
 *     summary: Actualizar perfil del usuario autenticado
 *     description: >
 *       Actualiza el `displayName` del usuario tanto en la base de datos como en
 *       Firebase Auth para mantenerlos sincronizados. Devuelve el perfil actualizado
 *       en el mismo formato que `GET /api/users/profile`.
 *     requestBody:
 *       required: true
 *       content:
 *         application/json:
 *           schema:
 *             type: object
 *             required: [displayName]
 *             properties:
 *               displayName:
 *                 type: string
 *                 minLength: 1
 *                 maxLength: 80
 *                 description: Nuevo nombre del usuario. Se trimean los espacios extremos.
 *           example:
 *             displayName: "Ana García López"
 *     responses:
 *       200:
 *         description: Perfil actualizado
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 success:
 *                   type: boolean
 *                 profile:
 *                   $ref: '#/components/schemas/UserProfile'
 *             example:
 *               success: true
 *               profile:
 *                 user:
 *                   id: "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
 *                   firebaseUid: "firebase_uid_abc123"
 *                   email: "ana@example.com"
 *                   displayName: "Ana García López"
 *                   createdAt: "2025-01-15T10:30:00.000Z"
 *                   updatedAt: "2026-05-22T16:00:00.000Z"
 *                 counts:
 *                   organized: 5
 *                   attended: 12
 *                   reviews: 3
 *       400:
 *         description: Body inválido
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "\"displayName\" debe tener entre 1 y 80 caracteres"
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
 *       500:
 *         description: Error al sincronizar con Firebase o actualizar la BD
 *         content:
 *           application/json:
 *             schema:
 *               $ref: '#/components/schemas/ApiError'
 *             example:
 *               error: "Error al actualizar perfil"
 */
userRouter.put('/profile', verifyFirebaseToken, attachDbUser, updateProfile);

export default userRouter;
