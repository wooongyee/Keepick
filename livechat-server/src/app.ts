import express from "express";
import https from "https";
import http from "http"; 
import cors from "cors";

import { serverConfig } from "./infra/server/server.config";
import { ServerManager } from './infra/server/server.manager'
import { SocketManager } from "./infra/socket/socket.manager";
import { mediasoupService } from "./domain/media/mediasoup.service";
import { mediaRouter } from "./routes/media.routes";
import { chatRouter } from "./routes/chat.routes";

import { logger } from "./utils/logger";


class Application {
  private app: express.Application;
  private serverManager: ServerManager;
  private socketManager: SocketManager;

  constructor() {
    this.app = express();
    this.setupMiddleware();
    this.setupRoutes();
    this.serverManager = new ServerManager(this.app);
    this.socketManager = new SocketManager(this.serverManager.server);
  }

  private setupMiddleware() {
    this.app.use(cors(serverConfig.cors));
    this.app.use(express.json());
  }

  private setupRoutes() {
    this.app.use("/", mediaRouter);

    this.app.use("/", chatRouter);

    this.app.get("/health", (_req, res) => {
      const worker = mediasoupService.getWorker();
      const ready = !!worker && !worker.closed;
      res.status(ready ? 200 : 503).json({ service: "keepick-realtime", ready });
    });

    this.app.get("/", (req, res) => {
      res.send(`
        <h2>MediaSoup SFU Server (${this.serverManager.server instanceof https.Server ? "HTTPS" : "HTTP"})</h2>
        <p>SFU 서버가 정상적으로 실행 중입니다.</p>
        <p>프로토콜: ${this.serverManager.server instanceof https.Server ? "HTTPS" : "HTTP"}</p>
        <p>포트: ${serverConfig.port}</p>
      `);
    });
  }

  async start(): Promise<void> {
    try {
      await mediasoupService.createWorker();    // Mediasoup Server 초기화
      this.serverManager.listen();              // HTTP/HTTPS 서버 시작
      
      this.setupGracefulShutdown();
    } catch (error) {
      logger.error("Failed to start server:", error);
      process.exit(1);
    }
  }
  
  private setupGracefulShutdown(): void {
    const shutdown = async () => {
      logger.info("Server shutting down...");
      
      await mediasoupService.closeWorker();  // MediaSoup Worker 정리
      this.socketManager.close();            // Socket.IO 서버 정리
      await this.serverManager.close();      // HTTP/HTTPS 서버 정리

    };
    process.on("SIGTERM", shutdown);
    process.on("SIGINT", shutdown);

    process.on("uncaughtException", (err) => {
      logger.error("Uncaught Exception:", err);
      process.exit(1);
    });

    process.on("unhandledRejection", (err) => {
      logger.error("Unhandled Rejection:", err);
      process.exit(1);
    });
  }
}

// 애플리케이션 시작
const app = new Application();
app.start().catch((error) => {
  console.error("Failed to start application:", error);
  process.exit(1);
});
