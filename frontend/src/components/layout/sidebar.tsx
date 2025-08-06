// components/layout/Sidebar.tsx
"use client";

import Link from "next/link";
import { XMarkIcon, PlusIcon } from "@heroicons/react/24/outline";

interface SidebarProps {
  sidebarOpen: boolean;
  setSidebarOpen: (open: boolean) => void;
  activeGroup?: string;
}

export default function Sidebar({
  sidebarOpen,
  setSidebarOpen,
  activeGroup,
}: SidebarProps) {
  const groups = [
    { id: "d207", name: "D207", icon: "👨‍👩‍👧‍👦", count: 5 },
    { id: "college", name: "대학 친구들", icon: "🎓", count: 5 },
    { id: "work", name: "회사 동료", icon: "💼", count: 5 },
  ];

  return (
    <>
      {/* Mobile Overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/60 z-40 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* [변경] Sidebar 디자인 및 구조 */}
      <aside
        className={`fixed top-0 left-0 z-50 w-64 h-full bg-gradient-to-br from-gray-800 to-gray-900 text-white flex flex-col transition-transform duration-300 ${
          sidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        {/* Sidebar Header (메인 헤더와 높이 맞춤) */}
        <div className="flex items-center justify-between p-4 h-16 border-b border-white/10">
          <Link
            href="/"
            className="font-montserrat font-bold text-2xl text-white"
          >
            Keepick
          </Link>
          <button
            onClick={() => setSidebarOpen(false)}
            className="text-white/80 hover:text-white lg:hidden p-1 rounded-full hover:bg-white/10"
          >
            <span className="sr-only">사이드바 닫기</span>
            <XMarkIcon className="h-6 w-6" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 p-4 overflow-y-auto">
          <div className="flex items-center justify-between mb-4">
            <span className="text-gray-400 text-sm font-semibold uppercase tracking-wider">
              내 그룹
            </span>
            <button className="flex items-center gap-1 text-sm text-gray-300 hover:text-white font-semibold">
              <PlusIcon className="w-4 h-4" />
              추가
            </button>
          </div>
          <ul className="space-y-1">
            {groups.map((group) => (
              <li key={group.id}>
                {/* [변경] 동적 라우팅 링크 적용 */}
                <Link
                  href={`/group/${encodeURIComponent(group.name)}`}
                  className={`flex items-center gap-3 p-3 rounded-lg font-semibold transition-all ${
                    activeGroup === group.id
                      ? "bg-white/10 text-white" // 활성화 스타일
                      : "text-gray-300 hover:bg-white/10 hover:text-white"
                  }`}
                >
                  <span className="text-xl">{group.icon}</span>
                  <span className="flex-1 truncate">{group.name}</span>
                  <span className="bg-white/20 text-white text-xs font-bold px-2 py-0.5 rounded-full">
                    {group.count}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </aside>
    </>
  );
}
