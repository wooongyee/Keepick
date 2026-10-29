import { Server, Socket } from "socket.io";
import { realtimeAccess } from "../../domain/auth/access";
import { registerReactionHandler } from "../../domain/reaction/reaction.handler";
import { roomService } from "../../domain/room/factory/room.factory";
import { chatService } from "../../domain/chat/factory/chat.factory";
import { screenShareService } from "../../domain/screenshare/factory/screen-share.factory";
import { RoomEventsHandler } from "../../domain/room/service/room-events.handler";
import { TransportEventsHandler } from "../../domain/transport/service/transport-events.handler";
import { MediaEventsHandler } from "../../domain/media/media-events.handler";
import { ChatEventsHandler } from "../../domain/chat/service/chat-events.handler";

import {
  CreateTransportData,
  ConnectTransportData,
  ProduceData,
  ConsumeData,
  JoinRoomData,
} from "../../domain/room/types/room.types";
import {
  SendMessageData,
  EditMessageData,
  DeleteMessageData,
  TypingData,
  GetMessagesData,
} from "../../domain/chat/types/chat.types";
// import {
//   StartScreenShareData,
//   StopScreenShareData,
//   ConsumeScreenShareData,
// } from "../../domain/screenshare/types/screen-share.types";

export function setupSocketHandlers(io: Server): void {
  io.on("connection", (socket: Socket) => {
    socket.use(async ([event, data], next) => {
      if (!realtimeAccess.required) { next(); return; }
      try {
        if (event === "join_room" || event === "chat_join") {
          await realtimeAccess.authorizeRoom(socket, data?.roomId);
          data.userName = socket.data.keepick.nickname;
        } else if (event !== "chat_leave" && data?.roomId && data.roomId !== roomService.getRoomByPeerId(socket.id)?.id) {
          throw new Error("그룹 접근 권한이 없습니다.");
        }
        next();
      } catch { socket.emit("error", { message: "로그인과 그룹 접근 권한을 확인해 주세요." }); }
    });
    registerReactionHandler(io, socket, roomService);
    // 핸들러들 인스턴스 생성
    const roomEventsHandler = new RoomEventsHandler(roomService);
    const transportEventsHandler = new TransportEventsHandler(roomService);
    const mediaEventsHandler = new MediaEventsHandler(
      roomService,
      roomEventsHandler,
      screenShareService
    );
    const chatEventsHandler = new ChatEventsHandler(chatService);
    // const screenShareEventsHandler = new ScreenShareEventsHandler(
    //   screenShareService,
    //   roomService,
    //   mediaEventsHandler
    // );
    // =========================
    // Room 관련 이벤트
    socket.on("join_room", async (data: JoinRoomData) => {
      await roomEventsHandler.handleJoinRoom(socket, data);
      // 룸 입장 시 자동으로 채팅방도 입장
      await chatEventsHandler.handleJoinChat(socket, {
        roomId: data.roomId,
        userName: data.userName,
      });
    });

    socket.on("leave_room", () => {
      roomEventsHandler.handleLeaveRoom(socket);
      // 룸 퇴장 시 채팅방도 퇴장
      chatEventsHandler.handleLeaveChat(socket);
    });

    socket.on("get_room_info", (data: { roomId: string }) => {
      roomEventsHandler.handleGetRoomInfo(socket, data);
    });

    // =========================
    // Transport 관련 이벤트
    socket.on("create_producer_transport", async (data: CreateTransportData) => {
      await transportEventsHandler.handleCreateProducerTransport(socket, data);
    });

    socket.on("create_consumer_transport", async (data: CreateTransportData) => {
      await transportEventsHandler.handleCreateConsumerTransport(socket, data);
    });

    socket.on("connect_transport", async (data: ConnectTransportData) => {
      await transportEventsHandler.handleConnectTransport(socket, data);
    });

    // =========================
    // Media 관련 이벤트
    socket.on("produce", async (data: ProduceData) => {
      await mediaEventsHandler.handleProduce(socket, data);
    });

    socket.on("consume", async (data: ConsumeData) => {
      await mediaEventsHandler.handleConsume(socket, data);
    });

    socket.on("resume_consumer", async (data: { consumerId: string }) => {
      await mediaEventsHandler.handleResumeConsumer(socket, data);
    });

    socket.on("pause_producer", async (data: { producerId: string }) => {
      await mediaEventsHandler.handlePauseProducer(socket, data);
    });

    socket.on("resume_producer", async (data: { producerId: string }) => {
      await mediaEventsHandler.handleResumeProducer(socket, data);
    });

    // Media 관련 이벤트 섹션에 추가
    socket.on("close_producer", async (data: { producerId: string }) => {
      await mediaEventsHandler.handleCloseProducer(socket, data);
    });

    // =========================
    // Gesture 관련 이벤트
    socket.on("gesture_detect", (data) => {
      // Preserve the legacy gesture/effect API, with room and sender ownership
      // enforced just like the current AI reaction path.
      const room = roomService.getRoomByPeerId(socket.id);
      const peer = room?.peers.get(socket.id);
      if (!data || typeof data !== "object" || !room || !peer ||
          data.roomId !== room.id || !socket.rooms.has(room.id)) return;
      socket.to(room.id).emit("gesture_detected", {
        ...data, roomId: room.id, userId: socket.id, userName: peer.name, timestamp: Date.now(),
      });
    });

    // // =========================
    // // 화면 공유 관련 이벤트
    // socket.on("start_screen_share", async (data: StartScreenShareData) => {
    //   await screenShareEventsHandler.handleStartScreenShare(socket, data);
    // });

    // socket.on("stop_screen_share", async (data: StopScreenShareData) => {
    //   await screenShareEventsHandler.handleStopScreenShare(socket, data);
    // });

    // socket.on("consume_screen_share", async (data: ConsumeScreenShareData) => {
    //   await screenShareEventsHandler.handleConsumeScreenShare(socket, data);
    // });

    // socket.on("get_active_screen_shares", (data: { roomId: string }) => {
    //   screenShareEventsHandler.handleGetActiveScreenShares(socket, data);
    // });

    // socket.on("debug_screen_share", (data: { roomId: string }) => {
    //   screenShareService.debugRoom(data.roomId);
    // });

    // =========================
    // Chat 관련 이벤트
    socket.on("chat_join", async (data: { roomId: string; userName: string }) => {
      await chatEventsHandler.handleJoinChat(socket, data);
    });

    socket.on("chat_leave", (data?: { roomId: string }) => {
      chatEventsHandler.handleLeaveChat(socket, data);
    });

    socket.on("chat_send_message", async (data: SendMessageData) => {
      await chatEventsHandler.handleSendMessage(socket, data);
    });

    socket.on("chat_edit_message", async (data: EditMessageData) => {
      await chatEventsHandler.handleEditMessage(socket, data);
    });

    socket.on("chat_delete_message", async (data: DeleteMessageData) => {
      await chatEventsHandler.handleDeleteMessage(socket, data);
    });

    socket.on("chat_typing", async (data: TypingData) => {
      await chatEventsHandler.handleTyping(socket, data);
    });

    socket.on("chat_get_messages", async (data: GetMessagesData) => {
      await chatEventsHandler.handleGetMessages(socket, data);
    });

    socket.on("chat_get_info", (data: { roomId: string }) => {
      chatEventsHandler.handleGetChatInfo(socket, data);
    });

    // =========================
    // 연결 해제
    socket.on("disconnect", () => {
      roomEventsHandler.handleDisconnect(socket);
      chatEventsHandler.handleLeaveChat(socket); // 모든 채팅방에서 퇴장
    });
  });
}
