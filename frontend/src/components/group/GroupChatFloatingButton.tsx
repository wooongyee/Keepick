// src/components/group/GroupChatFloatingButton.tsx
"use client";

import Link from "next/link";
import { VideoCameraIcon } from "@heroicons/react/24/solid";

interface GroupChatFloatingButtonProps {
  groupName: string;
  isChatActive: boolean;
}

const GroupChatFloatingButton: React.FC<GroupChatFloatingButtonProps> = ({
  groupName,
  isChatActive,
}) => {
  return (
    <Link
      href={`/groupchat/${groupName}`}
      // [디자인 변경] 다크 배경에 민트 포인트 색상 적용
      className="fixed bottom-8 right-8 z-40 flex items-center gap-3 pl-4 pr-5 py-3 bg-gray-800 text-white rounded-full shadow-lg hover:bg-gray-700 transition-all hover:scale-105 hover:shadow-teal-500/20"
    >
      {/* [포인트 색상] 아이콘에 민트색 적용 */}
      <VideoCameraIcon className="w-6 h-6 text-teal-400" />
      <span className="font-bold text-base">
        {isChatActive ? "그룹챗 참여하기" : "그룹챗 시작하기"}
      </span>
    </Link>
  );
};

export default GroupChatFloatingButton;
