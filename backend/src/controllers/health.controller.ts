import { Request, Response } from 'express';
import { readFileSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';
import { prisma } from '../config/prisma.js';

const __dirname = dirname(fileURLToPath(import.meta.url));
const pkg = JSON.parse(
  readFileSync(join(__dirname, '../../package.json'), 'utf-8'),
) as { version: string };

export const getHealth = async (_req: Request, res: Response) => {
  const base = {
    uptime: process.uptime(),
    timestamp: new Date().toISOString(),
    version: pkg.version,
    environment: process.env['NODE_ENV'] ?? 'development',
  };

  try {
    await prisma.$queryRaw`SELECT 1`;
    res.json({
      status: 'ok',
      ...base,
      services: { database: 'ok' },
    });
  } catch (error) {
    console.error('Health check: database error:', error);
    res.status(503).json({
      status: 'degraded',
      ...base,
      services: { database: 'error' },
    });
  }
};
