
import { portFromEnv } from "../../config/env";

const isProduction = process.env.NODE_ENV === "production";

// 상수 정의
const BASE_URL = process.env.BASE_URL
const DEV_URL = process.env.DEV_URL
const DEV_ORIGIN = process.env.DEV_ORIGIN
const BACKEND_ORIGIN = process.env.BACKEND_ORIGIN
const FRONTEND_VERCEL_ORIGIN = process.env.FRONTEND_VERCEL_ORIGIN
const protocol = process.env.SERVER_PROTOCOL ?? (isProduction ? "http" : "https");
if (protocol !== "http" && protocol !== "https") {
  throw new Error("SERVER_PROTOCOL must be http or https");
}

const allowedOrigins = process.env.SOCKET_ALLOWED_ORIGINS
  ? process.env.SOCKET_ALLOWED_ORIGINS.split(",").map((value) => value.trim()).filter(Boolean)
  : [DEV_ORIGIN, BACKEND_ORIGIN, FRONTEND_VERCEL_ORIGIN, "https://localhost:3000"]
      .filter((url): url is string => !!url);

export const serverConfig = {
  port: portFromEnv("SERVER_PORT", 8443),
  host: process.env.SERVER_HOST ?? (isProduction ? BASE_URL : DEV_URL) ?? "127.0.0.1",
  cors: {
    origin: allowedOrigins,
    credentials: true,
    methods: ["GET", "POST"],
  },
  ssl: protocol === "https"
    ? {
      keyPath: process.env.SERVER_SSL_KEY_PATH ?? "certs/server.key",
      certPath: process.env.SERVER_SSL_CERT_PATH ?? "certs/server.cert",
    }
    : null,
  socket: {
    pingTimeout: 60000,
    pingInterval: 25000
  },
  chat: {
    maxMessageLength: 1000,
    maxUsernameLength: 30,
    minUsernameLength: 2,
    maxRoomNameLength: 50,
    defaultRoom: 'general',
    messageHistoryLimit: 100
  }
};
