import type { Socket } from "socket.io-client";
import type { AppDispatch } from "@/shared/config/store";
import { addReaction, clearAllReactions, type EmojiReaction } from "@/entities/emoji-reaction/model/slice";

export interface AiEventData {
  roomId: string;
  userId: string;
  userName: string;
  reaction: EmojiReaction;
}

class AiEventHandler {
  private socket: Socket | null = null;
  private dispatch: AppDispatch | null = null;
  private currentRoomId = "";
  private seen = new Set<string>();

  private received = (data: AiEventData): void => {
    if (!this.dispatch || !this.socket || data.roomId !== this.currentRoomId ||
        !data.reaction || typeof data.reaction.id !== "string" || this.seen.has(data.reaction.id)) return;
    this.seen.add(data.reaction.id);
    if (this.seen.size > 100) this.seen.delete(this.seen.values().next().value!);
    this.dispatch(addReaction({
      ...data.reaction,
      // "local" is a UI card key only; it is never sent as participant identity.
      userId: data.userId === this.socket.id ? "local" : data.userId,
      userName: data.userName,
    }));
  };

  public init(socket: Socket, dispatch: AppDispatch): void {
    if (this.socket === socket && this.dispatch === dispatch) return;
    this.cleanup();
    this.socket = socket;
    this.dispatch = dispatch;
    socket.on("ai_reaction_received", this.received);
  }

  public setRoomId(roomId: string): void {
    if (this.currentRoomId !== roomId) {
      this.seen.clear();
      this.dispatch?.(clearAllReactions());
    }
    this.currentRoomId = roomId;
  }

  public sendAiReaction(reaction: EmojiReaction): void {
    if (!this.socket?.connected || !this.currentRoomId) return;
    this.socket.emit("ai_reaction", {
      roomId: this.currentRoomId,
      reaction: {
        id: reaction.id, emoji: reaction.emoji, duration: reaction.duration,
        aiType: reaction.aiType, confidence: reaction.confidence,
      },
    });
  }

  public cleanup(): void {
    this.socket?.off("ai_reaction_received", this.received);
    this.dispatch?.(clearAllReactions());
    this.socket = null;
    this.dispatch = null;
    this.currentRoomId = "";
    this.seen.clear();
  }
}

export const aiEventHandler = new AiEventHandler();
