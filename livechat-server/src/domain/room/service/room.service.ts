import { RoomState, Peer } from "../types/room.types";
import { RoomRepository } from "../repository/room.repository";
import { PeerManager } from "./peer.manager";
import { logger } from "../../../utils/logger";
import { Socket } from "socket.io";
import { randomUUID } from "crypto";

export class RoomService {
  constructor(
    private roomRepository: RoomRepository,
    private peerManager: PeerManager
  ) {}

  async createRoom(roomId: string): Promise<RoomState> {
    const existingRoom = this.roomRepository.getRoom(roomId);
    if (existingRoom) {
      return existingRoom;
    }

    const chatSessionId = randomUUID(); // 채팅 세션 ID 생성
    const room = await this.roomRepository.createRoom(roomId, chatSessionId);
    logger.info(`Created new room: ${roomId}`);
    return room;
  }

  getRoom(roomId: string): RoomState | undefined {
    return this.roomRepository.getRoom(roomId);
  }

  async joinRoom(
    roomId: string,
    socket: Socket,
    userName: string
  ): Promise<Peer> {
    let room = this.roomRepository.getRoom(roomId);
    if (!room) {
      room = await this.createRoom(roomId);
    }

    const peer = this.peerManager.createPeer(socket, userName, roomId);
    room.peers.set(socket.id, peer);
    socket.join(roomId);

    logger.info(`Peer ${socket.id} (${userName}) joined room ${roomId}`);
    return peer;
  }

  getProducersForPeer(roomId: string, socketId: string) {
    const room = this.getRoom(roomId);
    if (!room) return [];

    return this.peerManager.getProducersForPeer(room, socketId);
  }

  leaveRoom(socketId: string): void {
    const room = this.roomRepository.getRoomByPeerId(socketId);
    if (!room) return;

    const peer = room.peers.get(socketId);
    if (peer) {
      this.peerManager.cleanupPeer(peer);
      room.peers.delete(socketId);
      peer.socket.leave(room.id);

      // 룸이 비어있으면 삭제
      if (room.peers.size === 0) {
        this.roomRepository.deleteRoom(room.id);
        logger.info(`Empty room deleted: ${room.id}`);
      }

      logger.info(`Peer ${socketId} left room ${room.id}`);
    }
  }

  getPeer(socketId: string): Peer | undefined {
    return this.roomRepository.getPeer(socketId);
  }

  getRoomByPeerId(socketId: string): RoomState | undefined {
    return this.roomRepository.getRoomByPeerId(socketId);
  }

  getRoomsInfo() {
    return this.roomRepository.getRoomsInfo();
  }
}
