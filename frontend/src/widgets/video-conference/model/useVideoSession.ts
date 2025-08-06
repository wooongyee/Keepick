import { useState, useCallback } from "react";
import { User } from "@/shared/types/webrtc";

export const useVideoSession = () => {
  const [isConnected, setIsConnected] = useState<boolean>(false);
  const [roomId, setRoomId] = useState<string>("");
  const [isInRoom, setIsInRoom] = useState<boolean>(false);
  const [connectionState, setConnectionState] = useState<string>("new");
  const [users, setUsers] = useState<User[]>([]);
  const [error, setError] = useState<string>("");

  const handleConnect = useCallback(() => {
    console.log("✅ Socket connected");
    setIsConnected(true);
    setError("");
  }, []);

  const handleDisconnect = useCallback(() => {
    console.log("❌ Socket disconnected");
    setIsConnected(false);
  }, []);

  const handleAllUsers = useCallback((allUsers: User[]) => {
    console.log("👥 All users received:", allUsers);
    setUsers(allUsers);
  }, []);

  const handleUserJoined = useCallback((user: User) => {
    console.log("👋 User joined:", user);
    setUsers((prev) => [...prev, user]);
  }, []);

  const handleUserExit = useCallback((data: { id: string }) => {
    console.log("👋 User left:", data.id);
    setUsers((prev) => prev.filter((user) => user.id !== data.id));
  }, []);

  const handleRoomFull = useCallback(() => {
    setError("룸이 가득 찼습니다. 다른 룸 이름을 시도해보세요.");
  }, []);

  const handleError = useCallback((errorData: { message: string }) => {
    setError(errorData.message);
  }, []);

  const clearError = useCallback(() => {
    setError("");
  }, []);

  return {
    // State
    isConnected,
    roomId,
    isInRoom,
    connectionState,
    users,
    error,

    // Setters
    setRoomId,
    setIsInRoom,
    setConnectionState,
    setError,

    // Handlers
    handleConnect,
    handleDisconnect,
    handleAllUsers,
    handleUserJoined,
    handleUserExit,
    handleRoomFull,
    handleError,
    clearError,
  };
};
