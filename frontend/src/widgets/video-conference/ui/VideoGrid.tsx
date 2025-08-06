// src/widgets/video-conference/ui/VideoGrid.tsx
"use client";

import React, { useEffect, useRef } from "react";
import type { User } from "@/shared/types/webrtc";
import { GestureRecognizer } from "./GestureRecognizer";
import { UserCircleIcon } from "@heroicons/react/24/solid"; // UserCircleIcon 추가

/**
 * 원격 비디오를 렌더링하는 가장 안정적인 최종 컴포넌트
 */
const RemoteVideo: React.FC<{ stream: MediaStream; userId: string }> = ({
  stream,
  userId,
}) => {
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const videoElement = videoRef.current;
    if (!videoElement) return;

    if (videoElement.srcObject !== stream) {
      videoElement.srcObject = stream;
    }

    const handleCanPlay = async () => {
      try {
        videoElement.muted = true; // 초기에는 음소거
        await videoElement.play();
        // 이후 로직에서 필요에 따라 음소거 해제 가능 (예: 사용자가 음소거 해제 버튼 클릭 시)
        console.log(`✅✅✅ Successfully played video for ${userId}.`);
      } catch (error) {
        console.error(`❌❌❌ FAILED to play video for ${userId}`, error);
      }
    };

    videoElement.addEventListener("canplay", handleCanPlay, { once: true });
    return () => {
      videoElement.removeEventListener("canplay", handleCanPlay);
    };
  }, [stream, userId]);

  return (
    <video
      ref={videoRef}
      autoPlay
      playsInline
      muted // 원격 비디오는 기본적으로 음소거로 유지 (충돌 방지)
      className="w-full h-full object-cover rounded-lg shadow-lg" // Tailwind CSS 클래스 사용
    />
  );
};

interface VideoGridProps {
  localStream: MediaStream | null;
  remoteStreams: Map<string, MediaStream>;
  users: User[];
  isStaticGestureOn: boolean;
  isDynamicGestureOn: boolean;
}

export const VideoGrid: React.FC<VideoGridProps> = ({
  localStream,
  remoteStreams,
  users,
  isStaticGestureOn,
  isDynamicGestureOn,
}) => {
  const totalUsers = (users?.length || 0) + (localStream ? 1 : 0);

  // 혼자 있을 때의 UI (localStream이 있고 remoteStream이 없을 때)
  if (localStream && remoteStreams.size === 0) {
    return (
      <div className="w-full h-full flex items-center justify-center p-4">
        <div className="relative w-full max-w-4xl aspect-video bg-gray-800 rounded-lg overflow-hidden shadow-2xl">
          {/* 로컬 스트림을 GestureRecognizer로 감싸서 사용 */}
          <GestureRecognizer
            mediaStream={localStream}
            isStaticOn={isStaticGestureOn}
            isDynamicOn={isDynamicGestureOn}
          />
          <div className="absolute bottom-2 left-2 px-3 py-1 bg-black bg-opacity-50 rounded-md text-sm font-semibold text-white">
            📹 나 (You)
          </div>
        </div>
      </div>
    );
  }

  // 로컬 스트림도 없고 원격 스트림도 없을 때 (아무도 참여하지 않았을 때)
  if (!localStream && remoteStreams.size === 0) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center text-gray-400">
        <UserCircleIcon className="w-24 h-24 text-gray-600 mb-4" />
        <h2 className="text-2xl font-bold text-white">그룹챗 대기 중</h2>
        <p className="mt-2 max-w-sm text-gray-400">
          사이드바의 '참여하기' 버튼을 눌러 그룹챗을 시작하세요!
        </p>
      </div>
    );
  }

  // 여러 명 있을 때: 기존 그리드 레이아웃 사용
  const getGridClass = () => {
    if (totalUsers <= 1) return "grid-cols-1"; // 이 경우는 위에서 이미 처리됨
    if (totalUsers === 2) return "grid-cols-1 md:grid-cols-2";
    if (totalUsers <= 4) return "grid-cols-2";
    if (totalUsers <= 6) return "grid-cols-2 lg:grid-cols-3";
    if (totalUsers <= 9) return "grid-cols-3";
    return "grid-cols-3 lg:grid-cols-4"; // 9명 초과 시
  };

  return (
    <div className={`grid ${getGridClass()} gap-4 w-full h-full items-center`}>
      {localStream && (
        <div className="relative aspect-video bg-black rounded-lg overflow-hidden shadow-lg">
          <GestureRecognizer
            mediaStream={localStream}
            isStaticOn={isStaticGestureOn}
            isDynamicOn={isDynamicGestureOn}
          />
          <div className="absolute bottom-2 left-2 px-2 py-1 bg-black bg-opacity-50 rounded-md text-sm font-semibold">
            {/* [수정] 부모로부터 받은 로컬 사용자 이름으로 표시 */}
            📹 나 (You)
          </div>
        </div>
      )}

      {/* 원격 사용자 비디오 */}
      {users.map((user) => {
        const stream = remoteStreams.get(user.id);
        const hasVideo = stream && stream.getVideoTracks().length > 0;
        return (
          <div
            key={user.id}
            className="relative aspect-video bg-black rounded-lg overflow-hidden shadow-lg"
          >
            {stream && hasVideo ? (
              <RemoteVideo stream={stream} userId={user.id} />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-white bg-gray-700">
                <UserCircleIcon className="w-16 h-16 text-gray-500 mb-2" />
                {/* [수정] ID 대신 이름 표시 */}
                <span className="text-lg font-semibold">{user.name}</span>
              </div>
            )}
            <div className="absolute bottom-2 left-2 px-2 py-1 bg-black bg-opacity-50 rounded-md text-sm font-semibold">
              {/* [수정] ID 대신 이름 표시 */}
              📺 {user.name}
            </div>
          </div>
        );
      })}
    </div>
  );
};
