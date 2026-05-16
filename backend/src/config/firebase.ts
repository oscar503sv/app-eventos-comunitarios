import admin from 'firebase-admin';
import dotenv from 'dotenv';

dotenv.config();

// Inicializar Firebase Admin SDK
const projectId = process.env.FIREBASE_PROJECT_ID;
const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, '\n');
const clientEmail = process.env.FIREBASE_CLIENT_EMAIL;

if (!projectId || !privateKey || !clientEmail) {
  console.warn('⚠️  Firebase Admin no configurado. Las rutas protegidas no funcionarán.');
} else {
  admin.initializeApp({
    credential: admin.credential.cert({
      projectId,
      privateKey,
      clientEmail,
    }),
  });
  console.log('✅ Firebase Admin inicializado');
}

export default admin;