// components/group/TierBattleModal.tsx
"use client";

import { useState, useEffect } from "react";
import Image from "next/image";

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
}
interface TierBattleModalProps {
  isOpen: boolean;
  battleSequence: BattleSequence | null;
  onClose: () => void;
  onDecision: (winnerId: string) => void;
  onZoomRequest: (photo: Photo) => void;
}

export default function TierBattleModal({
  isOpen,
  battleSequence,
  onClose,
  onDecision,
  onZoomRequest,
}: TierBattleModalProps) {
  const [selectedPhoto, setSelectedPhoto] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      setSelectedPhoto(null);
    }
  }, [isOpen]);

  if (!isOpen || !battleSequence) return null;

  const { newPhoto, opponents, currentOpponentIndex, targetTier } =
    battleSequence;
  const currentOpponent = opponents[currentOpponentIndex];
  if (!currentOpponent) {
    onClose();
    return null;
  }

  return (
    <div
      className="fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl p-10 max-w-5xl w-full max-h-[90vh] shadow-2xl relative animate-modal-in"
        onClick={(e) => e.stopPropagation()}
      >
        {/* 닫기 버튼 */}
        <button
          onClick={onClose}
          className="absolute top-6 right-6 w-10 h-10 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center text-gray-500 hover:text-gray-700 text-xl transition-all"
        >
          ✕
        </button>

        {/* 좌상단 타이틀 */}
        <div className="absolute top-8 left-10 font-bold text-lg text-gray-700">
          티어 배틀
        </div>

        {/* 메인 헤더 */}
        <div className="text-center  mb-10">
          <h1 className="text-4xl font-bold text-gray-900 mb-3">
            {targetTier}티어 {opponents.length - currentOpponentIndex}위 결정전
          </h1>
          <p className="text-xl text-gray-600">
            더 높은 순위에 두고 싶은 추억을 선택해주세요
          </p>
        </div>

        {/* 대결 영역 */}
        <div className="flex items-center justify-center gap-16 mb-12">
          {/* 기존 추억 */}
          <div className="text-center">
            <div
              className={`relative group mb-4 cursor-pointer`}
              onClick={() => setSelectedPhoto(currentOpponent.id)}
            >
              <div
                className={`w-72 h-72 rounded-2xl overflow-hidden shadow-xl transition-all duration-300 relative ${
                  selectedPhoto === currentOpponent.id
                    ? "scale-105"
                    : "hover:scale-102"
                }`}
              >
                <Image
                  src={currentOpponent.src}
                  alt="기존 추억"
                  layout="fill"
                  objectFit="cover"
                />
                <div
                  className={`absolute inset-0 border-8 rounded-2xl transition-all duration-300 ${
                    selectedPhoto === currentOpponent.id
                      ? "border-emerald-500"
                      : "border-transparent"
                  }`}
                ></div>
              </div>
            </div>
            <h3 className="text-2xl font-semibold text-gray-800 mb-3">
              기존 추억
            </h3>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onZoomRequest(currentOpponent);
              }}
              className="px-5 py-2 bg-gray-700 hover:bg-gray-800 text-white rounded-lg font-medium transition-colors"
            >
              🔍 자세히 보기
            </button>
          </div>

          {/* VS 구분선 */}
          <div className="my-4">
            <span className="text-7xl font-black bg-gradient-to-r from-teal-500 to-cyan-600 bg-clip-text text-transparent">
              VS
            </span>
          </div>

          {/* 새로운 추억 */}
          <div className="text-center">
            <div
              className={`relative group mb-4 cursor-pointer`}
              onClick={() => setSelectedPhoto(newPhoto.id)}
            >
              <div
                className={`w-72 h-72 rounded-2xl overflow-hidden shadow-xl transition-all duration-300 relative ${
                  selectedPhoto === newPhoto.id
                    ? "scale-105"
                    : "hover:scale-102"
                }`}
              >
                <Image
                  src={newPhoto.src}
                  alt="새로운 추억"
                  layout="fill"
                  objectFit="cover"
                />
                <div
                  className={`absolute inset-0 border-8 rounded-2xl transition-all duration-300 ${
                    selectedPhoto === newPhoto.id
                      ? "border-emerald-500"
                      : "border-transparent"
                  }`}
                ></div>
              </div>
            </div>
            <h3 className="text-2xl font-semibold text-gray-800 mb-3">
              새로운 추억
            </h3>
            <button
              onClick={(e) => {
                e.stopPropagation();
                onZoomRequest(newPhoto);
              }}
              className="px-5 py-2 bg-gray-700 hover:bg-gray-800 text-white rounded-lg font-medium transition-colors"
            >
              🔍 자세히 보기
            </button>
          </div>
        </div>

        {/* 결정 버튼 */}
        <div className="text-center">
          {selectedPhoto ? (
            <button
              onClick={() => onDecision(selectedPhoto)}
              className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-bold text-xl py-5 px-16 rounded-2xl shadow-lg hover:shadow-xl transition-all transform hover:scale-105"
            >
              이걸로 결정하기!
            </button>
          ) : (
            <div className="bg-gray-50 rounded-2xl py-6 px-8 border-2 border-dashed border-gray-300">
              <p className="text-gray-500 text-lg font-medium">
                위의 두 추억 중 하나를 선택해주세요
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
