// src/widgets/video-conference/ui/ConferenceSidebar.tsx
"use client";

import React from "react";
import {
  VideoCameraIcon,
  VideoCameraSlashIcon,
  MicrophoneIcon,
  PhoneXMarkIcon,
  ArrowRightOnRectangleIcon,
  UserIcon,
  CpuChipIcon, // AI 제스처 아이콘 추가
} from "@heroicons/react/24/solid";
import { StatusDisplay } from "./StatusDisplay";
import type { User } from "@/shared/types/webrtc";

interface ConferenceSidebarProps {
  // ... 이전과 동일한 props ...
  roomId: string;
  isInRoom: boolean;
  onJoinRoom: () => void;
  onLeaveRoom: () => void;
  isStaticGestureOn: boolean;
  setStaticGestureOn: (isOn: boolean) => void;
  isDynamicGestureOn: boolean;
  setDynamicGestureOn: (isOn: boolean) => void;
  isCameraOn: boolean;
  isMicOn: boolean;
  onToggleCamera: () => void;
  onToggleMicrophone: () => void;
  isConnected: boolean;
  connectionState: string;
  userName: string;
  setUserName: (name: string) => void;
  users: User[]; // users 타입 User[]로 명시
  error: string | null;
}

const ToggleSwitch: React.FC<{
  label: string;
  isOn: boolean;
  onToggle: () => void;
}> = ({ label, isOn, onToggle }) => (
  <div className="flex items-center justify-between">
    <span className="text-gray-300">{label}</span>
    <button
      onClick={onToggle}
      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
        isOn ? "bg-teal-500" : "bg-gray-600"
      }`}
    >
      <span
        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
          isOn ? "translate-x-6" : "translate-x-1"
        }`}
      />
    </button>
  </div>
);

export const ConferenceSidebar: React.FC<ConferenceSidebarProps> = ({
  // ... 이전과 동일한 props ...
  roomId,
  isInRoom,
  onJoinRoom,
  onLeaveRoom,
  isStaticGestureOn,
  setStaticGestureOn,
  isDynamicGestureOn,
  setDynamicGestureOn,
  isCameraOn,
  isMicOn,
  onToggleCamera,
  onToggleMicrophone,
  isConnected,
  connectionState,
  userName,
  setUserName,
  users,
  error,
}) => {
  return (
    <div className="flex flex-col h-full bg-gray-800 border-r border-gray-700">
      <header className="p-4 text-center text-xl font-semibold bg-gray-900/50 flex-shrink-0">
        ✨ <span className="font-bold text-teal-400">{roomId}</span> 그룹챗 ✨
      </header>

      <div className="flex-1 p-4 space-y-6 overflow-y-auto">
        {/* [추가] 사용자 이름 설정 섹션 */}
        <div className="p-4 bg-gray-700/50 rounded-lg">
          <label
            htmlFor="userName"
            className="flex items-center gap-2 text-sm font-semibold text-gray-400 mb-2"
          >
            <UserIcon className="w-4 h-4" />
            사용자 이름
          </label>
          <input
            id="userName"
            type="text"
            value={userName}
            onChange={(e) => setUserName(e.target.value)}
            disabled={isInRoom} // 룸에 참여하면 비활성화
            className="w-full px-3 py-2 bg-gray-800 border border-gray-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-teal-500 disabled:bg-gray-800/50 disabled:cursor-not-allowed"
            placeholder="이름을 입력하세요"
          />
        </div>

        <StatusDisplay
          isConnected={isConnected}
          connectionState={connectionState}
          users={users}
          isInRoom={isInRoom}
          error={error}
        />

        {/* [가독성 개선] AI 제스처 설정 UI 변경 */}
        <div className="p-4 border-t border-gray-700 flex-shrink-0 space-y-4">
          <div className="flex items-center gap-3">
            <CpuChipIcon className="w-6 h-6 text-teal-400" />
            <h2 className="text-lg font-bold text-white">AI 제스처 설정</h2>
          </div>
          <ToggleSwitch
            label="정적 제스처"
            isOn={isStaticGestureOn}
            onToggle={() => setStaticGestureOn(!isStaticGestureOn)}
          />
          <ToggleSwitch
            label="동적 제스처"
            isOn={isDynamicGestureOn}
            onToggle={() => setDynamicGestureOn(!isDynamicGestureOn)}
          />
        </div>
      </div>

      <div className="p-4 border-t border-gray-700 flex-shrink-0 space-y-4">
        {isInRoom && (
          <div className="p-4 bg-gray-700/50 rounded-lg">
            <h2 className="text-sm font-semibold text-gray-400 mb-3 text-center">
              디바이스 제어
            </h2>
            <div className="flex justify-around">
              <button
                onClick={onToggleCamera}
                className={`p-3 rounded-full transition-colors ${
                  isCameraOn
                    ? "bg-teal-500 text-white"
                    : "bg-gray-600 text-gray-300 hover:bg-gray-500"
                }`}
              >
                {isCameraOn ? (
                  <VideoCameraIcon className="w-6 h-6" />
                ) : (
                  <VideoCameraSlashIcon className="w-6 h-6" />
                )}
              </button>
              <button
                onClick={onToggleMicrophone}
                className={`p-3 rounded-full transition-colors ${
                  isMicOn
                    ? "bg-teal-500 text-white"
                    : "bg-gray-600 text-gray-300 hover:bg-gray-500"
                }`}
              >
                <MicrophoneIcon className="w-6 h-6" />
              </button>
            </div>
          </div>
        )}

        {!isInRoom ? (
          <button
            onClick={onJoinRoom}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 font-bold text-white bg-teal-600 rounded-lg hover:bg-teal-500 transition-colors"
          >
            <ArrowRightOnRectangleIcon className="w-6 h-6" />
            {/* [텍스트 변경] '룸 참가하기' -> '참여하기' */}
            참여하기
          </button>
        ) : (
          <button
            onClick={onLeaveRoom}
            className="w-full flex items-center justify-center gap-2 px-4 py-3 font-bold text-white bg-red-600 rounded-lg hover:bg-red-500 transition-colors"
          >
            <PhoneXMarkIcon className="w-6 h-6" />
            나가기
          </button>
        )}
      </div>
    </div>
  );
};
