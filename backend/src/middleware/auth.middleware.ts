import { Request, Response, NextFunction } from 'express';
import type { User as PrismaUser } from '@prisma/client';
import admin from '../config/firebase.js';
import { resolveOrCreateUser } from '../services/user.service.js';

interface AuthUser {
  uid: string;
  email: string | undefined;
  displayName: string | undefined;
}

declare module 'express-serve-static-core' {
  interface Request {
    user?: AuthUser;
    dbUser?: PrismaUser;
  }
}

export const verifyFirebaseToken = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  try {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      res.status(401).json({ error: 'No token provided' });
      return;
    }

    const token = authHeader.split('Bearer ')[1];
    
    if (!token) {
      res.status(401).json({ error: 'Token malformed' });
      return;
    }

    const decodedToken = await admin.auth().verifyIdToken(token);
    
    req.user = {
      uid: decodedToken.uid,
      email: decodedToken.email ?? undefined,
      displayName: decodedToken.name ?? undefined,
    };

    next();
  } catch (error) {
    console.error('Token verification error:', error);
    res.status(401).json({ error: 'Invalid or expired token' });
    return;
  }
};

export const attachDbUser = async (
  req: Request,
  res: Response,
  next: NextFunction
): Promise<void> => {
  if (!req.user) {
    res.status(401).json({ error: 'No autenticado' });
    return;
  }
  try {
    req.dbUser = await resolveOrCreateUser(req.user);
    next();
  } catch (error) {
    console.error('Error resolving DB user:', error);
    res.status(500).json({ error: 'Error al resolver usuario' });
  }
};
