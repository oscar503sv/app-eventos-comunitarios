import express, { Request, Response } from "express";
import cors from "cors";
import dotenv from "dotenv";
import swaggerUi from "swagger-ui-express";
import eventRouter from "./routes/event.route.js";
import reviewRouter from "./routes/review.route.js";
import healthRouter from "./routes/health.route.js";
import { swaggerSpec } from "./config/swagger.js";

dotenv.config();

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// Swagger UI
app.use('/api-docs', swaggerUi.serve, swaggerUi.setup(swaggerSpec));
app.get('/api-docs.json', (_req: Request, res: Response) => { res.json(swaggerSpec); });

// Routes
app.use('/api/events', eventRouter);
app.use('/api/reviews', reviewRouter);
app.use('/health', healthRouter);

app.get("/", (req: Request, res: Response) => {
  res.json({ message: "API con TypeScript funcionando 🚀" });
});

app.listen(PORT, () => {
  console.log(`🚀 Servidor corriendo en http://localhost:${PORT}`);
});
