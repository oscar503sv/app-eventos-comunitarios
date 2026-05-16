import { prisma } from '../config/prisma.js';
import type { User } from '@prisma/client';

interface FirebaseUserData {
  uid: string;
  email: string | undefined;
  displayName: string | undefined;
}

export const resolveOrCreateUser = async ({ uid, email, displayName }: FirebaseUserData): Promise<User> => {
  let dbUser = await prisma.user.findUnique({ where: { firebaseUid: uid } });

  if (dbUser) {
    return prisma.user.update({
      where: { firebaseUid: uid },
      data: {
        email: email || dbUser.email,
        displayName: displayName ?? dbUser.displayName,
      },
    });
  }

  const existingByEmail = await prisma.user.findUnique({ where: { email: email || '' } });

  if (existingByEmail) {
    return prisma.user.update({
      where: { email: email || '' },
      data: {
        firebaseUid: uid,
        displayName: displayName ?? existingByEmail.displayName,
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
