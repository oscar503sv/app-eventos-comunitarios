import { Request, Response } from 'express';
import { prisma } from '../config/prisma.js';

// Crear review
export const createReview = async (req: Request, res: Response) => {
  try {
    const { eventId } = req.params;
    const { rating, comment } = req.body;
    const user = req.dbUser!;

    if (!eventId) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    if (rating < 1 || rating > 5) {
      res.status(400).json({ error: 'Rating debe ser entre 1 y 5' });
      return;
    }

    const review = await prisma.review.upsert({
      where: { userId_eventId: { userId: user.id, eventId } },
      update: { rating, comment: comment ?? null },
      create: { userId: user.id, eventId, rating, comment: comment ?? null },
    });

    res.status(201).json({ success: true, review });
  } catch (error) {
    console.error('Error creating review:', error);
    res.status(500).json({ error: 'Error al crear review' });
  }
};

// Obtener reviews de un evento
export const getEventReviews = async (req: Request, res: Response) => {
  try {
    const { eventId } = req.params;

    if (!eventId) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    const reviews = await prisma.review.findMany({
      where: { eventId },
      include: { user: { select: { id: true, displayName: true } } },
      orderBy: { createdAt: 'desc' },
    });

    const stats = await prisma.review.aggregate({
      where: { eventId },
      _avg: { rating: true },
      _count: true,
    });

    res.json({
      success: true,
      reviews,
      stats: {
        average: stats._avg?.rating || 0,
        count: stats._count || 0,
      },
    });
  } catch (error) {
    console.error('Error getting reviews:', error);
    res.status(500).json({ error: 'Error al obtener reviews' });
  }
};
