// src/components/group/HighlightAlbumView.tsx
"use client";

import React, { useState } from "react";
import Image from "next/image";
import {
  ArrowUturnLeftIcon,
  CursorArrowRaysIcon,
} from "@heroicons/react/24/outline";

interface Photo {
  id: string;
  src: string;
}

interface HighlightAlbumViewProps {
  albumId: string;
  albumTitle: string;
  onBack: () => void;
}

// [수정] 레이아웃 클래스 제거하여 데이터 구조 단순화
const emotionCategories = [
  {
    id: "lol",
    title: "웃음 대폭발의 순간",
    description: '"이때 왜 웃었지?" 다시 봐도 웃음이 나는 유쾌한 대화들',
    icon: "😂",
    images: Array(5)
      .fill(null)
      .map((_, i) => ({ id: `lol${i + 1}`, src: "/ssafy-dummy1.jpg" })),
  },
  {
    id: "surprised",
    title: "동공지진! 놀람의 순간",
    description: '"헐, 대박!"을 외쳤던, 예상치 못한 반전의 기록들',
    icon: "😮",
    images: Array(4)
      .fill(null)
      .map((_, i) => ({ id: `surp${i + 1}`, src: "/ssafy-dummy1.jpg" })),
  },
  {
    id: "serious",
    title: "진지한 대화, 깊어진 우리",
    description: "가끔은 진지하게, 우리의 깊은 속마음을 나눴던 순간",
    icon: "🤔",
    images: Array(3)
      .fill(null)
      .map((_, i) => ({ id: `seri${i + 1}`, src: "/ssafy-dummy1.jpg" })),
  },
  {
    id: "screenshots",
    title: "기억하고 싶은 모든 순간",
    description: "사소하지만 그래서 더 특별한, 우리만의 모든 기록들",
    icon: "📸",
    images: Array(6)
      .fill(null)
      .map((_, i) => ({ id: `ss${i + 1}`, src: "/ssafy-dummy1.jpg" })),
  },
];

const HighlightAlbumView: React.FC<HighlightAlbumViewProps> = ({
  albumId,
  albumTitle,
  onBack,
}) => {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState<Photo | null>(null);

  const handleCategoryClick = (categoryId: string) => {
    setSelectedCategory((prev) => (prev === categoryId ? null : categoryId));
  };

  const handleImageClick = (image: Photo) => {
    setSelectedImage(image);
  };

  const handleCloseModal = () => {
    setSelectedImage(null);
  };

  const activeCategoryData = emotionCategories.find(
    (cat) => cat.id === selectedCategory
  );

  return (
    <>
      <div className="space-y-6 animate-fade-in">
        <div className="flex items-center justify-between gap-4 p-4 bg-white rounded-xl shadow-md border">
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
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {emotionCategories.map((category) => (
            <button
              key={category.id}
              onClick={() => handleCategoryClick(category.id)}
              className={`group p-5 text-left bg-white rounded-xl shadow-md border-2 transition-all duration-300 ${
                selectedCategory === category.id
                  ? "border-teal-500 shadow-lg scale-105"
                  : "border-transparent hover:border-teal-300 hover:shadow-lg hover:-translate-y-1"
              }`}
            >
              <div className="flex items-start gap-4">
                <span className="text-3xl mt-1">{category.icon}</span>
                <div>
                  <h3 className="font-bold text-lg text-gray-800">
                    {category.title}
                  </h3>
                  <p className="text-sm text-gray-500">
                    {category.description}
                  </p>
                </div>
              </div>
            </button>
          ))}
        </div>

        {activeCategoryData ? (
          <div className="bg-white rounded-xl shadow-lg border p-6 animate-fade-in">
            <h3 className="text-2xl font-bold text-gray-800 mb-4 flex items-center gap-3">
              <span className="text-3xl">{activeCategoryData.icon}</span>
              {activeCategoryData.title}
            </h3>
            {/* [수정] 깔끔하고 반응형인 그리드 레이아웃으로 변경 */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 gap-3">
              {activeCategoryData.images.map((image) => (
                <div
                  key={image.id}
                  className="relative aspect-square overflow-hidden rounded-lg group cursor-pointer border border-gray-200 shadow-sm hover:shadow-xl hover:border-teal-400 transition-all duration-300"
                  onClick={() => handleImageClick(image)}
                >
                  <Image
                    src={image.src}
                    alt={image.id}
                    layout="fill"
                    objectFit="cover"
                    className="transition-transform duration-300 group-hover:scale-105"
                  />
                  <div className="absolute inset-0 bg-black/10 opacity-0 group-hover:opacity-100 transition-opacity" />
                </div>
              ))}
            </div>
          </div>
        ) : (
          <div className="text-center py-16 px-6 bg-white rounded-xl shadow-lg border animate-fade-in">
            <CursorArrowRaysIcon className="mx-auto h-16 w-16 text-teal-400 mb-4" />
            <h3 className="text-2xl font-bold text-gray-800">
              어떤 순간이 궁금하신가요?
            </h3>
            <p className="mt-2 text-md text-gray-500">
              궁금한 순간을 선택하여 그날의 기억을 확인해보세요!
            </p>
          </div>
        )}
      </div>

      {selectedImage && (
        <div
          className="fixed inset-0 bg-black/80 z-50 flex items-center justify-center p-4 animate-fade-in"
          onClick={handleCloseModal}
        >
          <div
            className="relative max-w-4xl max-h-full"
            onClick={(e) => e.stopPropagation()}
          >
            <Image
              src={selectedImage.src}
              alt={selectedImage.id}
              width={1200}
              height={800}
              className="object-contain rounded-lg shadow-2xl"
              style={{ maxHeight: "90vh", maxWidth: "90vw" }}
            />
          </div>
        </div>
      )}
    </>
  );
};

export default HighlightAlbumView;
