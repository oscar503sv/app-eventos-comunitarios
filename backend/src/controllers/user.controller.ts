import { Request, Response } from 'express';
import type { User } from '@prisma/client';
import { prisma } from '../config/prisma.js';
import admin from '../config/firebase.js';

const DISPLAY_NAME_MAX = 80;

const buildProfile = async (user: User) => {
  const [organized, attended, reviews] = await prisma.$transaction([
    prisma.event.count({ where: { organizerId: user.id } }),
    prisma.eventAttendance.count({ where: { userId: user.id, status: 'confirmed' } }),
    prisma.review.count({ where: { userId: user.id } }),
  ]);

  return {
    user: {
      id: user.id,
      firebaseUid: user.firebaseUid,
      email: user.email,
      displayName: user.displayName,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt,
    },
    counts: { organized, attended, reviews },
  };
};

// Perfil del usuario autenticado con conteos agregados
export const getProfile = async (req: Request, res: Response) => {
  try {
    const profile = await buildProfile(req.dbUser!);
    res.json({ success: true, profile });
  } catch (error) {
    console.error('Error getting user profile:', error);
    res.status(500).json({ error: 'Error al obtener perfil' });
  }
};

// Actualizar perfil del usuario autenticado (displayName)
export const updateProfile = async (req: Request, res: Response) => {
  try {
    const user = req.dbUser!;
    const { displayName } = req.body;

    if (typeof displayName !== 'string') {
      res.status(400).json({ error: '"displayName" es requerido y debe ser un string' });
      return;
    }

    const trimmed = displayName.trim();
    if (trimmed.length < 1 || trimmed.length > DISPLAY_NAME_MAX) {
      res.status(400).json({
        error: `"displayName" debe tener entre 1 y ${DISPLAY_NAME_MAX} caracteres`,
      });
      return;
    }

    // Sincronizar primero con Firebase; si falla, abortamos para evitar drift.
    // Si BD falla después, el middleware attachDbUser reconciliará en el próximo request.
    await admin.auth().updateUser(user.firebaseUid, { displayName: trimmed });

    const updated = await prisma.user.update({
      where: { id: user.id },
      data: { displayName: trimmed },
    });

    const profile = await buildProfile(updated);
    res.json({ success: true, profile });
  } catch (error) {
    console.error('Error updating user profile:', error);
    res.status(500).json({ error: 'Error al actualizar perfil' });
  }
};
