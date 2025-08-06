// src/widgets/video-conference/ui/StatusDisplay.tsx
"use client";

import React from "react";
import { UsersIcon, WifiIcon } from "@heroicons/react/24/solid";

interface StatusDisplayProps {
  isConnected: boolean;
  connectionState: string;
  users: { id: string }[];
  isInRoom: boolean;
  error: string | null;
}

export const StatusDisplay: React.FC<StatusDisplayProps> = ({
  isConnected,
  connectionState,
  users,
  isInRoom,
  error,
}) => {
  const getStatusInfo = () => {
    if (!isConnected)
      return { text: "서버 연결 중...", color: "text-yellow-400" };
    // [수정] 룸에 참가하면 '연결됨'으로, 참가 전에는 '연결 준비 완료'로 표시
    if (isInRoom) {
      switch (connectionState) {
        case "connecting":
          return { text: "미디어 서버 연결 중...", color: "text-yellow-400" };
        case "connected":
          return { text: "연결됨", color: "text-teal-400" };
        case "failed":
          return { text: "연결 실패", color: "text-red-500" };
        default:
          return { text: "연결됨", color: "text-teal-400" }; // 참가 후 기본 상태
      }
    }
    return { text: "연결 준비 완료", color: "text-teal-400" }; // 참가 전, 서버 연결 완료 상태
  };

  const status = getStatusInfo();

  return (
    <div className="space-y-4">
      <div className="p-4 bg-gray-700/50 rounded-lg flex items-center gap-4">
        <WifiIcon
          className={`w-8 h-8 flex-shrink-0 transition-colors ${status.color}`}
        />
        <div>
          <h3 className="font-bold text-white">연결 상태</h3>
          <p className={`text-sm ${status.color}`}>{status.text}</p>
        </div>
      </div>

      <div className="p-4 bg-gray-700/50 rounded-lg flex items-center gap-4">
        <UsersIcon className="w-8 h-8 flex-shrink-0 text-teal-400" />
        <div>
          <h3 className="font-bold text-white">참가자</h3>
          <p className="text-sm text-gray-300">
            현재 {users.length + (isInRoom ? 1 : 0)}명 참여 중
          </p>
        </div>
      </div>

      {error && (
        <div className="p-3 bg-red-900/50 border border-red-700 rounded-lg text-sm text-red-300">
          <span className="font-bold">에러:</span> {error}
        </div>
      )}
    </div>
  );
};
