import { Request, Response } from 'express';
import { EventCategory } from '@prisma/client';
import { prisma } from '../config/prisma.js';

const ALLOWED_CATEGORIES = Object.values(EventCategory);

const parseCategory = (value: unknown): EventCategory | { error: string } | null => {
  if (value === undefined || value === null || value === '') return null;
  if (typeof value !== 'string' || !ALLOWED_CATEGORIES.includes(value as EventCategory)) {
    return { error: `Categoría inválida. Valores permitidos: ${ALLOWED_CATEGORIES.join(', ')}` };
  }
  return value as EventCategory;
};

const DEFAULT_PAGE = 1;
const DEFAULT_LIMIT = 20;
const MAX_LIMIT = 50;

const parsePagination = (
  rawPage: unknown,
  rawLimit: unknown,
): { page: number; limit: number } | { error: string } => {
  const page = rawPage === undefined ? DEFAULT_PAGE : Number(rawPage);
  const limit = rawLimit === undefined ? DEFAULT_LIMIT : Number(rawLimit);

  if (!Number.isInteger(page) || page < 1) {
    return { error: '"page" debe ser un entero mayor o igual a 1' };
  }
  if (!Number.isInteger(limit) || limit < 1 || limit > MAX_LIMIT) {
    return { error: `"limit" debe ser un entero entre 1 y ${MAX_LIMIT}` };
  }
  return { page, limit };
};

// Crear evento
export const createEvent = async (req: Request, res: Response) => {
  try {
    const { title, description, date, location, category } = req.body;
    const user = req.dbUser!;

    const parsed = parseCategory(category);
    if (parsed && typeof parsed === 'object' && 'error' in parsed) {
      res.status(400).json({ error: parsed.error });
      return;
    }

    const event = await prisma.event.create({
      data: {
        title,
        description: description ?? null,
        date: new Date(date),
        location,
        category: parsed ?? EventCategory.OTRO,
        organizerId: user.id,
      },
    });

    res.status(201).json({ success: true, event });
  } catch (error) {
    console.error('Error creating event:', error);
    res.status(500).json({ error: 'Error al crear evento' });
  }
};

// Obtener todos los eventos (paginado)
export const getEvents = async (req: Request, res: Response) => {
  try {
    const pagination = parsePagination(req.query.page, req.query.limit);
    if ('error' in pagination) {
      res.status(400).json({ error: pagination.error });
      return;
    }
    const { page, limit } = pagination;

    const [events, total] = await prisma.$transaction([
      prisma.event.findMany({
        include: {
          organizer: { select: { id: true, displayName: true, email: true } },
          _count: { select: { attendances: true, reviews: true } },
        },
        orderBy: { date: 'asc' },
        skip: (page - 1) * limit,
        take: limit,
      }),
      prisma.event.count(),
    ]);

    const totalPages = total === 0 ? 0 : Math.ceil(total / limit);
    const hasMore = page < totalPages;

    res.json({
      success: true,
      events,
      pagination: { page, limit, total, totalPages, hasMore },
    });
  } catch (error) {
    console.error('Error getting events:', error);
    res.status(500).json({ error: 'Error al obtener eventos' });
  }
};

// Obtener un evento por ID
export const getEventById = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;

    if (!id) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    const event = await prisma.event.findUnique({
      where: { id },
      include: {
        organizer: { select: { id: true, firebaseUid: true, displayName: true, email: true } },
        attendances: {
          include: { user: { select: { id: true, firebaseUid: true, displayName: true } } },
        },
        reviews: {
          include: { user: { select: { id: true, firebaseUid: true, displayName: true } } },
        },
      },
    });

    if (!event) {
      res.status(404).json({ error: 'Evento no encontrado' });
      return;
    }

    res.json({ success: true, event });
  } catch (error) {
    console.error('Error getting event:', error);
    res.status(500).json({ error: 'Error al obtener evento' });
  }
};

// Confirmar asistencia (RSVP)
export const attendEvent = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const user = req.dbUser!;

    if (!id) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    const attendance = await prisma.eventAttendance.upsert({
      where: { userId_eventId: { userId: user.id, eventId: id } },
      update: { status: 'confirmed' },
      create: { userId: user.id, eventId: id, status: 'confirmed' },
    });

    res.json({ success: true, attendance });
  } catch (error) {
    console.error('Error attending event:', error);
    res.status(500).json({ error: 'Error al confirmar asistencia' });
  }
};

// Cancelar asistencia
export const cancelAttendance = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const user = req.dbUser!;

    if (!id) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    const attendance = await prisma.eventAttendance.update({
      where: { userId_eventId: { userId: user.id, eventId: id } },
      data: { status: 'cancelled' },
    });

    res.json({ success: true, attendance });
  } catch (error) {
    console.error('Error cancelling attendance:', error);
    res.status(500).json({ error: 'Error al cancelar asistencia' });
  }
};

// Historial de eventos del usuario
export const getUserEvents = async (req: Request, res: Response) => {
  try {
    const user = req.dbUser!;

    const organized = await prisma.event.findMany({
      where: { organizerId: user.id },
      orderBy: { date: 'desc' },
    });

    const attended = await prisma.eventAttendance.findMany({
      where: { userId: user.id },
      include: { event: true },
      orderBy: { createdAt: 'desc' },
    });

    res.json({ success: true, organized, attended });
  } catch (error) {
    console.error('Error getting user events:', error);
    res.status(500).json({ error: 'Error al obtener historial' });
  }
};

// Actualizar evento (solo el organizador puede hacerlo)
export const updateEvent = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const { title, description, date, location, category } = req.body;
    const user = req.dbUser!;

    if (!id) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    const event = await prisma.event.findUnique({ where: { id } });
    if (!event) {
      res.status(404).json({ error: 'Evento no encontrado' });
      return;
    }

    if (event.organizerId !== user.id) {
      res.status(403).json({ error: 'Solo el organizador puede editar el evento' });
      return;
    }

    const parsed = parseCategory(category);
    if (parsed && typeof parsed === 'object' && 'error' in parsed) {
      res.status(400).json({ error: parsed.error });
      return;
    }

    const updatedEvent = await prisma.event.update({
      where: { id },
      data: {
        title,
        description: description ?? null,
        date: new Date(date),
        location,
        ...(parsed ? { category: parsed } : {}),
      },
    });

    res.json({ success: true, event: updatedEvent });
  } catch (error) {
    console.error('Error updating event:', error);
    res.status(500).json({ error: 'Error al actualizar evento' });
  }
};

// Eliminar evento (solo el organizador puede hacerlo)
export const deleteEvent = async (req: Request, res: Response) => {
  try {
    const { id } = req.params;
    const user = req.dbUser!;

    if (!id) {
      res.status(400).json({ error: 'ID de evento requerido' });
      return;
    }

    const event = await prisma.event.findUnique({ where: { id } });
    if (!event) {
      res.status(404).json({ error: 'Evento no encontrado' });
      return;
    }

    if (event.organizerId !== user.id) {
      res.status(403).json({ error: 'Solo el organizador puede eliminar el evento' });
      return;
    }

    await prisma.event.delete({ where: { id } });

    res.json({ success: true, message: 'Evento eliminado' });
  } catch (error) {
    console.error('Error deleting event:', error);
    res.status(500).json({ error: 'Error al eliminar evento' });
  }
};
