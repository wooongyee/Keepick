// src/domain/media/media-events.handler.ts

import { Socket } from "socket.io";
import { RoomService } from "../room/service/room.service";
import { RoomEventsHandler } from "../room/service/room-events.handler";
import { logger } from "../../utils/logger";
import { ProduceData, ConsumeData } from "../room/types/room.types";
import {
  ProducerAppData,
  ConsumerAppData,
  isScreenShareProducer,
  getProducerAppData,
  getConsumerAppData,
} from "../../shared/types/media.type";
import { ScreenShareService } from "../screenshare/services/screen-share.service";

export class MediaEventsHandler {
  constructor(
    private roomService: RoomService,
    private roomEventsHandler: RoomEventsHandler,
    private screenShareService: ScreenShareService
  ) {}

  async handleProduce(socket: Socket, data: ProduceData) {
    try {
      const { transportId, kind, rtpParameters, roomId, appData } = data;
      const room = this.roomService.getRoom(roomId);
      const peer = room?.peers.get(socket.id);

      if (!room || !peer) {
        throw new Error("Room or peer not found");
      }

      const transport = peer.transports.get(transportId);
      if (!transport) {
        throw new Error("Transport not found");
      }

      // MediaSoup Producer에 appData 포함하여 생성
      const producer = await transport.produce({
        kind,
        rtpParameters,
        appData: appData as any,
      });

      peer.producers.set(producer.id, producer);

      // 🔧 헬퍼 함수로 안전하게 appData 추출
      const producerAppData = getProducerAppData(producer);
      const isScreenShare = producerAppData && isScreenShareProducer(producerAppData);

      logger.info(
        `Producer created: ${producer.id} (${kind})${isScreenShare ? " [SCREEN SHARE]" : ""} for ${
          socket.id
        }`,
        {
          producerId: producer.id,
          kind,
          socketId: socket.id,
          appData: producerAppData,
          isScreenShare,
        }
      );

      // 방에 있는 다른 모든 peer에게 새로 생성된 producer의 정보를 알림
      const newProducerData = {
        producerId: producer.id,
        producerSocketId: socket.id,
        kind,
        appData: producerAppData || {},
      };

      socket.to(roomId).emit("new_producer", newProducerData);

      // 요청을 보낸 peer에게 성공했음을 알림
      socket.emit("producer_created", {
        id: producer.id,
        appData: producerAppData,
      });

      const otherPeers = Array.from(room.peers.keys()).filter((id) => id !== socket.id);

      logger.info(
        `Notified ${otherPeers.length} peers about new ${kind}${
          isScreenShare ? " screen share" : ""
        } producer from ${socket.id}`,
        {
          producerId: producer.id,
          notifiedPeerCount: otherPeers.length,
          isScreenShare,
        }
      );
    } catch (error) {
      logger.error("Error creating producer:", error);
      socket.emit("error", { message: "Failed to create producer" });
    }
  }

  async handleConsume(socket: Socket, data: ConsumeData) {
    try {
      const { transportId, producerId, rtpCapabilities, roomId } = data;

      if (!this.roomEventsHandler.addPendingConsumerRequest(socket.id, producerId)) {
        return;
      }

      const room = this.roomService.getRoom(roomId);
      const peer = room?.peers.get(socket.id);

      if (!room || !peer) {
        this.roomEventsHandler.removePendingConsumerRequest(socket.id, producerId);
        throw new Error("Room or peer not found");
      }

      const transport = peer.transports.get(transportId);
      if (!transport) {
        this.roomEventsHandler.removePendingConsumerRequest(socket.id, producerId);
        throw new Error("Transport not found");
      }

      // Producer 찾기
      let producer;
      let producerAppData: ProducerAppData | undefined;

      for (const [, otherPeer] of room.peers.entries()) {
        producer = otherPeer.producers.get(producerId);
        if (producer) {
          // 🔧 헬퍼 함수로 안전하게 appData 추출
          producerAppData = getProducerAppData(producer);
          break;
        }
      }

      if (!producer) {
        this.roomEventsHandler.removePendingConsumerRequest(socket.id, producerId);
        throw new Error(`Producer ${producerId} not found`);
      }

      // Consumer용 appData 생성
      const consumerAppData: ConsumerAppData = {
        ...producerAppData!,
        producerId,
        consumerId: `consumer_${socket.id}_${producerId}`,
        consumerPeerId: socket.id,
      };

      // Consumer 생성
      const consumer = await transport.consume({
        producerId,
        rtpCapabilities,
        paused: false,
        appData: consumerAppData as any,
      });

      peer.consumers.set(consumer.id, consumer);
      // Closing the source producer also closes its consumers in the worker.
      // Remove those references while the receiving peer stays in the room.
      consumer.observer.once("close", () => peer.consumers.delete(consumer.id));

      const isScreenShare = producerAppData && isScreenShareProducer(producerAppData);

      logger.info(
        `Consumer created: ${consumer.id} (${consumer.kind})${
          isScreenShare ? " [SCREEN SHARE]" : ""
        } for ${socket.id}`,
        {
          consumerId: consumer.id,
          producerId,
          kind: consumer.kind,
          socketId: socket.id,
          isScreenShare,
          producerAppData,
          consumerAppData,
        }
      );

      // 요청을 보낸 peer에게 성공했음을 알림
      socket.emit("consumer_created", {
        id: consumer.id,
        producerId,
        kind: consumer.kind,
        rtpParameters: consumer.rtpParameters,
        appData: producerAppData,
      });

      this.roomEventsHandler.removePendingConsumerRequest(socket.id, producerId);
    } catch (error) {
      this.roomEventsHandler.removePendingConsumerRequest(socket.id, data.producerId);
      logger.error("Error creating consumer:", error);
      socket.emit("error", { message: "Failed to create consumer" });
    }
  }

  async handleResumeConsumer(socket: Socket, data: { consumerId: string }) {
    try {
      const { consumerId } = data;
      const peer = this.roomService.getPeer(socket.id);

      if (!peer) {
        throw new Error("Peer not found");
      }

      const consumer = peer.consumers.get(consumerId);
      if (!consumer) {
        throw new Error("Consumer not found");
      }

      await consumer.resume();

      // 🔧 헬퍼 함수로 안전하게 appData 추출
      const consumerAppData = getConsumerAppData(consumer);
      const isScreenShare = consumerAppData && isScreenShareProducer(consumerAppData);

      logger.info(
        `Consumer resumed: ${consumerId}${isScreenShare ? " [SCREEN SHARE]" : ""} for ${socket.id}`,
        {
          consumerId,
          socketId: socket.id,
          isScreenShare,
          consumerAppData,
        }
      );

      socket.emit("consumer_resumed", { consumerId });
    } catch (error) {
      logger.error("Error resuming consumer:", error);
      socket.emit("error", { message: "Failed to resume consumer" });
    }
  }


  // producer를 일시 중지하는 메서드
  async handlePauseProducer(socket: Socket, data: { producerId: string }) {
    const { producerId } = data;

    const room = this.roomService.getRoomByPeerId(socket.id);
    if (!room) return;

    const peer = room.peers.get(socket.id);
    if (!peer) return;

    const producer = peer.producers.get(producerId);
    if (!producer) return;

    // Producer를 일시 중지
    await producer.pause();
    logger.info(`Producer ${producerId} paused by ${socket.id}`);

    // 방에 있는 다른 모든 peer에게 producer가 일시 중지되었음을 알림
    socket.to(room.id).emit("producer_paused", {
      producerId,
      socketId: socket.id,
    });
  }

  // producer를 다시 시작하는 메서드
  async handleResumeProducer(socket: Socket, data: { producerId: string }) {
    const { producerId } = data;

    const room = this.roomService.getRoomByPeerId(socket.id);
    if (!room) return;

    const peer = room.peers.get(socket.id);
    if (!peer) return;

    const producer = peer.producers.get(producerId);
    if (!producer) return;

    // Producer를 다시 시작
    await producer.resume();
    logger.info(`Producer ${producerId} resumed by ${socket.id}`);

    // 방에 있는 다른 모든 peer에게 producer가 다시 시작되었음을 알림
    socket.to(room.id).emit("producer_resumed", {
      producerId,
      socketId: socket.id,
    });
  }


  async handleCloseProducer(socket: Socket, data: { producerId: string }) {
    try {
      const { producerId } = data;
      const peer = this.roomService.getPeer(socket.id);
      if (!peer) {
        throw new Error("Peer not found");
      }

      const producer = peer.producers.get(producerId);
      if (!producer) {
        throw new Error("Producer not found");
      }

      // Producer를 닫습니다.
      producer.close();

      // Peer의 producer 목록에서 제거
      peer.producers.delete(producerId);

      // Producer가 닫혔음을 방 안의 모든 사람에게 알립니다.
      // 클라이언트는 이 이벤트를 받아 관련 미디어 엘리먼트를 제거해야 합니다.
      socket.to(peer.roomId).emit("producer_closed", { producerId });

      logger.info(`Producer closed: ${producerId} for peer ${socket.id}`);
      socket.emit("producer_closed_success", { producerId });
    } catch (error) {
      logger.error(`Error closing producer for ${socket.id}:`, error);
      socket.emit("error", { message: "Failed to close producer" });
    }
  }
}
