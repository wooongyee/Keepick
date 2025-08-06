// src/widgets/video-conference/ui/VideoConference.tsx
"use client";

import React, {
  useEffect,
  useCallback,
  useMemo,
  useRef,
  useState,
} from "react";
import { useSocket } from "@/shared/api/socket/useSocket";
import { useMediasoup } from "../lib/useMediaSoup";
import { useVideoSession } from "../model/useVideoSession";
import { VideoGrid } from "./VideoGrid";
import { ConferenceSidebar } from "./ConferenceSidebar";
import type { Consumer } from "mediasoup-client/types";
import type { User } from "@/shared/types/webrtc";

interface VideoConferenceProps {
  initialRoomId: string;
}

interface NewProducerInfo {
  producerId: string;
  producerSocketId: string;
  kind: "audio" | "video";
}

export const VideoConference: React.FC<VideoConferenceProps> = ({
  initialRoomId,
}) => {
  const {
    isConnected,
    roomId,
    isInRoom,
    connectionState,
    users,
    error,
    setRoomId,
    setIsInRoom,
    setError,
    handleConnect,
    handleDisconnect,
    handleAllUsers,
    handleUserJoined,
    handleUserExit,
    handleError,
    clearError,
  } = useVideoSession();

  const [isStaticGestureOn, setStaticGestureOn] = useState(true);
  const [isDynamicGestureOn, setDynamicGestureOn] = useState(true);
  const [remoteStreams, setRemoteStreams] = useState<Map<string, MediaStream>>(
    new Map()
  );
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [isCameraOn, setCameraOn] = useState(true);
  const [isMicOn, setMicOn] = useState(true);
  const [userName, setUserName] = useState(`게스트`);
  const [isLeaving, setIsLeaving] = useState(false);

  const consumersRef = useRef<Map<string, Consumer>>(new Map());

  useEffect(() => {
    if (initialRoomId && initialRoomId !== roomId) {
      setRoomId(initialRoomId);
      clearError();
    }
  }, [initialRoomId, setRoomId, roomId, clearError]);

  const toggleCamera = useCallback(() => {
    if (localStream) {
      const videoTrack = localStream.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled;
        setCameraOn(videoTrack.enabled);
      }
    }
  }, [localStream]);

  const toggleMicrophone = useCallback(() => {
    if (localStream) {
      const audioTrack = localStream.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        setMicOn(audioTrack.enabled);
      }
    }
  }, [localStream]);

  const socketCallbacks = useMemo(
    () => ({
      onConnect: handleConnect,
      onDisconnect: () => {
        handleDisconnect();
        setIsInRoom(false);
        setRemoteStreams(new Map());
        consumersRef.current.clear();
        setIsLeaving(false);
      },
      onConnectError: (err: Error) =>
        handleError({ message: `Connection error: ${err.message}` }),
    }),
    [handleConnect, handleDisconnect, handleError, setIsInRoom]
  );

  const { socket, joinRoom, leaveRoom } = useSocket(socketCallbacks);
  const {
    deviceLoaded,
    initializeDevice,
    createProducerTransport,
    createConsumerTransport,
    startProducing,
    consume,
    initializeLocalMedia,
    cleanup,
  } = useMediasoup(socket);

  const handleRemoteStream = useCallback(
    (consumer: Consumer, producerSocketId: string) => {
      const track = consumer.track;
      if (track) {
        setRemoteStreams((prev) => {
          const newMap = new Map(prev);
          let stream = newMap.get(producerSocketId) || new MediaStream();
          stream.addTrack(track);
          newMap.set(producerSocketId, stream);
          return newMap;
        });
        consumersRef.current.set(consumer.producerId, consumer);
      }
    },
    []
  );

  const handleLeaveRoom = useCallback(async () => {
    if (isLeaving) return;

    console.log("👋 Leaving room");
    setIsLeaving(true);

    try {
      // 서버에 leave_room 신호 보내고 확인 대기
      await leaveRoom();

      // 로컬 정리
      cleanup();
      consumersRef.current.forEach((consumer) => consumer.close());
      consumersRef.current.clear();
      setLocalStream(null);
      setRemoteStreams(new Map());
      setIsInRoom(false);
      setCameraOn(true);
      setMicOn(true);
      clearError();
      handleAllUsers([]); // 사용자 목록 초기화

      console.log("✅ Successfully left room");
    } catch (error) {
      console.error("❌ Error leaving room:", error);
    } finally {
      setIsLeaving(false);
    }
  }, [leaveRoom, cleanup, setIsInRoom, clearError, handleAllUsers, isLeaving]);

  const handleJoinRoom = useCallback(async () => {
    if (!roomId.trim() || !socket?.connected) {
      setError("룸 이름을 입력해주세요.");
      return;
    }

    if (!userName.trim()) {
      setError("사용자 이름을 입력해주세요.");
      return;
    }

    try {
      // 입장 전 완전한 상태 초기화
      consumersRef.current.clear();
      setRemoteStreams(new Map());
      handleAllUsers([]);

      const { rtpCapabilities, peers } = await joinRoom({
        roomId: roomId.trim(),
        userName: userName.trim(),
      });

      setIsInRoom(true);
      await initializeDevice(rtpCapabilities);

      // UI에 기존 참여자들 정보 반영
      handleAllUsers(peers);

      // 로컬 미디어 초기화 및 프로듀싱 시작
      const stream = await initializeLocalMedia();
      setLocalStream(stream);
      await createProducerTransport(roomId.trim());
      await createConsumerTransport(roomId.trim());
      if (stream) await startProducing(stream);

      // 기존 참여자들의 미디어 스트림 소비
      for (const peer of peers) {
        if (peer.producers && peer.producers.length > 0) {
          for (const producer of peer.producers) {
            try {
              const consumer = await consume(producer.producerId, roomId);
              if (consumer) {
                handleRemoteStream(consumer, peer.id);
              }
            } catch (err) {
              console.error(
                `Failed to consume existing producer ${producer.producerId} for peer ${peer.id}:`,
                err
              );
            }
          }
        }
      }

      clearError();
    } catch (err: unknown) {
      console.error("❌ Failed to join room:", err);
      setError(err instanceof Error ? err.message : "룸 참가에 실패했습니다.");
      await handleLeaveRoom();
    }
  }, [
    roomId,
    userName,
    socket,
    joinRoom,
    initializeDevice,
    initializeLocalMedia,
    createProducerTransport,
    createConsumerTransport,
    startProducing,
    setError,
    clearError,
    handleLeaveRoom,
    handleAllUsers,
    consume,
    handleRemoteStream,
  ]);

  // Socket 이벤트 핸들러
  useEffect(() => {
    if (!socket) return;

    const onUserJoined = (user: User) => {
      console.log(`👋 User joined: ${user.name} (${user.id})`);
      handleUserJoined(user);
    };

    const onUserLeft = (data: { id: string }) => {
      console.log(`👋 User ${data.id} left. Updating UI.`);
      handleUserExit({ id: data.id });

      setRemoteStreams((prev) => {
        const newMap = new Map(prev);
        newMap.delete(data.id);
        return newMap;
      });
    };

    const onNewProducer = async (data: NewProducerInfo) => {
      console.log("🎬 New producer available:", data);
      if (deviceLoaded && data.producerSocketId !== socket.id) {
        try {
          const consumer = await consume(data.producerId, roomId);
          if (consumer) {
            handleRemoteStream(consumer, data.producerSocketId);
          }
        } catch (err) {
          console.error("❌ Failed to consume new producer:", err);
        }
      }
    };

    const onProducerClosed = ({
      producerId,
      producerSocketId,
    }: {
      producerId: string;
      producerSocketId: string;
    }) => {
      console.log(
        `🎬 Producer ${producerId} from user ${producerSocketId} closed. Cleaning up consumer.`
      );
      const consumer = consumersRef.current.get(producerId);

      if (consumer) {
        consumer.close();
        consumersRef.current.delete(producerId);

        setRemoteStreams((prev) => {
          const newMap = new Map(prev);
          const stream = newMap.get(producerSocketId);
          if (stream) {
            const track = stream.getTrackById(consumer.track.id);
            if (track) {
              stream.removeTrack(track);
              console.log(
                `- Removed track ${track.id} from stream for socket ${producerSocketId}`
              );
            }
            if (stream.getTracks().length === 0) {
              newMap.delete(producerSocketId);
            }
          }
          return newMap;
        });
      }
    };

    const onError = (data: { message: string }) => setError(data.message);

    socket.on("user_joined", onUserJoined);
    socket.on("user_left", onUserLeft);
    socket.on("producer_closed", onProducerClosed);
    socket.on("new_producer", onNewProducer);
    socket.on("error", onError);

    return () => {
      socket.off("user_joined", onUserJoined);
      socket.off("user_left", onUserLeft);
      socket.off("producer_closed", onProducerClosed);
      socket.off("new_producer", onNewProducer);
      socket.off("error", onError);
    };
  }, [
    socket,
    roomId,
    consume,
    handleRemoteStream,
    handleUserJoined,
    handleUserExit,
    setError,
    deviceLoaded,
  ]);

  return (
    <div className="flex h-screen bg-gray-900 text-white font-sans">
      <aside className="w-80 flex-shrink-0">
        <ConferenceSidebar
          roomId={roomId || initialRoomId}
          isInRoom={isInRoom}
          onJoinRoom={handleJoinRoom}
          onLeaveRoom={handleLeaveRoom}
          isStaticGestureOn={isStaticGestureOn}
          setStaticGestureOn={setStaticGestureOn}
          isDynamicGestureOn={isDynamicGestureOn}
          setDynamicGestureOn={setDynamicGestureOn}
          isCameraOn={isCameraOn}
          isMicOn={isMicOn}
          onToggleCamera={toggleCamera}
          onToggleMicrophone={toggleMicrophone}
          isConnected={isConnected}
          connectionState={connectionState}
          userName={userName}
          setUserName={setUserName}
          users={users as User[]}
          error={error}
        />
      </aside>
      <main className="flex-1 flex items-center justify-center p-4 bg-black/20 overflow-hidden">
        <VideoGrid
          localStream={localStream}
          remoteStreams={remoteStreams}
          users={users as User[]}
          isStaticGestureOn={isStaticGestureOn}
          isDynamicGestureOn={isDynamicGestureOn}
        />
      </main>
    </div>
  );
};
