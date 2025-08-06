// src/widgets/video-conference/lib/useMediaSoup.ts
import { useRef, useCallback, useState } from "react";
import { Device } from "mediasoup-client";
import type { Socket } from "socket.io-client";
import type {
  Transport,
  Producer,
  Consumer,
  RtpCapabilities,
  DtlsParameters,
} from "mediasoup-client/types";
import type {
  ConsumerCreatedData,
  TransportOptions,
  ProducerCreatedData,
} from "@/shared/types/mediasoup.types";

const ICE_SERVERS = [
  { urls: "stun:stun.l.google.com:19302" },
  { urls: "stun:stun1.l.google.com:19302" },
];

// 에러를 일관되게 처리하기 위한 헬퍼 함수
const toError = (error: unknown): Error => {
  if (error instanceof Error) {
    return error;
  }
  return new Error(String(error));
};

export const useMediasoup = (socket: Socket | null) => {
  const deviceRef = useRef<Device | null>(null);
  const producerTransportRef = useRef<Transport | null>(null);
  const consumerTransportRef = useRef<Transport | null>(null);
  const producersRef = useRef<Map<string, Producer>>(new Map());
  const consumersRef = useRef<Map<string, Consumer>>(new Map());
  const localStreamRef = useRef<MediaStream | null>(null);

  const [deviceLoaded, setDeviceLoaded] = useState(false);
  const [isProducing, setIsProducing] = useState(false);

  const initializeDevice = useCallback(
    async (rtpCapabilities: RtpCapabilities) => {
      try {
        const device = new Device();
        await device.load({ routerRtpCapabilities: rtpCapabilities });
        deviceRef.current = device;
        setDeviceLoaded(true);
        console.log("✅ MediaSoup device loaded");
        return device;
      } catch (error) {
        console.error("❌ Failed to initialize device:", error);
        throw toError(error);
      }
    },
    []
  );

  const createProducerTransport = useCallback(
    (roomId: string) => {
      if (!socket || !deviceRef.current) {
        throw new Error("Socket or device not available");
      }

      return new Promise<Transport>((resolve, reject) => {
        socket.emit("create_producer_transport", { roomId });
        socket.once(
          "producer_transport_created",
          (transportOptions: TransportOptions) => {
            try {
              const transport = deviceRef.current!.createSendTransport({
                ...transportOptions,
                iceServers: ICE_SERVERS,
              });

              transport.on(
                "connect",
                (
                  { dtlsParameters }: { dtlsParameters: DtlsParameters },
                  callback: () => void,
                  errback: (error: Error) => void
                ) => {
                  socket.emit("connect_transport", {
                    transportId: transport.id,
                    dtlsParameters,
                  });
                  socket.once("transport_connected", callback);
                  socket.once("error", (e) => errback(toError(e)));
                }
              );

              transport.on(
                "produce",
                (
                  { kind, rtpParameters },
                  callback,
                  errback: (error: Error) => void
                ) => {
                  try {
                    socket.emit("produce", {
                      transportId: transport.id,
                      kind,
                      rtpParameters,
                      roomId,
                    });
                    socket.once(
                      "producer_created",
                      (data: ProducerCreatedData) => callback({ id: data.id })
                    );
                  } catch (error) {
                    errback(toError(error));
                  }
                }
              );

              producerTransportRef.current = transport;
              console.log("✅ Producer transport created");
              resolve(transport);
            } catch (error) {
              console.error("❌ Failed to create producer transport:", error);
              reject(toError(error));
            }
          }
        );
        socket.once("error", (error) => {
          console.error("❌ Producer transport creation error:", error);
          reject(
            new Error(
              typeof error === "object" && error && "message" in error
                ? String(error.message)
                : "Failed to create producer transport"
            )
          );
        });
      });
    },
    [socket]
  );

  const createConsumerTransport = useCallback(
    (roomId: string) => {
      if (!socket || !deviceRef.current) {
        throw new Error("Socket or device not available");
      }
      return new Promise<Transport>((resolve, reject) => {
        socket.emit("create_consumer_transport", { roomId });
        socket.once(
          "consumer_transport_created",
          (transportOptions: TransportOptions) => {
            try {
              const transport = deviceRef.current!.createRecvTransport({
                ...transportOptions,
                iceServers: ICE_SERVERS,
              });
              transport.on(
                "connect",
                (
                  { dtlsParameters },
                  callback,
                  errback: (error: Error) => void
                ) => {
                  socket.emit("connect_transport", {
                    transportId: transport.id,
                    dtlsParameters,
                  });
                  socket.once("transport_connected", callback);
                  socket.once("error", (e) => errback(toError(e)));
                }
              );
              consumerTransportRef.current = transport;
              console.log("✅ Consumer transport created");
              resolve(transport);
            } catch (error) {
              console.error("❌ Failed to create consumer transport:", error);
              reject(toError(error));
            }
          }
        );
        socket.once("error", (error) => {
          reject(
            new Error(
              typeof error === "object" && error && "message" in error
                ? String(error.message)
                : "Failed to create consumer transport"
            )
          );
        });
      });
    },
    [socket]
  );

  const startProducing = useCallback(async (stream: MediaStream) => {
    if (!producerTransportRef.current)
      throw new Error("Producer transport not available");
    try {
      const videoTrack = stream.getVideoTracks()[0];
      const audioTrack = stream.getAudioTracks()[0];
      const producers: Producer[] = [];
      if (videoTrack) {
        const videoProducer = await producerTransportRef.current.produce({
          track: videoTrack,
        });
        producersRef.current.set(videoProducer.id, videoProducer);
        producers.push(videoProducer);
        console.log("✅ Video producer created:", videoProducer.id);
      }
      if (audioTrack) {
        const audioProducer = await producerTransportRef.current.produce({
          track: audioTrack,
        });
        producersRef.current.set(audioProducer.id, audioProducer);
        producers.push(audioProducer);
        console.log("✅ Audio producer created:", audioProducer.id);
      }
      if (producers.length > 0) setIsProducing(true);
      return producers;
    } catch (error) {
      console.error("❌ Failed to start producing:", error);
      throw toError(error);
    }
  }, []);

  const activeConsumerRequests = useRef<Set<string>>(new Set());
  const consume = useCallback(
    async (producerId: string, roomId: string): Promise<Consumer | null> => {
      if (activeConsumerRequests.current.has(producerId)) return null;
      if (!socket || !consumerTransportRef.current || !deviceRef.current)
        return null;

      activeConsumerRequests.current.add(producerId);
      return new Promise<Consumer>((resolve, reject) => {
        const cleanup = () => activeConsumerRequests.current.delete(producerId);
        socket.emit("consume", {
          transportId: consumerTransportRef.current!.id,
          producerId,
          rtpCapabilities: deviceRef.current!.rtpCapabilities,
          roomId,
        });
        socket.once(
          "consumer_created",
          async (consumerData: ConsumerCreatedData) => {
            try {
              const consumer = await consumerTransportRef.current!.consume(
                consumerData
              );
              consumersRef.current.set(consumer.id, consumer);
              socket.emit("resume_consumer", { consumerId: consumer.id });
              console.log(
                "✅ Consumer created:",
                consumer.id,
                "kind:",
                consumer.kind
              );
              cleanup();
              resolve(consumer);
            } catch (error) {
              console.error("❌ Failed to create consumer:", error);
              cleanup();
              reject(toError(error));
            }
          }
        );
        socket.once("error", (error) => {
          console.error("❌ Consumer creation error:", error);
          cleanup();
          reject(
            new Error(
              typeof error === "object" && error && "message" in error
                ? String(error.message)
                : "Failed to create consumer"
            )
          );
        });
      });
    },
    [socket]
  );

  const initializeLocalMedia = useCallback(async () => {
    try {
      if (localStreamRef.current) {
        localStreamRef.current.getTracks().forEach((track) => track.stop());
      }
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 1280 }, height: { ideal: 720 } },
        audio: true,
      });
      localStreamRef.current = stream;
      console.log("✅ Local media initialized");
      return stream;
    } catch (error) {
      console.error("❌ Failed to initialize local media:", error);
      throw toError(error);
    }
  }, []);

  const getConsumer = useCallback((consumerId: string) => {
    return consumersRef.current.get(consumerId);
  }, []);

  const getProducer = useCallback((producerId: string) => {
    return producersRef.current.get(producerId);
  }, []);

  const cleanup = useCallback(() => {
    console.log("🧹 MediaSoup cleanup started");
    producersRef.current.forEach((p) => p.close());
    producersRef.current.clear();
    consumersRef.current.forEach((c) => c.close());
    consumersRef.current.clear();
    producerTransportRef.current?.close();
    producerTransportRef.current = null;
    consumerTransportRef.current?.close();
    consumerTransportRef.current = null;
    localStreamRef.current?.getTracks().forEach((track) => track.stop());
    localStreamRef.current = null;
    deviceRef.current = null;
    setDeviceLoaded(false);
    setIsProducing(false);
    console.log("✅ MediaSoup cleanup completed");
  }, []);

  return {
    deviceLoaded,
    isProducing,
    initializeDevice,
    createProducerTransport,
    createConsumerTransport,
    startProducing,
    consume,
    initializeLocalMedia,
    cleanup,
    localStreamRef,
    getConsumer,
    getProducer,
  };
};
