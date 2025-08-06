// src/shared/api/socket/useSocket.ts

import { useRef, useCallback, useEffect } from "react";
import { io } from "socket.io-client";
import type { Socket } from "socket.io-client";
import { SOCKET_SERVER_URL } from "@/shared/config";
import type { RtpCapabilities } from "mediasoup-client/types";
import type { User } from "@/shared/types/webrtc";

interface ProducerInfo {
  producerId: string;
  kind: "audio" | "video";
}

interface PeerWithProducers extends User {
  producers: ProducerInfo[];
}

interface JoinRoomPayload {
  roomId: string;
  userName: string;
}

interface JoinRoomResponse {
  rtpCapabilities: RtpCapabilities;
  peers: PeerWithProducers[];
}

interface SocketCallbacks {
  onConnect?: () => void;
  onDisconnect?: () => void;
  onConnectError?: (error: Error) => void;
}

export const useSocket = (callbacks?: SocketCallbacks) => {
  const socketRef = useRef<Socket | null>(null);

  const initializeSocket = useCallback(() => {
    if (socketRef.current?.connected) {
      console.log("Socket already connected");
      return;
    }

    console.log("🔌 Connecting to:", SOCKET_SERVER_URL);

    const socket = io(SOCKET_SERVER_URL, {
      path: "/sfu-demo/socket.io/",
      autoConnect: true,
      reconnection: true,
      reconnectionAttempts: 5,
      reconnectionDelay: 1000,
      timeout: 10000,
    });

    socket.on("connect", () => {
      console.log("✅ Socket connected successfully");
      callbacks?.onConnect?.();
    });

    socket.on("disconnect", (reason) => {
      console.log("❌ Socket disconnected:", reason);
      callbacks?.onDisconnect?.();
    });

    socket.on("connect_error", (error) => {
      console.error("❌ Socket connection error:", error);
      callbacks?.onConnectError?.(error);
    });

    socketRef.current = socket;
    console.log("✅ Socket initialized");
  }, [callbacks]);

  const joinRoom = useCallback(
    (payload: JoinRoomPayload): Promise<JoinRoomResponse> => {
      return new Promise((resolve, reject) => {
        if (!socketRef.current?.connected) {
          return reject(new Error("Socket not connected. Cannot join room."));
        }

        console.log(
          `🚪 Emitting join_room request for room: ${payload.roomId} as ${payload.userName}`
        );

        socketRef.current.emit("join_room", payload);

        const handleJoinedRoom = (data: JoinRoomResponse) => {
          if (data && data.rtpCapabilities) {
            console.log("✅ Successfully joined room with data:", data);
            resolve(data);
          } else {
            console.error("❌ Invalid 'joined_room' response:", data);
            reject(new Error("Server response for join_room is invalid."));
          }
          socketRef.current?.off("error", handleError);
        };

        const handleError = (error: { message: string }) => {
          console.error("❌ Error while joining room:", error);
          reject(new Error(error.message || "Failed to join room"));
          socketRef.current?.off("joined_room", handleJoinedRoom);
        };

        socketRef.current.once("joined_room", handleJoinedRoom);
        socketRef.current.once("error", handleError);
      });
    },
    []
  );

  const leaveRoom = useCallback((): Promise<void> => {
    return new Promise((resolve) => {
      if (!socketRef.current?.connected) {
        console.log("Socket not connected. Cannot leave room.");
        resolve();
        return;
      }

      console.log("🚪 Emitting leave_room request");

      // 서버로부터 left_room 확인을 받으면 resolve
      const handleLeftRoom = () => {
        console.log("✅ Successfully left room");
        resolve();
      };

      socketRef.current.once("left_room", handleLeftRoom);
      socketRef.current.emit("leave_room");

      // 타임아웃 설정 (3초 후 강제 resolve)
      setTimeout(() => {
        socketRef.current?.off("left_room", handleLeftRoom);
        console.log("⏰ Leave room timeout - forcing resolve");
        resolve();
      }, 3000);
    });
  }, []);

  const disconnect = useCallback(() => {
    if (socketRef.current) {
      socketRef.current.removeAllListeners();
      socketRef.current.disconnect();
      socketRef.current = null;
    }
  }, []);

  useEffect(() => {
    initializeSocket();
    return () => {
      disconnect();
    };
  }, [initializeSocket, disconnect]);

  return {
    socket: socketRef.current,
    joinRoom,
    leaveRoom,
    disconnect,
    isConnected: socketRef.current?.connected || false,
  };
};
