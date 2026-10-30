import type { Server, Socket } from "socket.io";
import type { RoomService } from "../room/service/room.service";

const emojis = new Set(["👎", "✊", "👍", "👉", "🫶", "👌", "✋", "🤙", "🤘", "✌️", "🔥", "👋", "💥", "🚫", "🐾", "💖", "😄", "😤", "😲", "🥱", "😠", "😢", "😊", "😐"]);
const object = (value: unknown): Record<string, unknown> | null =>
  value !== null && typeof value === "object" ? value as Record<string, unknown> : null;

// One room-scoped contract for the active conference AI reaction path.
// Identity and timestamps always come from the joined socket on this server.
export function registerReactionHandler(io: Server, socket: Socket, rooms: RoomService): void {
  const seen = new Map<string, number>();
  let lastSent = 0;
  socket.on("ai_reaction", (payload: unknown) => {
    const data = object(payload), reaction = object(data?.reaction);
    const room = rooms.getRoomByPeerId(socket.id), peer = room?.peers.get(socket.id);
    if (!data || !reaction || !room || !peer || data.roomId !== room.id || !socket.rooms.has(room.id)) return;
    if (typeof reaction.id !== "string" || !/^[a-zA-Z0-9_-]{1,100}$/.test(reaction.id) ||
        typeof reaction.emoji !== "string" || !emojis.has(reaction.emoji) ||
        (reaction.aiType !== "gesture" && reaction.aiType !== "emotion") ||
        typeof reaction.confidence !== "number" || !Number.isFinite(reaction.confidence) ||
        reaction.confidence < 0 || reaction.confidence > 1) return;
    const now = Date.now();
    if (seen.has(reaction.id) || now - lastSent < 200) return;
    lastSent = now;
    seen.set(reaction.id, now);
    if (seen.size > 100) seen.delete(seen.keys().next().value!);
    const duration = typeof reaction.duration === "number" && Number.isFinite(reaction.duration)
      ? Math.max(500, Math.min(5000, reaction.duration)) : 2000;
    io.to(room.id).emit("ai_reaction_received", {
      roomId: room.id, userId: socket.id, userName: peer.name,
      reaction: {
        id: `${socket.id}:${reaction.id}`, emoji: reaction.emoji, userId: socket.id, userName: peer.name,
        timestamp: now, duration, source: "ai", aiType: reaction.aiType, confidence: reaction.confidence,
      },
    });
  });
  // Reset the per-room deduplication when a socket joins another call.
  socket.on("leave_room", () => { seen.clear(); lastSent = 0; });
}
