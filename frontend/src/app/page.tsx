// app/page.tsx
"use client";

import { useState } from "react";
import Header from "@/components/layout/header";
import Sidebar from "@/components/layout/sidebar";
import Image from "next/image";
import Link from "next/link";
import dynamic from "next/dynamic";
import { Suspense } from "react";


// [수정] 더 명확한 아이콘들을 solid 버전으로 가져옵니다.
import {
  PlusCircleIcon,
  PhotoIcon,
  RocketLaunchIcon,
  UserPlusIcon,
} from "@heroicons/react/24/solid";

// 클라이언트 전용으로 OAuth 핸들러 로드 (SSR 비활성화)
const OAuthHandler = dynamic(
  () => import("@/features/auth/model/OAuthHandler"),
  {
    ssr: false,
  }
);

export default function HomePage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const userName = "WebrtcMaster";
  const totalPhotos = 2845;
  const totalAlbums = 17;

  return (
    <>
      {/* 토큰 파싱을 위한 클라이언트 전용 컴포넌트 */}
      <Suspense fallback={null}>
        <OAuthHandler />
      </Suspense>
      ;
      <div className="min-h-screen bg-gray-50 flex">
        <Sidebar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
        <div
          className={`flex-1 flex flex-col transition-all duration-300 ${
            sidebarOpen ? "lg:ml-64" : "ml-0"
          }`}
        >
          <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />

          {/* [수정] 메인 콘텐츠를 화면 전체에 채우도록 구조 변경 */}
          <main className="flex-1 flex items-center justify-center p-4 sm:p-6 lg:p-8">
            <section className="flex items-center justify-between gap-12 flex-wrap w-full max-w-7xl">
              <div className="flex-1 min-w-[350px] animate-fade-slide-in">
                {/* [수정] 글자 크기 및 여백 조정 */}
                <h1 className="text-4xl md:text-5xl font-bold text-gray-800 leading-tight mb-4">
                  안녕하세요,{" "}
                  <span className="text-[var(--primary-color)]">
                    {userName}
                  </span>
                  님!
                </h1>
                <p className="text-xl text-gray-500 mt-4 max-w-xl">
                  기억은 함께 나눌 때 더 선명해집니다. <br />
                  KeePick은 혼자가 아닌, 함께 정리하는 앨범 경험을 만듭니다.
                </p>
                <p className="text-lg text-gray-600 mt-6 max-w-lg">
                  지금까지 총 {totalPhotos.toLocaleString()}장의 사진과{" "}
                  {totalAlbums}개의 앨범을 만들었어요.
                  <br />
                  오늘 어떤 추억을 정리해볼까요?
                </p>

                {/* [수정] 빠른 실행 메뉴 UI 개선 (더 크고 명확하게) */}
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 mt-10 max-w-xl">
                  <Link
                    href="/albums/new"
                    className="group p-6 bg-teal-50 rounded-xl cursor-pointer hover:bg-teal-100 transition-all shadow-sm hover:shadow-lg hover:-translate-y-1"
                  >
                    <PlusCircleIcon className="h-10 w-10 text-teal-500 mb-3" />
                    <h3 className="text-xl font-bold text-teal-800">
                      새 앨범 만들기
                    </h3>
                    <p className="text-md text-teal-600 mt-1">
                      타임라인 또는 티어 앨범
                    </p>
                  </Link>
                  <Link
                    href="/photos/upload"
                    className="group p-6 bg-sky-50 rounded-xl cursor-pointer hover:bg-sky-100 transition-all shadow-sm hover:shadow-lg hover:-translate-y-1"
                  >
                    <PhotoIcon className="h-10 w-10 text-sky-500 mb-3" />
                    <h3 className="text-xl font-bold text-sky-800">
                      사진 업로드
                    </h3>
                    <p className="text-md text-sky-600 mt-1">
                      그룹 갤러리에 추가하기
                    </p>
                  </Link>
                  <Link
                    href="/group/D207"
                    className="group p-6 bg-purple-50 rounded-xl cursor-pointer hover:bg-purple-100 transition-all shadow-sm hover:shadow-lg hover:-translate-y-1"
                  >
                    <RocketLaunchIcon className="h-10 w-10 text-purple-500 mb-3" />
                    <h3 className="text-xl font-bold text-purple-800">
                      그룹 스페이스
                    </h3>
                    <p className="text-md text-purple-600 mt-1">
                      내 그룹으로 바로 이동
                    </p>
                  </Link>
                  <Link
                    href="/friends/invite"
                    className="group p-6 bg-indigo-50 rounded-xl cursor-pointer hover:bg-indigo-100 transition-all shadow-sm hover:shadow-lg hover:-translate-y-1"
                  >
                    <UserPlusIcon className="h-10 w-10 text-indigo-500 mb-3" />
                    <h3 className="text-xl font-bold text-indigo-800">
                      친구 초대하기
                    </h3>
                    <p className="text-md text-indigo-600 mt-1">
                      함께 추억을 공유해요
                    </p>
                  </Link>
                </div>
              </div>

              <div className="flex-[1.2] flex justify-center items-center min-w-[350px] animate-float">
                <div className="w-full max-w-2xl rounded-3xl shadow-2xl overflow-hidden">
                  <video
                    autoPlay
                    muted
                    loop
                    playsInline
                    className="w-full h-auto filter brightness-105"
                    poster="/placeholder.svg?height=400&width=600&text=Keepick+Demo+Video"
                  >
                    <source src="/main-video.mp4" type="video/mp4" />
                    Your browser does not support the video tag.
                  </video>
                </div>
              </div>
            </section>
          </main>
        </div>
      </div>
    </>
  );
}
