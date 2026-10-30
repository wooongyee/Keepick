// src/shared/api/socket/socketManager.ts

import { io, Socket } from "socket.io-client";
import { AppDispatch } from "@/shared/config/store";
import { SOCKET_SERVER_URL } from "@/shared/config";
import { setConnected, setError } from "@/entities/video-conference/session/model/slice";

interface SocketEventHandler {
  initialize(socket: Socket, dispatch: AppDispatch): void;
}

class SocketManager {
  private socket: Socket | null = null;
  private isInitialized: boolean = false;
  private handlers: SocketEventHandler[] = [];

  public init(dispatch: AppDispatch, handlers: SocketEventHandler[]) {
    if (this.isInitialized) {
      console.warn("SocketManager already initialized. Skipping.");
      return;
    }

    console.log("Connecting to socket server...");
    this.socket = io(SOCKET_SERVER_URL, {
      transports: ["websocket"],
      auth: (callback) => callback({ token: typeof window !== "undefined" ? localStorage.getItem("accessToken") : null }),
    });
    this.handlers = handlers;
    this.setupCoreListeners(dispatch);
    this.registerHandlers(dispatch);
    this.isInitialized = true;
  }

  private setupCoreListeners(dispatch: AppDispatch) {
    if (!this.socket) return;
    this.socket.on("connect", () => {
      console.log("✅ Socket connected:", this.socket?.id);
      dispatch(setConnected(true));
    });
    this.socket.on("disconnect", () => {
      console.log("❌ Socket disconnected");
      dispatch(setConnected(false));
      this.isInitialized = false;
    });
    this.socket.on("connect_error", (err) => {
      console.error("❌ Socket connection error:", err.message);
      dispatch(setError(`Socket connection error: ${err.message}`));
    });
  }

  private registerHandlers(dispatch: AppDispatch) {
    if (!this.socket) return;
    this.handlers.forEach((handler) => handler.initialize(this.socket!, dispatch));
  }

  public getSocket = (): Socket | null => this.socket;

  /**
   * [추가] 서버에 요청을 보내고 응답을 Promise로 받는 메서드
   * @param event 요청을 보낼 이벤트 이름
   * @param responseEvent 응답을 받을 이벤트 이름
   * @param data 보낼 데이터
   * @param timeout 타임아웃 (ms)
   */
  public request<T>(
    event: string, responseEvent: string, data: unknown, timeout = 3000,
    matches: (response: T) => boolean = () => true,
  ): Promise<T> {
    const socket = this.socket;
    if (!socket) return Promise.reject(new Error("Socket not initialized."));
    return new Promise<T>((resolve, reject) => {
      const clean = () => {
        clearTimeout(timer);
        socket.off(responseEvent, received);
      };
      const received = (response: T) => {
        if (!matches(response)) return;
        clean();
        resolve(response);
      };
      const timer = setTimeout(() => {
        clean();
        reject(new Error(`Request for '${event}' timed out after ${timeout}ms`));
      }, timeout);
      socket.on(responseEvent, received);
      socket.emit(event, data, (ack?: { error?: string }) => {
        if (ack?.error) { clean(); reject(new Error(ack.error)); }
      });
    });
  }
}

export const socketManager = new SocketManager();
