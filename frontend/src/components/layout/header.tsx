"use client";

import { useState } from "react";
import { useLogout } from "@/features/auth/model/useLogout";

import Link from "next/link";
import {
  Bars3Icon,
  BellIcon,
  UserCircleIcon,
  ArrowRightOnRectangleIcon,
  ArrowLeftStartOnRectangleIcon, // [추가] 대시보드 이동 아이콘
} from "@heroicons/react/24/outline";
import Image from "next/image";
import { useAppSelector } from "@/shared/config/hooks";

interface HeaderProps {
  onMenuClick?: () => void;
  onBackToDashboard?: () => void; // [추가] 대시보드로 돌아가기 함수 prop
}

export default function Header({
  onMenuClick,
  onBackToDashboard,
}: HeaderProps) {
  const [isProfileMenuOpen, setProfileMenuOpen] = useState(false);
  const { currentUser } = useAppSelector((state) => state.user);
  const { isAuthenticated } = useAppSelector((state) => state.auth);

  const logout = useLogout();

  // 실제로는 인증 상태에서 가져올 데이터
  // const user = { name: "wmwogus", imageUrl: "/jaewan1.jpg" };
  const user = {
    name: currentUser?.nickname || "Guest",
    imageUrl: currentUser?.profileUrl || "/jaewan1.jpg",
  };

  return (
    <header className="sticky top-0 z-30 bg-white/80 backdrop-blur-sm shadow-sm h-16">
      <div className="flex items-center justify-between h-full px-4 sm:px-6 lg:px-8">
        {/* Left side */}
        <div className="flex items-center gap-4">
          {/* 사이드바 메뉴 버튼 (onMenuClick prop이 있을 때만 렌더링) */}
          {onMenuClick ? (
            <button
              onClick={onMenuClick}
              className="text-gray-600 hover:text-[var(--primary-color)]"
            >
              <span className="sr-only">메뉴 열기</span>
              <Bars3Icon className="h-7 w-7" />
            </button>
          ) : (
            // 메뉴 버튼이 없을 때도 레이아웃 유지를 위한 빈 공간
            <div className="w-7 h-7"></div>
          )}

          {/* 로고 */}
          <Link
            href="/"
            className="font-montserrat font-bold text-2xl text-[var(--primary-color)]"
          >
            Keep<span className="text-[var(--text-dark)] ml-1">ick</span>
          </Link>

          {/* [추가] 대시보드 복귀 버튼 (onBackToDashboard prop이 있을 때만 렌더링) */}
          {onBackToDashboard && (
            <>
              <div className="h-6 w-px bg-gray-200"></div> {/* 시각적 구분선 */}
              <button
                onClick={onBackToDashboard}
                className="flex items-center gap-2 text-gray-600 hover:text-[var(--primary-color)] transition-colors"
              >
                <ArrowLeftStartOnRectangleIcon className="h-6 w-6" />
                <span className="font-semibold hidden sm:block">대시보드</span>
              </button>
            </>
          )}
        </div>

        {/* Right side: Actions */}
        <div className="flex items-center gap-4">
          <button className="relative text-gray-600 hover:text-[var(--primary-color)] p-2 rounded-full hover:bg-gray-100">
            <span className="sr-only">알림</span>
            <BellIcon className="h-6 w-6" />
            <span className="absolute top-1 right-1 block h-2 w-2 rounded-full bg-red-500" />
          </button>

          <div className="relative">
            <button
              onClick={() => setProfileMenuOpen(!isProfileMenuOpen)}
              className="flex items-center gap-2 rounded-full p-1 pr-3 hover:bg-gray-100 transition-colors"
            >
              <Image
                src={user.imageUrl}
                alt="프로필 사진"
                width={32}
                height={32}
                className="rounded-full w-8 h-8 object-cover"
                quality={90}
              />
              <span className="font-semibold text-sm text-gray-800 hidden sm:block">
                {user.name}
              </span>
            </button>
            {isProfileMenuOpen && (
              <div
                className="absolute right-0 mt-2 w-48 bg-white rounded-lg shadow-xl py-2 z-50 border"
                onMouseLeave={() => setProfileMenuOpen(false)}
              >
                <div className="px-4 py-2 border-b mb-2">
                  <p className="font-bold text-sm text-gray-800">{user.name}</p>
                  <p className="text-xs text-gray-500">반갑습니다!</p>
                </div>
                <Link
                  href="/profile"
                  className="flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                >
                  <UserCircleIcon className="w-5 h-5" /> 프로필
                </Link>
                <button onClick={logout} className="w-full flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50">
                  <ArrowRightOnRectangleIcon className="w-5 h-5" /> 로그아웃
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
