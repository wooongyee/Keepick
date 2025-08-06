// src/widgets/video-conference/ui/BottomControls.tsx
"use client";

import React from "react";
import Image from "next/image"; // next/image를 사용하여 최적화

// Props 인터페이스는 이전과 동일
interface BottomControlsProps {
  onLeaveRoom: () => void;
  isCameraOn: boolean;
  isMicOn: boolean;
  onToggleCamera: () => void;
  onToggleMicrophone: () => void;
}

export const BottomControls: React.FC<BottomControlsProps> = ({
  onLeaveRoom,
  isCameraOn,
  isMicOn,
  onToggleCamera,
  onToggleMicrophone,
}) => {
  return (
    <div className="flex justify-center items-center p-2">
      <div className="flex items-center space-x-4 bg-gray-800/80 backdrop-blur-sm p-3 rounded-full shadow-lg">
        {/* 카메라 버튼 */}
        <button
          onClick={onToggleCamera}
          className={`p-3 rounded-full transition-colors ${
            isCameraOn
              ? "bg-gray-600 hover:bg-gray-500"
              : "bg-red-600 hover:bg-red-500"
          }`}
        >
          <Image
            src={isCameraOn ? "/icons/camera-on.svg" : "/icons/camera-off.svg"}
            alt={isCameraOn ? "카메라 끄기" : "카메라 켜기"}
            width={24}
            height={24}
          />
        </button>

        {/* 마이크 버튼 */}
        <button
          onClick={onToggleMicrophone}
          className={`p-3 rounded-full transition-colors ${
            isMicOn
              ? "bg-gray-600 hover:bg-gray-500"
              : "bg-red-600 hover:bg-red-500"
          }`}
        >
          <Image
            src={isMicOn ? "/icons/mic-on.svg" : "/icons/mic-off.svg"}
            alt={isMicOn ? "마이크 끄기" : "마이크 켜기"}
            width={24}
            height={24}
          />
        </button>

        {/* 채팅 버튼 */}
        <button className="p-3 bg-gray-600 hover:bg-gray-500 rounded-full transition-colors">
          <Image src="/icons/chat.svg" alt="채팅 열기" width={24} height={24} />
        </button>

        {/* 나가기 버튼 */}
        <button
          onClick={onLeaveRoom}
          className="p-3 bg-red-600 hover:bg-red-700 rounded-full transition-colors"
        >
          <Image
            src="/icons/hang-up.svg"
            alt="통화 종료"
            width={24}
            height={24}
          />
        </button>
      </div>
    </div>
  );
};
