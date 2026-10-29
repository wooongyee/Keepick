// src/app/[groupId]/_components/ConferenceClientPage.tsx (AI 초기화 부분 수정)

"use client";

import { useEffect } from "react";
import { useAppDispatch, useAppSelector } from "@/shared/config/hooks";
import { ConferenceLayout } from "@/widgets/video-conference/ConferenceLayout";
import { Lobby } from "@/widgets/video-conference/lobby/ui/Lobby";
import { mediasoupManager } from "@/shared/api/mediasoupManager";
import { initializeSocketApi, webrtcHandler, chatHandler, aiEventHandler } from "@/shared/api/socket";
import { setRoomId } from "@/entities/video-conference/session/model/slice";
import { joinRoomThunk } from "@/entities/video-conference/session/model/thunks";
import { frontendAiProcessor } from "@/shared/api/ai"; // frontendAiProcessor 임포트 유지
import { mapGestureToEmoji, mapEmotionToEmoji, generateReactionId } from "@/shared/utils/aiEmojiMapper";
import type { GestureResult, EmotionResult } from "@/shared/types/ai.types";


interface ConferenceClientPageProps {
  roomId: string;
}

export const ConferenceClientPage = ({ roomId }: ConferenceClientPageProps) => {
  const dispatch = useAppDispatch();
  const { isInRoom, error, status, userName } = useAppSelector((state) => state.session);
  const aiState = useAppSelector((state) => state.ai);
  const isJoining = status === "pending";
  const hasLocalVideo = useAppSelector(state => !!state.media.local.tracks.video);

  useEffect(() => {
    let cancelled = false;
    const initializeSystems = async () => {
      try {
        dispatch(setRoomId(roomId));
        await mediasoupManager.init(dispatch);
        if (cancelled) return;
        initializeSocketApi(dispatch);

      } catch (e) {
        console.error("❌ Failed to initialize systems:", e);
        // TODO: UI에 에러 메시지를 표시하는 로직 추가
      }
    };

    initializeSystems();

    return () => {
      cancelled = true;
      console.log("🧹 Cleaning up conference page resources...");
      webrtcHandler.leaveRoom();
      chatHandler.leaveChat({ roomId });
      aiEventHandler.cleanup();
      // mediasoupManager.cleanup owns model and track cleanup.
      // 로비에서 설정한 AI 상태를 유지하기 위해 setAiEnabled(false) 제거
    };
  }, [dispatch, roomId]);

  // Lobby owns preview callbacks; entering the conference restores its owner.
  useEffect(() => {
    if (!isInRoom) return;
        // AI 이벤트 핸들러 설정
        console.log("🚀 Setting up AI handlers...");
        aiEventHandler.setRoomId(roomId);
        

        // AI 콜백 설정 (화상회의용)
        frontendAiProcessor.setGestureCallback((result: GestureResult) => {
          console.log("🤖 Conference Gesture Result:", result);
          
          // 정적 제스처 처리 (임계값 낮춤)
          if (result.static && result.static.label !== "none" && result.static.confidence > 0.3) {
            const emoji = mapGestureToEmoji(result.static.label);
            const reaction = {
              id: generateReactionId(),
              emoji,
              userId: "local",
              userName: userName || "익명",
              timestamp: Date.now(),
              duration: 2000,
              source: 'ai' as const,
              aiType: 'gesture' as const,
              confidence: result.static.confidence
            };
            
            aiEventHandler.sendAiReaction(reaction);
            console.log("✅ Static gesture reaction added:", reaction);
          }
          
          // 동적 제스처 처리 (임계값 낮춤)
          if (result.dynamic && result.dynamic.label !== "none" && result.dynamic.confidence > 0.3) {
            const emoji = mapGestureToEmoji(result.dynamic.label);
            const reaction = {
              id: generateReactionId(),
              emoji,
              userId: "local",
              userName: userName || "익명",
              timestamp: Date.now(),
              duration: 2000,
              source: 'ai' as const,
              aiType: 'gesture' as const,
              confidence: result.dynamic.confidence
            };
            
            aiEventHandler.sendAiReaction(reaction);
            console.log("✅ Dynamic gesture reaction added:", reaction);
          }
        });

        frontendAiProcessor.setEmotionCallback((result: EmotionResult) => {
          console.log("😊 Conference Emotion Result:", result);
          
          if (result.label !== "none" && result.confidence > 0.3) {
            const emoji = mapEmotionToEmoji(result.label);
            const reaction = {
              id: generateReactionId(),
              emoji,
              userId: "local",
              userName: userName || "익명",
              timestamp: Date.now(),
              duration: 2500,
              source: 'ai' as const,
              aiType: 'emotion' as const,
              confidence: result.confidence
            };
            
            aiEventHandler.sendAiReaction(reaction);
            console.log("✅ Emotion reaction added:", reaction);
          }
        });
        
        console.log("✅ AI handlers set up successfully.");
    return () => {
      aiEventHandler.setRoomId("");
      frontendAiProcessor.setGestureCallback(null);
      frontendAiProcessor.setEmotionCallback(null);
    };
  }, [dispatch, roomId, userName, isInRoom]);

  // One owner applies Redux intent after the camera exists. Track identity is
  // deliberately not a dependency: AI replacement changes that identity.
  useEffect(() => {
    if (!isInRoom || !hasLocalVideo) return;
    let cancelled = false;
    void mediasoupManager.toggleAIDuringConference(aiState.isAiEnabled, {
      gesture: {
        static: { enabled: aiState.isStaticGestureDetectionEnabled, confidence: 0.7 },
        dynamic: { enabled: aiState.isDynamicGestureDetectionEnabled, confidence: 0.7 },
      },
      emotion: { enabled: aiState.isEmotionDetectionEnabled, confidence: 0.5 },
      beauty: { ...aiState.beautyFilterConfig, enabled: aiState.isBeautyFilterEnabled },
    }).catch(error => {
      if (!cancelled) console.warn("AI 전환 실패; 기본 영상 유지", error);
    });
    return () => { cancelled = true; };
  }, [isInRoom, hasLocalVideo, aiState.isAiEnabled, aiState.isStaticGestureDetectionEnabled,
    aiState.isDynamicGestureDetectionEnabled, aiState.isEmotionDetectionEnabled,
    aiState.isBeautyFilterEnabled, aiState.beautyFilterConfig]);

  const handleJoin = async (userName: string) => {
    if (roomId && userName) {
      try {
        console.log(`🚀 Joining room: ${roomId}, user: ${userName}`);
        dispatch(joinRoomThunk({ roomId, userName }));
      } catch (e) {
        console.error("❌ Failed to join room:", e);
      }
    }
  };

  if (!isInRoom) {
    return <Lobby onJoin={handleJoin} isLoading={isJoining} error={error} />;
  }

  return <ConferenceLayout />;
};
