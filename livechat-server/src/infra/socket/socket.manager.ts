import { Server as SocketIOServer } from "socket.io";
import http from "http";
import https from "https";
import { serverConfig } from "../server/server.config";
import { setupSocketHandlers } from "./socket.handler";
import { realtimeAccess } from "../../domain/auth/access";
import { logger } from "../../utils/logger";

export class SocketManager {
  public io: SocketIOServer;

  constructor(server: http.Server | https.Server) {
    this.io = new SocketIOServer(server, {
      cors: serverConfig.cors,
      // Browser WebSocket handshakes also need an origin check; HTTP CORS
      // headers alone only cover polling. This is not participant authentication.
      allowRequest: (request, callback) => {
        const origin = request.headers.origin;
        callback(null, !origin || serverConfig.cors.origin.includes(origin));
      },
    });
    this.io.use((socket, next) => { void realtimeAccess.authenticate(socket, next); });
    setupSocketHandlers(this.io);
    logger.info("Socket.IO server configured");
  }

  close(): void {
    this.io.close();
  }
}
