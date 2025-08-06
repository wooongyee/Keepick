// components/group/TierAlbumView.tsx
"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import { ArrowUturnLeftIcon, BoltIcon } from "@heroicons/react/24/outline";
import TierBattleModal from "./TierBattleModal";

// 타입 정의
interface Photo {
  id: string;
  src: string;
  name: string;
}
interface BattleSequence {
  newPhoto: Photo;
  opponents: Photo[];
  currentOpponentIndex: number;
  targetTier: string;
  targetIndex: number;
  sourceType: string;
}
interface TierAlbumViewProps {
  albumId: string;
  albumTitle: string;
  onBack: () => void;
}

export default function TierAlbumView({
  albumId,
  albumTitle,
  onBack,
}: TierAlbumViewProps) {
  // --- 상태 관리 ---
  const [tierPhotos, setTierPhotos] = useState<{ [key: string]: Photo[] }>({});
  const [availablePhotos, setAvailablePhotos] = useState<Photo[]>([]);
  const [precisionTierMode, setPrecisionTierMode] = useState(false);
  const [showImageModal, setShowImageModal] = useState(false);
  const [selectedImage, setSelectedImage] = useState<Photo | null>(null);
  const [showComparisonModal, setShowComparisonModal] = useState(false);
  const [battleSequence, setBattleSequence] = useState<BattleSequence | null>(
    null
  );
  const [dragOverPosition, setDragOverPosition] = useState<{
    tier: string;
    index: number;
  } | null>(null);
  const [draggingPhotoId, setDraggingPhotoId] = useState<string | null>(null);

  const tiers = [
    { label: "S", color: "from-amber-300 to-yellow-400" },
    { label: "A", color: "from-sky-300 to-blue-500" },
    { label: "B", color: "from-teal-300 to-emerald-500" },
    { label: "C", color: "from-orange-300 to-rose-400" },
    { label: "D", color: "from-gray-300 to-slate-500" },
  ];

  // --- 데이터 관리 함수 ---
  useEffect(() => {
    loadTierAlbumData(albumId);
  }, [albumId]);

  const saveTierAlbumData = () => {
    try {
      localStorage.setItem(
        `tierAlbum_${albumId}`,
        JSON.stringify({ tierPhotos, availablePhotos })
      );
      const sTierFirstPhoto = tierPhotos.S?.[0];
      if (sTierFirstPhoto) {
        localStorage.setItem(`tierAlbumCover_${albumId}`, sTierFirstPhoto.src);
      }
      alert("✅ 티어 앨범이 성공적으로 저장되었습니다!");
      onBack();
    } catch (error) {
      console.error("Failed to save data:", error);
      alert("❌ 저장 실패");
    }
  };

  const loadTierAlbumData = (id: string) => {
    try {
      const savedData = localStorage.getItem(`tierAlbum_${id}`);
      if (savedData) {
        const data = JSON.parse(savedData);
        setTierPhotos(data.tierPhotos || { S: [], A: [], B: [], C: [], D: [] });
        setAvailablePhotos(data.availablePhotos || []);
      } else {
        setTierPhotos({
          S: [{ id: "photo_s1", src: "/jaewan1.jpg", name: "S급 사진1" }],
          A: [],
          B: [],
          C: [],
          D: [],
        });
        setAvailablePhotos([
          { id: "photo1", src: "/ssafy-dummy1.jpg", name: "사진1" },
          { id: "photo2", src: "/ssafy-dummy2.jpg", name: "사진2" },
          { id: "photo3", src: "/ssafy-dummy3.jpg", name: "사진3" },
          { id: "photo4", src: "/ssafy-dummy4.jpg", name: "사진4" },
          { id: "photo5", src: "/dummy/dummy1.jpg", name: "사진5" },
          { id: "photo6", src: "/dummy/dummy2.jpg", name: "사진6" },
          { id: "photo7", src: "/dummy/dummy3.jpg", name: "사진7" },
          { id: "photo8", src: "/dummy/dummy4.jpg", name: "사진8" },
          { id: "photo9", src: "/dummy/dummy5.jpg", name: "사진9" },
          { id: "photo9", src: "/dummy/dummy6.jpg", name: "사진10" },
          { id: "photo9", src: "/dummy/dummy7.jpg", name: "사진11" },
          { id: "photo9", src: "/dummy/dummy8.jpg", name: "사진12" },
        ]);
      }
    } catch (error) {
      console.error("Failed to load data:", error);
    }
  };

  const handleImageClick = (photo: Photo) => {
    setSelectedImage(photo);
    setShowImageModal(true);
  };

  const handleCloseImageModal = () => {
    setShowImageModal(false);
    setSelectedImage(null);
  };

  const handleReturnToAvailable = (photoId: string, fromTier: string) => {
    const photo = tierPhotos[fromTier]?.find((p) => p.id === photoId);
    if (photo) {
      setTierPhotos((prev) => ({
        ...prev,
        [fromTier]: prev[fromTier].filter((p) => p.id !== photoId),
      }));
      setAvailablePhotos((prev) => [...prev, photo]);
    }
  };

  // --- 드래그 앤 드롭 로직 ---
  const handleDragStart = (
    e: React.DragEvent,
    photo: Photo,
    source: string | "available"
  ) => {
    e.dataTransfer.setData(
      "text/plain",
      JSON.stringify({ photoId: photo.id, source })
    );
    setDraggingPhotoId(photo.id);
  };

  const handleDragEnd = () => {
    setDraggingPhotoId(null);
    setDragOverPosition(null);
  };

  const handleDragOverTierArea = (e: React.DragEvent, tier: string) => {
    e.preventDefault();
    setDragOverPosition({ tier, index: (tierPhotos[tier] || []).length });
  };

  const handleDropTierArea = (e: React.DragEvent, targetTier: string) => {
    e.preventDefault();
    handleDropAtPosition(e, targetTier, (tierPhotos[targetTier] || []).length);
  };

  const handleDragOverPosition = (
    e: React.DragEvent,
    tier: string,
    index: number
  ) => {
    e.preventDefault();
    e.stopPropagation();
    const rect = (e.currentTarget as HTMLElement).getBoundingClientRect();
    const mouseX = e.clientX - rect.left;
    const isLeftHalf = mouseX < rect.width / 2;
    const targetIndex = isLeftHalf ? index : index + 1;
    setDragOverPosition({ tier, index: targetIndex });
  };

  const handleDropAtPosition = (
    e: React.DragEvent,
    targetTier: string,
    targetIndex: number
  ) => {
    e.preventDefault();
    e.stopPropagation();
    setDragOverPosition(null);
    const data = JSON.parse(e.dataTransfer.getData("text/plain"));
    const { photoId, source } = data;
    const draggedPhoto =
      source === "available"
        ? availablePhotos.find((p) => p.id === photoId)
        : tierPhotos[source]?.find((p) => p.id === photoId);
    if (!draggedPhoto) return;

    const sourceIndex =
      source !== "available"
        ? tierPhotos[source].findIndex((p) => p.id === photoId)
        : -1;
    if (
      source === targetTier &&
      (targetIndex === sourceIndex || targetIndex === sourceIndex + 1)
    )
      return;

    if (
      precisionTierMode &&
      tierPhotos[targetTier]?.length > 0 &&
      source !== targetTier
    ) {
      const opponents = [...tierPhotos[targetTier]]
        .slice(0, targetIndex)
        .reverse();
      if (opponents.length > 0) {
        setBattleSequence({
          newPhoto: draggedPhoto,
          opponents,
          currentOpponentIndex: 0,
          targetTier,
          targetIndex,
          sourceType: source,
        });
        setShowComparisonModal(true);
        return;
      }
    }

    setTierPhotos((prev) => {
      const newTiers = { ...prev };
      if (source === "available") {
        setAvailablePhotos((p) => p.filter((p) => p.id !== photoId));
      } else {
        newTiers[source] = [...(newTiers[source] || [])].filter(
          (p) => p.id !== photoId
        );
      }
      const targetArray = [...(newTiers[targetTier] || [])];
      targetArray.splice(targetIndex, 0, draggedPhoto);
      newTiers[targetTier] = targetArray;
      return newTiers;
    });
  };

  // --- 배틀 로직 ---
  const handleBattleDecision = (winnerId: string) => {
    if (!battleSequence) return;
    const isNewPhotoWin = winnerId === battleSequence.newPhoto.id;

    if (
      isNewPhotoWin &&
      battleSequence.currentOpponentIndex < battleSequence.opponents.length - 1
    ) {
      setBattleSequence((prev) =>
        prev
          ? { ...prev, currentOpponentIndex: prev.currentOpponentIndex + 1 }
          : null
      );
    } else {
      finalizeBattleResult(isNewPhotoWin);
    }
  };

  const finalizeBattleResult = (isNewPhotoWin: boolean) => {
    if (!battleSequence) return;
    const {
      newPhoto,
      targetTier,
      sourceType,
      opponents,
      currentOpponentIndex,
      targetIndex,
    } = battleSequence;
    let finalIndex = isNewPhotoWin
      ? targetIndex - opponents.length
      : tierPhotos[targetTier].findIndex(
          (p) => p.id === opponents[currentOpponentIndex].id
        ) + 1;

    setTierPhotos((prev) => {
      const newTiers = { ...prev };
      if (sourceType === "available") {
        setAvailablePhotos((p) => p.filter((p) => p.id !== newPhoto.id));
      } else {
        newTiers[sourceType] = newTiers[sourceType].filter(
          (p) => p.id !== newPhoto.id
        );
      }
      const targetArray = [...(newTiers[targetTier] || [])];
      targetArray.splice(finalIndex, 0, newPhoto);
      newTiers[targetTier] = targetArray;
      return newTiers;
    });
    setShowComparisonModal(false);
    setBattleSequence(null);
  };

  const handleCloseBattleModal = () => {
    setShowComparisonModal(false);
    setBattleSequence(null);
  };

  // --- 최종 렌더링 ---
  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between gap-4 mb-4 p-4 bg-white rounded-xl shadow-md border">
        <div className="flex items-center gap-4">
          <button
            onClick={onBack}
            className="p-2 text-gray-500 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ArrowUturnLeftIcon className="w-6 h-6" />
          </button>
          <h2 className="text-xl font-bold text-gray-800 truncate">
            {albumTitle}
          </h2>
        </div>
        <div className="flex items-center gap-4">
          <button
            onClick={() => setPrecisionTierMode(!precisionTierMode)}
            className={`flex items-center gap-2 px-4 py-2 rounded-lg font-semibold transition-all duration-300 ${
              precisionTierMode
                ? "bg-teal-100 text-teal-700 ring-2 ring-teal-500"
                : "bg-gray-100 text-gray-600 hover:bg-gray-200"
            }`}
          >
            <BoltIcon className="w-5 h-5" />
            <span className="hidden sm:block">
              {precisionTierMode ? "배틀 활성화됨" : "정밀 배틀"}
            </span>
          </button>
          <button
            onClick={saveTierAlbumData}
            className="px-5 py-2 bg-teal-500 text-white rounded-lg font-bold hover:bg-teal-600 shadow-sm transition-all"
          >
            💾 저장
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[2fr_1fr] gap-4">
        <div className="bg-white rounded-xl shadow-md border p-4 space-y-2">
          {tiers.map(({ label, color }) => (
            <div key={label} className="flex items-start">
              <div
                className={`w-16 h-28 flex-shrink-0 flex items-center justify-center text-white text-3xl font-black rounded-l-md bg-gradient-to-br ${color}`}
              >
                {label}
              </div>
              <div
                className="flex-1 p-2 flex flex-wrap gap-2 items-center border-t border-b border-r rounded-r-md min-h-[112px]"
                onDragOver={(e) => handleDragOverTierArea(e, label)}
                onDrop={(e) => handleDropTierArea(e, label)}
              >
                {(tierPhotos[label] || []).length === 0 && !draggingPhotoId && (
                  <div className="w-full h-full flex flex-col items-center justify-center text-gray-400 text-center">
                    <span className="text-3xl mb-2">📷</span>
                    <span className="text-sm">사진을 여기에 드래그하세요.</span>
                  </div>
                )}
                {(tierPhotos[label] || []).map((photo, index) => (
                  <div
                    key={photo.id}
                    className="flex items-center"
                    onDragOver={(e) => handleDragOverPosition(e, label, index)}
                    onDrop={(e) =>
                      handleDropAtPosition(
                        e,
                        label,
                        dragOverPosition?.index ?? index
                      )
                    }
                  >
                    {dragOverPosition?.tier === label &&
                      dragOverPosition.index === index && (
                        <div className="w-1.5 h-20 bg-teal-400 rounded-full transition-all" />
                      )}

                    {/* [수정] 투명도 효과(transition-opacity, opacity-40) 제거 */}
                    <div
                      draggable
                      onDragStart={(e) => handleDragStart(e, photo, label)}
                      onDragEnd={handleDragEnd}
                      onClick={() => handleImageClick(photo)}
                      className="relative group"
                    >
                      <Image
                        src={photo.src}
                        alt={photo.name}
                        width={88}
                        height={88}
                        className="rounded-md object-cover cursor-grab w-22 h-22 shadow-md hover:scale-105 transition-transform"
                      />

                      {label === "S" && index < 3 && (
                        <div className="absolute -top-2 -left-1 text-2xl z-10 filter drop-shadow-lg">
                          {index === 0 ? "👑" : index === 1 ? "🥈" : "🥉"}
                        </div>
                      )}

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          handleReturnToAvailable(photo.id, label);
                        }}
                        className="absolute -top-2 -right-2 w-5 h-5 bg-red-500 text-white rounded-full text-xs opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center z-10"
                      >
                        ✕
                      </button>
                    </div>
                  </div>
                ))}
                {dragOverPosition?.tier === label &&
                  dragOverPosition.index ===
                    (tierPhotos[label] || []).length && (
                    <div className="w-1.5 h-20 bg-teal-400 rounded-full ml-2 transition-all" />
                  )}
              </div>
            </div>
          ))}
        </div>

        <div className="bg-white rounded-xl p-6 shadow-md border lg:h-full lg:max-h-[calc(100vh-180px)] overflow-y-auto">
          <h3 className="text-xl font-bold text-gray-700 mb-4">
            사용 가능한 사진
          </h3>
          <div className="p-4 bg-gray-50 rounded-lg min-h-[120px] flex flex-wrap gap-3 items-center border-2 border-dashed">
            {availablePhotos.map((photo) => (
              // [수정] 여기에서도 투명도 효과 제거
              <div
                key={photo.id}
                draggable
                onDragStart={(e) => handleDragStart(e, photo, "available")}
                onDragEnd={handleDragEnd}
                onClick={() => handleImageClick(photo)}
              >
                <Image
                  src={photo.src}
                  alt={photo.name}
                  width={88}
                  height={88}
                  className="rounded-md object-cover cursor-grab w-22 h-22 shadow-sm"
                />
              </div>
            ))}
            {availablePhotos.length === 0 && (
              <p className="text-gray-400 text-sm">
                모든 사진이 배치되었습니다.
              </p>
            )}
          </div>
        </div>
      </div>

      <TierBattleModal
        isOpen={showComparisonModal}
        battleSequence={battleSequence}
        onClose={handleCloseBattleModal}
        onDecision={handleBattleDecision}
        onZoomRequest={handleImageClick}
      />
      {showImageModal && selectedImage && (
        <div
          className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center"
          onClick={handleCloseImageModal}
        >
          <Image
            src={selectedImage.src}
            alt={selectedImage.name}
            width={800}
            height={800}
            className="max-w-[90vw] max-h-[90vh] object-contain rounded-lg"
          />
        </div>
      )}
    </div>
  );
}
