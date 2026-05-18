import { Router } from 'express';
import { getHealth } from '../controllers/health.controller.js';

const healthRouter = Router();

/**
 * @openapi
 * /health:
 *   get:
 *     tags: [Health]
 *     summary: Estado del servicio y dependencias
 *     description: >
 *       Endpoint público (sin autenticación) que reporta el estado de la API y de
 *       sus dependencias. Pensado para ser consumido por orquestadores
 *       (Docker, Kubernetes, load balancers) y dashboards de monitoreo.
 *     security: []
 *     responses:
 *       200:
 *         description: Servicio sano
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 status:
 *                   type: string
 *                   enum: [ok]
 *                 uptime:
 *                   type: number
 *                   description: Segundos desde que arrancó el proceso
 *                 timestamp:
 *                   type: string
 *                   format: date-time
 *                 version:
 *                   type: string
 *                   description: Versión declarada en package.json
 *                 environment:
 *                   type: string
 *                   description: Valor de NODE_ENV (development por defecto)
 *                 services:
 *                   type: object
 *                   properties:
 *                     database:
 *                       type: string
 *                       enum: [ok]
 *             example:
 *               status: ok
 *               uptime: 1234.56
 *               timestamp: "2026-05-17T12:34:56.789Z"
 *               version: "0.0.1"
 *               environment: development
 *               services:
 *                 database: ok
 *       503:
 *         description: Servicio degradado (alguna dependencia falla)
 *         content:
 *           application/json:
 *             schema:
 *               type: object
 *               properties:
 *                 status:
 *                   type: string
 *                   enum: [degraded]
 *                 uptime:
 *                   type: number
 *                 timestamp:
 *                   type: string
 *                   format: date-time
 *                 version:
 *                   type: string
 *                 environment:
 *                   type: string
 *                 services:
 *                   type: object
 *                   properties:
 *                     database:
 *                       type: string
 *                       enum: [error]
 *             example:
 *               status: degraded
 *               uptime: 1234.56
 *               timestamp: "2026-05-17T12:34:56.789Z"
 *               version: "0.0.1"
 *               environment: development
 *               services:
 *                 database: error
 */
healthRouter.get('/', getHealth);

export default healthRouter;
