// app/group/[groupName]/page.tsx
"use client";

import { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import Header from "@/components/layout/header";
import GroupSidebar from "@/components/layout/GroupSidebar";
import TierAlbumView from "@/components/group/TierAlbumView";
import TimelineAlbumView from "@/components/group/TimelineAlbumView";
import HighlightAlbumView from "@/components/group/HighlightAlbumView";
import { PlusIcon } from "@heroicons/react/24/solid";
import Image from "next/image";
import GroupChatFloatingButton from "@/components/group/GroupChatFloatingButton";

type AlbumType = "timeline" | "tier" | "highlight";

// ✅ Next.js 15 호환: params를 Promise 타입으로 변경
interface PageProps {
  params: Promise<{ groupName: string }>;
}

export default function GroupPage({ params }: PageProps) {
  const searchParams = useSearchParams();
  const [groupName, setGroupName] = useState<string>("");
  const [encodedGroupName, setEncodedGroupName] = useState<string>("");
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [activeTab, setActiveTab] = useState<AlbumType>("tier");
  const [selectedAlbum, setSelectedAlbum] = useState<{
    id: string;
    title: string;
    type: AlbumType;
  } | null>(null);

  const [isChatActive, setIsChatActive] = useState(false);

  // ✅ useEffect에서 params Promise 해결
  useEffect(() => {
    const resolveParams = async () => {
      try {
        const resolvedParams = await params;
        const decodedName = decodeURIComponent(resolvedParams.groupName);
        setGroupName(decodedName);
        setEncodedGroupName(resolvedParams.groupName);
      } catch (error) {
        console.error("Error resolving params:", error);
      }
    };

    resolveParams();
  }, [params]);

  useEffect(() => {
    if (groupName) {
      setIsChatActive(Math.random() > 0.5);
    }
  }, [groupName]);

  useEffect(() => {
    if (!searchParams) return;

    const albumId = searchParams.get("album");
    const albumType = searchParams.get("type") as AlbumType;

    const albumsData: { [key: string]: { title: string; type: AlbumType } } = {
      "airport-trip": { title: "김포공항에서 출발", type: "timeline" },
      "jeju-travel": { title: "제주도 여행", type: "timeline" },
      "best-moments": { title: "제주도 제일 재밌었던 곳 뽑기!", type: "tier" },
      "travel-memories": { title: "싸피에서의 추억", type: "tier" },
      "family-gathering": { title: "도쿄여행에서 먹은 음식", type: "tier" },
      "highlight-1": { title: "가족 추억", type: "highlight" },
      "highlight-2": { title: "프로젝트 회고", type: "highlight" },
      "highlight-3": { title: "반려동물 일상", type: "highlight" },
      "highlight-4": { title: "팀 빌딩 워크샵", type: "highlight" },
    };

    if (albumId && albumType && albumsData[albumId]) {
      const albumInfo = albumsData[albumId];
      setSelectedAlbum({
        id: albumId,
        title: albumInfo.title,
        type: albumType,
      });
    } else {
      setSelectedAlbum(null);
    }
  }, [searchParams]);

  useEffect(() => {
    if (selectedAlbum) {
      setSidebarOpen(false);
    } else {
      setSidebarOpen(true);
    }
  }, [selectedAlbum]);

  const handleSelectAlbum = (id: string, title: string, type: AlbumType) => {
    if (!encodedGroupName) return;
    const newUrl = `/group/${encodedGroupName}?album=${id}&type=${type}`;
    window.history.pushState({ path: newUrl }, "", newUrl);
    setSelectedAlbum({ id, title, type });
  };

  const handleBackToList = () => {
    if (!encodedGroupName) return;
    const newUrl = `/group/${encodedGroupName}`;
    window.history.pushState({ path: newUrl }, "", newUrl);
    setSelectedAlbum(null);
  };

  const renderTimelineAlbumList = () => {
    const albums = [
      {
        id: "airport-trip",
        title: "김포공항에서 출발",
        date: "2025.06.25",
        coverImage: "/airport-dummy1.jpg",
      },
      {
        id: "jeju-travel",
        title: "제주도 여행",
        date: "2025.07.10",
        coverImage: "/jeju-dummy1.webp",
      },
    ];
    return (
      <div className="space-y-6 animate-fade-in">
        <div className="flex justify-between items-center">
          <h3 className="text-2xl font-bold text-gray-800">타임라인 앨범</h3>
          <button className="flex items-center gap-2 px-4 py-2 bg-teal-500 text-white rounded-lg font-semibold hover:bg-teal-600 transition-colors">
            <PlusIcon className="w-5 h-5" /> 새 앨범
          </button>
        </div>
        <div className="grid grid-cols-[repeat(auto-fill,minmax(320px,1fr))] gap-6">
          {albums.map((album) => (
            <div
              key={album.id}
              onClick={() =>
                handleSelectAlbum(album.id, album.title, "timeline")
              }
              className="bg-white rounded-xl shadow-md border overflow-hidden cursor-pointer group hover:shadow-xl hover:-translate-y-1 transition-all"
            >
              <div className="relative h-48">
                <Image
                  src={album.coverImage}
                  alt={album.title}
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  className="object-cover group-hover:scale-105 transition-transform duration-300"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
                <h4 className="absolute bottom-4 left-4 text-white text-xl font-bold">
                  {album.title}
                </h4>
              </div>
              <p className="p-4 text-sm text-gray-500">{album.date}</p>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const renderTierAlbumList = () => {
    const tierAlbums = [
      {
        id: "best-moments",
        title: "제주도 제일 재밌었던 곳 뽑기!",
        coverImage: "/jeju-dummy2.jpg",
        date: "2025.07.25",
      },
      {
        id: "travel-memories",
        title: "싸피에서의 추억",
        coverImage: "/jaewan1.jpg",
        date: "2025.06.10",
      },
      {
        id: "family-gathering",
        title: "도쿄여행에서 먹은 음식",
        coverImage: "/food-dummy1.jpg",
        date: "2025.08.15",
      },
    ];
    return (
      <div className="animate-fade-in">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-2xl font-bold text-gray-800">티어 앨범 목록</h3>
          <button className="flex items-center gap-2 px-4 py-2 bg-teal-500 text-white rounded-lg font-semibold hover:bg-teal-600 transition-colors">
            <PlusIcon className="w-5 h-5" /> 새 앨범
          </button>
        </div>
        <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-6">
          {tierAlbums.map((album) => (
            <div
              key={album.id}
              onClick={() => handleSelectAlbum(album.id, album.title, "tier")}
              className="bg-white rounded-xl shadow-md border overflow-hidden cursor-pointer group hover:shadow-xl hover:-translate-y-1 transition-all"
            >
              <div className="relative h-48">
                <Image
                  src={album.coverImage}
                  alt={album.title}
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  className="object-cover group-hover:scale-105 transition-transform duration-300"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
                <h4 className="absolute bottom-4 left-4 text-white text-xl font-bold">
                  {album.title}
                </h4>
              </div>
              <p className="p-4 text-sm text-gray-500">{album.date}</p>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const renderHighlightAlbumList = () => {
    const highlightAlbums = [
      {
        id: "highlight-1",
        title: "가족 추억",
        date: "2024.05.01",
        coverImage: "/jeju-dummy3.jpg",
      },
      {
        id: "highlight-2",
        title: "프로젝트 회고",
        date: "2024.06.15",
        coverImage: "/jeju-dummy4.jpg",
      },
      {
        id: "highlight-3",
        title: "반려동물 일상",
        date: "2024.07.01",
        coverImage: "/jeju-dummy5.jpg",
      },
      {
        id: "highlight-4",
        title: "팀 빌딩 워크샵",
        date: "2024.07.20",
        coverImage: "/jeju-dummy6.jpg",
      },
    ];
    return (
      <div className="animate-fade-in">
        <div className="flex justify-between items-center mb-6">
          <h3 className="text-2xl font-bold text-gray-800">하이라이트 앨범</h3>
          <button className="flex items-center gap-2 px-4 py-2 bg-teal-500 text-white rounded-lg font-semibold hover:bg-teal-600 transition-colors">
            <PlusIcon className="w-5 h-5" /> 새 앨범
          </button>
        </div>
        <div className="grid grid-cols-[repeat(auto-fill,minmax(300px,1fr))] gap-6">
          {highlightAlbums.map((album) => (
            <div
              key={album.id}
              onClick={() =>
                handleSelectAlbum(album.id, album.title, "highlight")
              }
              className="bg-white rounded-xl shadow-md border overflow-hidden cursor-pointer group hover:shadow-xl hover:-translate-y-1 transition-all"
            >
              <div className="relative h-48">
                <Image
                  src={album.coverImage}
                  alt={album.title}
                  fill
                  sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
                  className="object-cover group-hover:scale-105 transition-transform duration-300"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 to-transparent"></div>
                <h4 className="absolute bottom-4 left-4 text-white text-xl font-bold">
                  {album.title}
                </h4>
              </div>
              <p className="p-4 text-sm text-gray-500">{album.date}</p>
            </div>
          ))}
        </div>
      </div>
    );
  };

  const renderActiveAlbumView = () => {
    if (!selectedAlbum) return null;
    switch (selectedAlbum.type) {
      case "tier":
        return (
          <TierAlbumView
            albumId={selectedAlbum.id}
            albumTitle={selectedAlbum.title}
            onBack={handleBackToList}
          />
        );
      case "timeline":
        return (
          <TimelineAlbumView
            albumId={selectedAlbum.id}
            albumTitle={selectedAlbum.title}
            onBack={handleBackToList}
          />
        );
      case "highlight":
        return (
          <HighlightAlbumView
            albumId={selectedAlbum.id}
            albumTitle={selectedAlbum.title}
            onBack={handleBackToList}
          />
        );
      default:
        return null;
    }
  };

  // ✅ 데이터가 로드되지 않았을 때 로딩 처리
  if (!groupName || !encodedGroupName) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-teal-500"></div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <GroupSidebar
        groupName={groupName}
        sidebarOpen={sidebarOpen}
        setSidebarOpen={setSidebarOpen}
        onSelectAlbum={handleSelectAlbum}
      />
      <div
        className={`flex-1 transition-all duration-300 ${
          sidebarOpen ? "lg:ml-64" : "ml-0"
        }`}
      >
        <Header
          onMenuClick={() => setSidebarOpen(!sidebarOpen)}
          onBackToDashboard={() => (window.location.href = "/")}
        />
        <main className="p-6 sm:p-8">
          {selectedAlbum ? (
            renderActiveAlbumView()
          ) : (
            <>
              <div className="flex justify-between items-center mb-8 flex-wrap gap-4">
                <h1 className="text-3xl font-bold text-gray-900">
                  {groupName} 그룹스페이스
                </h1>
              </div>
              <div className="flex items-center justify-between border-b border-gray-200 mb-8">
                <div className="flex gap-2">
                  <button
                    onClick={() => setActiveTab("timeline")}
                    className={`px-4 py-3 font-semibold ${
                      activeTab === "timeline"
                        ? "border-b-2 border-teal-500 text-teal-600"
                        : "text-gray-500"
                    }`}
                  >
                    📅 타임라인
                  </button>
                  <button
                    onClick={() => setActiveTab("tier")}
                    className={`px-4 py-3 font-semibold ${
                      activeTab === "tier"
                        ? "border-b-2 border-teal-500 text-teal-600"
                        : "text-gray-500"
                    }`}
                  >
                    🏆 티어
                  </button>
                  <button
                    onClick={() => setActiveTab("highlight")}
                    className={`px-4 py-3 font-semibold ${
                      activeTab === "highlight"
                        ? "border-b-2 border-teal-500 text-teal-600"
                        : "text-gray-500"
                    }`}
                  >
                    ✨ 하이라이트
                  </button>
                </div>
              </div>
              <div>
                {activeTab === "timeline" && renderTimelineAlbumList()}
                {activeTab === "tier" && renderTierAlbumList()}
                {activeTab === "highlight" && renderHighlightAlbumList()}
              </div>
            </>
          )}
        </main>
      </div>

      <GroupChatFloatingButton
        groupName={encodedGroupName}
        isChatActive={isChatActive}
      />
    </div>
  );
}
