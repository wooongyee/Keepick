// components/layout/GroupSidebar.tsx
"use client";

import Link from "next/link";
import {
  PlusIcon,
  BookOpenIcon,
  StarIcon,
  TrophyIcon,
  XMarkIcon,
  HomeIcon,
  RectangleStackIcon,
  ChevronUpDownIcon,
} from "@heroicons/react/24/outline";
import { useState } from "react";

// [추가] AlbumType 정의
type AlbumType = "timeline" | "tier" | "highlight";

// [수정] onSelectAlbum prop 추가
interface GroupSidebarProps {
  groupName: string;
  sidebarOpen: boolean;
  setSidebarOpen: (open: boolean) => void;
  onSelectAlbum: (id: string, title: string, type: AlbumType) => void;
}

export default function GroupSidebar({
  groupName,
  sidebarOpen,
  setSidebarOpen,
  onSelectAlbum, // [수정] prop 받기
}: GroupSidebarProps) {
  const [showGroupDropdown, setShowGroupDropdown] = useState(false);

  // [수정] albums 배열 타입 명시
  const albums: { id: string; name: string; type: AlbumType }[] = [
    { id: "airport-trip", name: "김포공항에서 출발", type: "timeline" },
    { id: "best-moments", name: "제주도 제일 재밌었던 곳 뽑기!", type: "tier" },
    { id: "jeju-travel", name: "제주도 여행", type: "timeline" },
    { id: "travel-memories", name: "싸피에서의 추억", type: "tier" },
  ];

  const otherGroups = [
    { id: "구미 2반", name: "구미 2반" },
    { id: "서울 1반", name: "서울 1반" },
    { id: "부산 3반", name: "부산 3반" },
  ];

  const getIconForType = (type: string) => {
    switch (type) {
      case "timeline":
        return <BookOpenIcon className="w-5 h-5" />;
      case "tier":
        return <TrophyIcon className="w-5 h-5" />;
      case "highlight":
        return <StarIcon className="w-5 h-5" />;
      default:
        return null;
    }
  };

  return (
    <>
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/60 z-30 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
      <aside
        className={`fixed top-0 left-0 z-40 w-64 h-full bg-gradient-to-br from-gray-800 to-gray-900 text-white flex flex-col transition-transform duration-300 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <div className="h-16 flex items-center justify-between p-4 border-b border-white/10 relative">
          <button
            onClick={() => setShowGroupDropdown(!showGroupDropdown)}
            className="flex items-center gap-2 w-full text-left relative z-10"
          >
            <div className="flex-1">
              <p className="text-sm text-gray-400">현재 그룹</p>
              <h2 className="text-lg font-bold text-white truncate">
                {groupName}
              </h2>
            </div>
            <ChevronUpDownIcon className="w-5 h-5 text-gray-400" />
          </button>
          <button
            onClick={() => setSidebarOpen(false)}
            className="text-white/80 hover:text-white lg:hidden ml-2 z-10"
          >
            <XMarkIcon className="h-6 w-6" />
          </button>
          {showGroupDropdown && (
            <div className="absolute top-full left-0 w-full bg-gray-700 rounded-md shadow-lg py-2 z-20">
              <span className="block px-4 py-2 text-xs text-gray-400 uppercase">
                다른 그룹
              </span>
              {otherGroups.map((group) => (
                <Link
                  key={group.id}
                  href={`/group/${encodeURIComponent(group.id)}`}
                  onClick={() => {
                    setShowGroupDropdown(false);
                    setSidebarOpen(false);
                  }}
                  className="block px-4 py-2 text-sm text-white hover:bg-gray-600 truncate"
                >
                  {group.name}
                </Link>
              ))}
            </div>
          )}
        </div>
        <nav className="flex-1 p-4 overflow-y-auto">
          <ul className="space-y-1 mb-6">
            <li>
              <Link
                href={`/group/${encodeURIComponent(groupName)}`}
                onClick={() => setSidebarOpen(false)}
                className="flex items-center gap-3 p-3 rounded-lg font-medium text-gray-300 bg-white/5 hover:bg-white/10 hover:text-white transition-colors"
              >
                <HomeIcon className="w-5 h-5" />
                <span className="flex-1 truncate text-sm">홈</span>
              </Link>
            </li>
            <li>
              <Link
                href={`/group/${encodeURIComponent(groupName)}/gallery`}
                onClick={() => setSidebarOpen(false)}
                className="flex items-center gap-3 p-3 rounded-lg font-medium text-gray-300 hover:bg-white/10 hover:text-white transition-colors"
              >
                <RectangleStackIcon className="w-5 h-5" />
                <span className="flex-1 truncate text-sm">갤러리</span>
              </Link>
            </li>
          </ul>
          <span className="text-gray-400 text-sm font-semibold uppercase tracking-wider px-2">
            앨범 목록
          </span>
          <ul className="space-y-1 mt-2">
            {albums.map((album) => (
              <li key={album.id}>
                <a // Link 대신 a 태그 사용 또는 Link의 onClick 로직 수정
                  href={`/group/${encodeURIComponent(groupName)}?album=${
                    album.id
                  }&type=${album.type}`}
                  onClick={(e) => {
                    e.preventDefault(); // 기본 페이지 이동 방지
                    onSelectAlbum(album.id, album.name, album.type); // 부모 함수 호출
                    setSidebarOpen(false); // 사이드바 닫기
                  }}
                  className="flex items-center gap-3 p-3 rounded-lg font-medium text-gray-300 hover:bg-white/10 hover:text-white transition-colors cursor-pointer"
                >
                  <span className="text-gray-400">
                    {getIconForType(album.type)}
                  </span>
                  <span className="flex-1 truncate text-sm">{album.name}</span>
                </a>
              </li>
            ))}
          </ul>
        </nav>
        <div className="mt-auto p-4 border-t border-white/10 space-y-4">
          <Link
            href={`/group/${encodeURIComponent(groupName)}/new-album`}
            onClick={() => setSidebarOpen(false)}
            className="flex items-center justify-center gap-2 w-full px-4 py-2.5 bg-teal-500 text-white rounded-lg font-semibold hover:bg-teal-400 transition-all"
          >
            <PlusIcon className="w-5 h-5" />새 앨범 만들기
          </Link>
        </div>
      </aside>
    </>
  );
}
