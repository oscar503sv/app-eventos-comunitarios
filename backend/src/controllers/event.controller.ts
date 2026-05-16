import { Request, Response } from 'express';
import { prisma } from '../config/prisma.js';

// Crear evento
export const createEvent = async (req: Request, res: Response) => {
  try {
    const { title, description, date, location } = req.body;
    const user = req.dbUser!;

    const event = await prisma.event.create({
      data: {
        title,
        description: description ?? null,
        date: new Date(date),
        location,
        organizerId: user.id,
      },
    });

    res.status(201).json({ success: true, event });
  } catch (error) {
    console.error('Error creating event:', error);
    res.status(500).json({ error: 'Error al crear evento' });
  }
};

// Obtener todos los eventos
export const getEvents = async (req: Request, res: Response) => {
  try {
    const events = await prisma.event.findMany({
      include: {
        organizer: { select: { id: true, displayName: true, email: true } },
        _count: { select: { attendances: true, reviews: true } },
      },
      orderBy: { date: 'asc' },
    });

    res.json({ success: true, events });
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
    const { title, description, date, location } = req.body;
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

    const updatedEvent = await prisma.event.update({
      where: { id },
      data: {
        title,
        description: description ?? null,
        date: new Date(date),
        location,
      },
    });

    res.json({ success: true, event: updatedEvent });
  } catch (error) {
    console.error('Error updating event:', error);
    res.status(500).json({ error: 'Error al actualizar evento' });
  }
};
