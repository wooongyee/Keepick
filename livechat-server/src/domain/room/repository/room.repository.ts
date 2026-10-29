import { RoomState, Peer } from "../types/room.types";
import { mediasoupService } from "../../media/mediasoup.service";

export class RoomRepository {
  private rooms: Map<string, RoomState> = new Map();
  private pendingRooms = new Map<string, Promise<RoomState>>();

  async createRoom(roomId: string, chatSessionId: string): Promise<RoomState> {
    const existing = this.rooms.get(roomId);
    if (existing) return existing;
    const pending = this.pendingRooms.get(roomId);
    if (pending) return pending;

    // Router creation awaits the worker. Concurrent first joins must share it.
    const creation = this.createRoomWithRouter(roomId, chatSessionId);
    this.pendingRooms.set(roomId, creation);
    try {
      return await creation;
    } finally {
      this.pendingRooms.delete(roomId);
    }
  }

  private async createRoomWithRouter(roomId: string, chatSessionId: string): Promise<RoomState> {
    const router = await mediasoupService.createRouter();
    const room: RoomState = {
      id: roomId,
      router,
      peers: new Map(),
      createdAt: new Date(),
      chatSessionId: chatSessionId, // 방 생성시 채팅 세션 ID
    };

    this.rooms.set(roomId, room);
    return room;
  }

  getRoom(roomId: string): RoomState | undefined {
    return this.rooms.get(roomId);
  }

  deleteRoom(roomId: string): void {
    const room = this.rooms.get(roomId);
    if (room) {
      room.router.close();
      this.rooms.delete(roomId);
    }
  }

  getPeer(socketId: string): Peer | undefined {
    for (const room of this.rooms.values()) {
      const peer = room.peers.get(socketId);
      if (peer) return peer;
    }
    return undefined;
  }

  getRoomByPeerId(socketId: string): RoomState | undefined {
    for (const room of this.rooms.values()) {
      if (room.peers.has(socketId)) {
        return room;
      }
    }
    return undefined;
  }

  getRoomsInfo() {
    return Array.from(this.rooms.entries()).map(([id, room]) => ({
      id,
      peersCount: room.peers.size,
      createdAt: room.createdAt,
    }));
  }

  getAllRooms(): Map<string, RoomState> {
    return this.rooms;
  }
}
