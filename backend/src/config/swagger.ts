import swaggerJsdoc from 'swagger-jsdoc';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __dirname = dirname(fileURLToPath(import.meta.url));

const options: swaggerJsdoc.Options = {
  definition: {
    openapi: '3.0.3',
    info: {
      title: 'Eventos Comunitarios API',
      version: '0.0.1',
      description:
        'API REST para gestionar eventos comunitarios. Permite crear eventos, confirmar asistencia y dejar reseñas. Todos los endpoints requieren autenticación mediante Firebase ID token.',
      license: {
        name: 'CC-BY-4.0',
        url: 'https://creativecommons.org/licenses/by/4.0/',
      },
    },
    servers: [
      {
        url: `http://localhost:${process.env['PORT'] ?? 3000}`,
        description: 'Servidor local de desarrollo',
      },
      ...(process.env['API_PUBLIC_URL']
        ? [{ url: process.env['API_PUBLIC_URL'], description: 'Servidor de producción' }]
        : []),
    ],
    tags: [
      { name: 'Events', description: 'Creación, consulta, actualización y asistencia a eventos' },
      { name: 'Reviews', description: 'Reseñas y calificaciones de eventos' },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'Firebase ID Token',
          description:
            'Firebase ID token obtenido tras autenticar al usuario con Firebase Auth. Se obtiene llamando a `getIdToken()` en el SDK de Firebase en el cliente.',
        },
      },
    },
    security: [{ bearerAuth: [] }],
  },
  apis: [
    join(__dirname, '../routes/*.js'),
    join(__dirname, '../docs/*.js'),
    join(__dirname, '../routes/*.ts'),
    join(__dirname, '../docs/*.ts'),
  ],
};

export const swaggerSpec = swaggerJsdoc(options);
