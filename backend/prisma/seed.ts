import { PrismaClient, EventCategory } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  // Limpiar datos existentes (excepto usuarios reales)
  await prisma.review.deleteMany();
  await prisma.eventAttendance.deleteMany();
  await prisma.event.deleteMany();

  // Buscar si ya existe un usuario, si no crear uno de prueba
  let user = await prisma.user.findFirst();

  if (!user) {
    user = await prisma.user.create({
      data: {
        firebaseUid: 'test-uid-001',
        email: 'organizador@test.com',
        displayName: 'Juan Organizador',
      },
    });
  }

  const eventsData = [
    // === 4 eventos pasados ===
    {
      title: 'Plantación de Árboles',
      description: 'Jornada comunitaria de reforestación en la zona verde del este.',
      date: new Date('2025-12-10T09:00:00'),
      location: 'Zona Verde Este',
      category: EventCategory.SALUD,
    },
    {
      title: 'Concierto Barrial de Navidad',
      description: 'Bandas locales celebrando la temporada navideña al aire libre.',
      date: new Date('2025-12-21T19:00:00'),
      location: 'Plaza del Vecindario',
      category: EventCategory.MUSICA,
    },
    {
      title: 'Torneo Relámpago de Fútbol',
      description: 'Equipos de la colonia compitiendo en formato 5 vs 5.',
      date: new Date('2026-02-14T10:00:00'),
      location: 'Cancha Municipal #3',
      category: EventCategory.DEPORTE,
    },
    {
      title: 'Feria Gastronómica de Primavera',
      description: 'Degustación de platillos típicos preparados por vecinos del barrio.',
      date: new Date('2026-04-05T12:00:00'),
      location: 'Plaza del Vecindario',
      category: EventCategory.GASTRONOMIA,
    },

    // === 6 eventos futuros ===
    {
      title: 'Taller de Pintura al Óleo',
      description: 'Aprende técnicas básicas de pintura al óleo con artistas locales.',
      date: new Date('2026-06-08T17:00:00'),
      location: 'Casa de la Cultura',
      category: EventCategory.CULTURA,
    },
    {
      title: 'Clase Abierta de Yoga',
      description: 'Sesión de yoga al aire libre para todos los niveles. Trae tu propio tapete.',
      date: new Date('2026-06-22T07:30:00'),
      location: 'Parque Central',
      category: EventCategory.SALUD,
    },
    {
      title: 'Maratón Comunitario 10K',
      description: 'Carrera de 10 kilómetros organizada por la asociación de vecinos.',
      date: new Date('2026-07-19T06:30:00'),
      location: 'Avenida Principal',
      category: EventCategory.DEPORTE,
    },
    {
      title: 'Programación para Niños con Scratch',
      description: 'Curso introductorio de programación visual para niños de 8 a 12 años.',
      date: new Date('2026-09-12T15:00:00'),
      location: 'Biblioteca Comunitaria',
      category: EventCategory.EDUCACION,
    },
    {
      title: 'Festival Cultural de Otoño',
      description: 'Música, danza y exposiciones de artistas del barrio durante todo el día.',
      date: new Date('2026-10-17T11:00:00'),
      location: 'Plaza Mayor',
      category: EventCategory.CULTURA,
    },
    {
      title: 'Asamblea Vecinal Anual',
      description: 'Reunión abierta para tratar temas pendientes del barrio.',
      date: new Date('2026-11-15T18:00:00'),
      location: 'Centro Comunitario Norte',
      category: EventCategory.OTRO,
    },
  ];

  const createdEvents = [];
  for (const data of eventsData) {
    const event = await prisma.event.create({
      data: {
        ...data,
        organizerId: user.id,
      },
    });
    createdEvents.push(event);
  }

  // Asistencias: el usuario asiste a un evento pasado y a uno futuro
  await prisma.eventAttendance.create({
    data: {
      userId: user.id,
      eventId: createdEvents[0]!.id,
      status: 'confirmed',
    },
  });

  await prisma.eventAttendance.create({
    data: {
      userId: user.id,
      eventId: createdEvents[4]!.id,
      status: 'confirmed',
    },
  });

  // Review para el primer evento pasado (Plantación)
  await prisma.review.create({
    data: {
      userId: user.id,
      eventId: createdEvents[0]!.id,
      rating: 5,
      comment: 'Excelente jornada, muy bien organizada!',
    },
  });

  console.log('✅ Datos de prueba creados:');
  console.log(`   - ${createdEvents.length} eventos (4 pasados, 6 futuros)`);
  console.log(`   - 2 asistencias`);
  console.log(`   - 1 review`);
  console.log(`\nIDs de eventos para probar:`);
  for (const event of createdEvents) {
    console.log(`   - [${event.category}] ${event.title}: ${event.id}`);
  }
}

main()
  .catch((e) => {
    console.error('Error:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
