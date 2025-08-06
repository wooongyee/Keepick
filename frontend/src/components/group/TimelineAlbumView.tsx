// src/components/group/TimelineAlbumView.tsx
"use client";

import React, { useState, useEffect, useRef } from "react";
import Image from "next/image";
import {
  ArrowUturnLeftIcon,
  PlusIcon,
  CalendarDaysIcon,
  MapPinIcon,
  TrashIcon,
} from "@heroicons/react/24/outline";

// 타입 정의
interface Photo {
  id: string;
  src: string;
  name: string;
}

interface TimelineEvent {
  id: string;
  title: string;
  date: string;
  location: string;
  emoji: string;
  description: string;
  photos: Photo[];
}

interface TimelineAlbumViewProps {
  albumId: string;
  albumTitle: string;
  onBack: () => void;
}

const TimelineAlbumView: React.FC<TimelineAlbumViewProps> = ({
  albumId,
  albumTitle,
  onBack,
}) => {
  // --- 상태 관리 ---
  const [timelineEvents, setTimelineEvents] = useState<TimelineEvent[]>([
    {
      id: "event1",
      title: "우리의 첫 만남",
      date: "2024.01.15",
      location: "서울 카페거리",
      emoji: "🥰",
      description: "이날 엄청 설렜는데, 날씨도 완벽했어.",
      photos: [],
    },
    {
      id: "event2",
      title: "첫 번째 여행",
      date: "2024.03.20",
      location: "부산 해운대",
      emoji: "✈️",
      description: "밤바다 보면서 나눴던 얘기들, 아직도 생생해.",
      photos: [],
    },
  ]);

  const [availablePhotos, setAvailablePhotos] = useState<Photo[]>([
    { id: "photo1", src: "/ssafy-dummy1.jpg", name: "사진1" },
    { id: "photo2", src: "/ssafy-dummy2.jpg", name: "사진2" },
    { id: "photo3", src: "/ssafy-dummy3.jpg", name: "사진3" },
    { id: "photo4", src: "/ssafy-dummy4.jpg", name: "사진4" },
    { id: "photo5", src: "/jaewan1.jpg", name: "사진5" },
    { id: "photo6", src: "/food-dummy1.jpg", name: "사진6" },
  ]);

  const [dragOverEventId, setDragOverEventId] = useState<string | null>(null);
  const [editingEmojiEventId, setEditingEmojiEventId] = useState<string | null>(
    null
  ); // [추가] 이모지 편집 상태

  const emojiOptions = ["🥰", "🥳", "✈️", "😂", "😢", "🤔", "😎", "🎉"];
  const emojiPickerRef = useRef<HTMLDivElement>(null);

  // --- 외부 클릭 감지 로직 ---
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        emojiPickerRef.current &&
        !emojiPickerRef.current.contains(event.target as Node)
      ) {
        setEditingEmojiEventId(null);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [emojiPickerRef]);

  // --- 이벤트 핸들러 ---
  const handleDragStart = (
    e: React.DragEvent<HTMLDivElement>,
    photo: Photo
  ) => {
    e.dataTransfer.setData("photo", JSON.stringify(photo));
  };

  const handleDragOver = (
    e: React.DragEvent<HTMLDivElement>,
    eventId: string
  ) => {
    e.preventDefault();
    setDragOverEventId(eventId);
  };

  const handleDrop = (
    e: React.DragEvent<HTMLDivElement>,
    targetEventId: string
  ) => {
    e.preventDefault();
    const photoData = JSON.parse(e.dataTransfer.getData("photo")) as Photo;
    setTimelineEvents((prev) =>
      prev.map((event) =>
        event.id === targetEventId
          ? { ...event, photos: [...event.photos, photoData] }
          : event
      )
    );
    setAvailablePhotos((prev) => prev.filter((p) => p.id !== photoData.id));
    setDragOverEventId(null);
  };

  const handleRemovePhoto = (eventId: string, photoToRemove: Photo) => {
    setTimelineEvents((prev) =>
      prev.map((event) =>
        event.id === eventId
          ? {
              ...event,
              photos: event.photos.filter((p) => p.id !== photoToRemove.id),
            }
          : event
      )
    );
    setAvailablePhotos((prev) => [photoToRemove, ...prev]);
  };

  const handleFieldChange = (
    eventId: string,
    field: keyof TimelineEvent,
    value: string
  ) => {
    setTimelineEvents((prev) =>
      prev.map((event) =>
        event.id === eventId ? { ...event, [field]: value } : event
      )
    );
  };

  const handleAddEvent = () => {
    const newEvent: TimelineEvent = {
      id: `event_${Date.now()}`,
      title: "새로운 추억",
      date: new Date().toISOString().split("T")[0].replaceAll("-", "."),
      location: "어디에서?",
      emoji: "😊",
      description: "",
      photos: [],
    };
    setTimelineEvents((prev) => [...prev, newEvent]);
  };

  return (
    <div className="animate-fade-in">
      <div className="flex items-center justify-between gap-4 mb-6 p-4 bg-white rounded-xl shadow-md border">
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
            onClick={handleAddEvent}
            className="px-5 py-2 bg-teal-500 text-white rounded-lg font-bold hover:bg-teal-600 shadow-sm transition-all flex items-center gap-2"
          >
            <PlusIcon className="w-5 h-5" /> 추억 추가
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[2.5fr_1fr] gap-8">
        <div className="relative pl-8">
          {/* [수정] 타임라인 그라데이션 색상 변경 */}
          <div className="absolute left-4 top-0 bottom-0 w-1 bg-gradient-to-b from-emerald-300 via-cyan-300 to-indigo-300 rounded-full" />
          {timelineEvents.map((event) => (
            <div key={event.id} className="relative pl-8 mb-10">
              {/* [수정] 이모지 선택 기능 추가 */}
              <div
                className="absolute -left-1.5 top-5 w-8 h-8 rounded-full bg-white flex items-center justify-center cursor-pointer"
                onClick={() => setEditingEmojiEventId(event.id)}
              >
                <span className="text-xl">{event.emoji}</span>
              </div>

              {/* [추가] 이모지 선택 팝업 */}
              {editingEmojiEventId === event.id && (
                <div
                  ref={emojiPickerRef}
                  className="absolute -left-2 top-14 bg-white rounded-lg shadow-lg p-2 grid grid-cols-4 gap-2 z-10"
                >
                  {emojiOptions.map((emoji) => (
                    <button
                      key={emoji}
                      onClick={() => {
                        handleFieldChange(event.id, "emoji", emoji);
                        setEditingEmojiEventId(null);
                      }}
                      className="text-2xl hover:bg-gray-100 rounded-md p-1"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              )}

              <div
                onDragOver={(e) => handleDragOver(e, event.id)}
                onDrop={(e) => handleDrop(e, event.id)}
                onDragLeave={() => setDragOverEventId(null)}
                className={`bg-white rounded-lg shadow-md p-5 transition-all duration-300 ${
                  dragOverEventId === event.id
                    ? "shadow-2xl ring-2 ring-teal-400"
                    : ""
                }`}
              >
                <input
                  type="text"
                  value={event.title}
                  onChange={(e) =>
                    handleFieldChange(event.id, "title", e.target.value)
                  }
                  className="text-2xl font-bold text-gray-900 w-full bg-transparent focus:outline-none focus:bg-gray-50 rounded p-1 mb-2"
                />
                <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-gray-600 mb-4">
                  <div className="flex items-center gap-1.5">
                    <CalendarDaysIcon className="w-4 h-4" />
                    <input
                      type="text"
                      value={event.date}
                      onChange={(e) =>
                        handleFieldChange(event.id, "date", e.target.value)
                      }
                      className="bg-transparent focus:outline-none focus:bg-gray-50 rounded p-1 w-28"
                    />
                  </div>
                  <div className="flex items-center gap-1.5">
                    <MapPinIcon className="w-4 h-4" />
                    <input
                      type="text"
                      value={event.location}
                      onChange={(e) =>
                        handleFieldChange(event.id, "location", e.target.value)
                      }
                      className="bg-transparent focus:outline-none focus:bg-gray-50 rounded p-1"
                    />
                  </div>
                </div>

                <div
                  className={`p-3 bg-gray-50/70 rounded-md min-h-[110px] border-2 border-dashed transition-colors ${
                    dragOverEventId === event.id
                      ? "border-teal-400"
                      : "border-gray-200"
                  }`}
                >
                  {event.photos.length > 0 ? (
                    <div className="flex flex-wrap gap-3">
                      {event.photos.map((photo) => (
                        <div key={photo.id} className="relative group">
                          <Image
                            src={photo.src}
                            alt={photo.name}
                            width={80}
                            height={80}
                            className="w-20 h-20 object-cover rounded-md shadow-sm"
                          />
                          <button
                            onClick={() => handleRemovePhoto(event.id, photo)}
                            className="absolute -top-2 -right-2 w-6 h-6 bg-red-500 text-white rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity z-10"
                          >
                            <TrashIcon className="w-4 h-4" />
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-center text-gray-400 text-sm py-8">
                      이곳에 사진을 드래그하세요
                    </p>
                  )}
                </div>
                <div className="mt-4">
                  <textarea
                    value={event.description}
                    onChange={(e) =>
                      handleFieldChange(event.id, "description", e.target.value)
                    }
                    placeholder="이날의 이야기를 기록해보세요..."
                    className="w-full text-gray-700 bg-gray-50 rounded-md p-3 text-sm focus:ring-2 focus:ring-teal-500 focus:outline-none transition"
                    rows={2}
                  ></textarea>
                </div>
              </div>
            </div>
          ))}
          <div className="text-center text-gray-500 font-semibold py-8">
            ✨ 우리의 이야기는 계속됩니다... ✨
          </div>
        </div>

        <div className="bg-white rounded-xl p-6 shadow-md border h-fit sticky top-24">
          <h3 className="text-xl font-bold text-gray-700 mb-4">
            사용 가능한 사진
          </h3>
          {availablePhotos.length > 0 ? (
            <div className="grid grid-cols-[repeat(auto-fill,minmax(80px,1fr))] gap-3">
              {availablePhotos.map((photo) => (
                <div
                  key={photo.id}
                  draggable
                  onDragStart={(e) => handleDragStart(e, photo)}
                  className="cursor-grab"
                >
                  <Image
                    src={photo.src}
                    alt={photo.name}
                    width={88}
                    height={88}
                    className="w-full h-auto object-cover rounded-md shadow-sm aspect-square hover:scale-105 transition-transform"
                  />
                </div>
              ))}
            </div>
          ) : (
            <p className="text-center text-gray-400 text-sm py-10">
              모든 사진이 배치되었습니다.
            </p>
          )}
        </div>
      </div>
    </div>
  );
};

export default TimelineAlbumView;
