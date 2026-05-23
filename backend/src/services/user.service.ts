import { prisma } from '../config/prisma.js';
import type { User } from '@prisma/client';

interface FirebaseUserData {
  uid: string;
  email: string | undefined;
  displayName: string | undefined;
}

export const resolveOrCreateUser = async ({ uid, email, displayName }: FirebaseUserData): Promise<User> => {
  const dbUser = await prisma.user.findUnique({ where: { firebaseUid: uid } });

  if (dbUser) {
    // displayName es editable por el usuario vía PUT /api/users/profile, así que la BD es
    // la fuente de verdad: solo se inicializa desde el token si la BD aún no tiene valor.
    // Sin esto, el token de Firebase (que cachea claims por ~1h) revertiría los cambios
    // de displayName en cada request hasta que el token caducara.
    return prisma.user.update({
      where: { firebaseUid: uid },
      data: {
        email: email || dbUser.email,
        displayName: dbUser.displayName ?? displayName ?? null,
      },
    });
  }

  const existingByEmail = await prisma.user.findUnique({ where: { email: email || '' } });

  if (existingByEmail) {
    return prisma.user.update({
      where: { email: email || '' },
      data: {
        firebaseUid: uid,
        displayName: existingByEmail.displayName ?? displayName ?? null,
      },
    });
  }

  return prisma.user.create({
    data: {
      firebaseUid: uid,
      email: email || '',
      displayName: displayName ?? null,
    },
  });
};
