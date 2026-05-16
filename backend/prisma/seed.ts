import { PrismaClient } from '@prisma/client';

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

  // Crear eventos de prueba
  const event1 = await prisma.event.create({
    data: {
      title: 'Limpieza del Parque Central',
      description: 'Jornada de limpieza comunitaria en el parque central. Traer guantes y bolsas.',
      date: new Date('2025-12-15T09:00:00'),
      location: 'Parque Central, Calle Principal #123',
      organizerId: user.id,
    },
  });

  const event2 = await prisma.event.create({
    data: {
      title: 'Taller de Reciclaje',
      description: 'Aprende técnicas de reciclaje y reutilización de materiales.',
      date: new Date('2025-12-20T14:00:00'),
      location: 'Centro Comunitario Norte',
      organizerId: user.id,
    },
  });

  const event3 = await prisma.event.create({
    data: {
      title: 'Feria de Comida Local',
      description: 'Degustación de platillos típicos preparados por vecinos del barrio.',
      date: new Date('2025-12-25T12:00:00'),
      location: 'Plaza del Vecindario',
      organizerId: user.id,
    },
  });

  // Evento pasado con review
  const pastEvent = await prisma.event.create({
    data: {
      title: 'Plantación de Árboles (Pasado)',
      description: 'Evento ya realizado.',
      date: new Date('2025-10-01T10:00:00'),
      location: 'Zona Verde Este',
      organizerId: user.id,
    },
  });

  // Crear asistencias
  await prisma.eventAttendance.create({
    data: {
      userId: user.id,
      eventId: event1.id,
      status: 'confirmed',
    },
  });

  await prisma.eventAttendance.create({
    data: {
      userId: user.id,
      eventId: pastEvent.id,
      status: 'confirmed',
    },
  });

  // Crear review para evento pasado
  await prisma.review.create({
    data: {
      userId: user.id,
      eventId: pastEvent.id,
      rating: 5,
      comment: 'Excelente evento, muy bien organizado!',
    },
  });

  console.log('✅ Datos de prueba creados:');
  console.log(`   - 4 eventos`);
  console.log(`   - 2 asistencias`);
  console.log(`   - 1 review`);
  console.log(`\nIDs de eventos para probar:`);
  console.log(`   - ${event1.id}`);
  console.log(`   - ${event2.id}`);
  console.log(`   - ${event3.id}`);
  console.log(`   - ${pastEvent.id}`);
}

main()
  .catch((e) => {
    console.error('Error:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
