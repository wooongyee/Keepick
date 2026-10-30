// src/widgets/video-conference/SettingsPanel.tsx
"use client";

import { useAppDispatch, useAppSelector } from "@/shared/config/hooks";
import {
  setAiEnabled,
  toggleStaticGestureDetection,
  toggleDynamicGestureDetection,
  toggleEmotionDetection,
  toggleBeautyFilter,
} from "@/entities/video-conference/ai/model/aiSlice";
import {
  XMarkIcon,
  CogIcon,
  HandRaisedIcon,
  SparklesIcon,
  VideoCameraIcon,
  MicrophoneIcon,
  ComputerDesktopIcon,
  FaceSmileIcon,
  StarIcon,
} from "@heroicons/react/24/solid";
import { motion, AnimatePresence } from "framer-motion";
import { useCallback } from "react";

interface SettingsPanelProps {
  isOpen: boolean;
  onClose: () => void;
}

type AiFeature = 'static' | 'dynamic' | 'emotion' | 'beauty';

const ToggleSwitch = ({
  label,
  description,
  isOn,
  onToggle,
  disabled = false,
  icon,
}: {
  label: string;
  description?: string;
  isOn: boolean;
  onToggle: () => void;
  disabled?: boolean;
  icon?: React.ReactNode;
}) => (
  <div
    className={`flex items-start justify-between p-4 rounded-lg ${
      disabled ? "opacity-50 cursor-not-allowed" : "hover:bg-[#424245]/30"
    } transition-colors`}
  >
    <div className="flex-1">
      <div className="flex items-center space-x-2 mb-1">
        {icon && <div className="text-[#FE7A25]">{icon}</div>}
        <span className="text-[#FFFFFF] font-medium">{label}</span>
      </div>
      {description && <p className="text-[#A0A0A5] text-sm">{description}</p>}
    </div>
    <button
      onClick={onToggle}
      role="switch"
      aria-label={label}
      aria-checked={isOn}
      disabled={disabled}
      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
        disabled ? "cursor-not-allowed" : "cursor-pointer"
      } ${isOn ? "bg-[#FE7A25]" : "bg-[#424245]"}`}
    >
      <span
        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform ${
          isOn ? "translate-x-6" : "translate-x-1"
        }`}
      />
    </button>
  </div>
);

export const SettingsPanel = ({ isOpen, onClose }: SettingsPanelProps) => {
  const dispatch = useAppDispatch();
  
  const aiState = useAppSelector((state) => state.ai);
  const { isCameraOn, isMicOn } = useAppSelector((state) => state.mediaControls);

  // ConferenceClientPage owns the asynchronous media transition.
  const handleAiToggle = useCallback(() => {
    dispatch(setAiEnabled(!aiState.isAiEnabled));
  }, [dispatch, aiState.isAiEnabled]);

  const handleFeatureToggle = useCallback((feature: AiFeature) => {
    switch (feature) {
      case 'static': dispatch(toggleStaticGestureDetection()); break;
      case 'dynamic': dispatch(toggleDynamicGestureDetection()); break;
      case 'emotion': dispatch(toggleEmotionDetection()); break;
      case 'beauty': dispatch(toggleBeautyFilter()); break;
    }
  }, [dispatch]);

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            className="fixed inset-0 bg-black/50 z-40"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
          />
          <motion.div
            className="fixed bottom-20 left-1/2 transform -translate-x-1/2 w-96 bg-[#2C2C2E] rounded-2xl shadow-2xl z-50 border border-[#424245]"
            initial={{ opacity: 0, y: 20, scale: 0.95 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 20, scale: 0.95 }}
            transition={{ duration: 0.2 }}
          >
            <div className="flex items-center justify-between p-6 border-b border-[#424245]">
              <div className="flex items-center space-x-2">
                <CogIcon className="w-5 h-5 text-[#FE7A25]" />
                <h3 className="text-[#FFFFFF] font-semibold font-header">설정</h3>
              </div>
              <button onClick={onClose} className="p-1 rounded-lg hover:bg-[#424245] transition-colors">
                <XMarkIcon className="w-5 h-5 text-[#A0A0A5]" />
              </button>
            </div>
            <div className="p-4 space-y-2 max-h-96 overflow-y-auto">
              <div className="mb-6">
                <ToggleSwitch
                  label="AI 기능 전체"
                  description="모든 AI 기능을 활성화/비활성화합니다"
                  isOn={aiState.isAiEnabled}
                  onToggle={handleAiToggle}
                  icon={<StarIcon className="w-4 h-4" />}
                />
                {aiState.status === "loading" && <p role="status" className="px-4 text-sm text-[#FCBC34]">AI 준비 중 · 기본 통화는 계속됩니다.</p>}
                {aiState.error && <p role="alert" className="px-4 text-sm text-red-400">AI 준비 또는 처리에 실패했습니다. 잠시 후 다시 켜 주세요. 기본 영상으로 통화할 수 있습니다.</p>}

              </div>
              
              {/* <div className="mb-6 bg-[#222222]/30 rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <h4 className="text-[#A0A0A5] text-sm font-medium">AI 세부 기능</h4>
                </div> */}
                {/* <ToggleSwitch
                  label="정적 제스처"
                  description="손 모양으로 이모지 표현 (👍, 👌, ✌️ 등)"
                  isOn={aiState.isStaticGestureDetectionEnabled}
                  onToggle={() => handleFeatureToggle('static')}
                  disabled={!aiState.isAiEnabled}
                  icon={<HandRaisedIcon className="w-4 h-4" />}
                />
                <ToggleSwitch
                  label="동적 제스처"
                  description="손 움직임으로 이모지 표현 (👋, 🔥, 💖 등)"
                  isOn={aiState.isDynamicGestureDetectionEnabled}
                  onToggle={() => handleFeatureToggle('dynamic')}
                  disabled={!aiState.isAiEnabled}
                  icon={<SparklesIcon className="w-4 h-4" />}
                />
                <ToggleSwitch
                  label="감정 감지"
                  description="실시간 감정 분석 및 캡처 (😊, 😢, 😮 등)"
                  isOn={aiState.isEmotionDetectionEnabled}
                  onToggle={() => handleFeatureToggle('emotion')}
                  disabled={!aiState.isAiEnabled}
                  icon={<FaceSmileIcon className="w-4 h-4" />}
                />
                <ToggleSwitch
                  label="뷰티 필터"
                  description="AI 기반 자동 보정 및 필터 효과"
                  isOn={aiState.isBeautyFilterEnabled}
                  onToggle={() => handleFeatureToggle('beauty')}
                  disabled={!aiState.isAiEnabled}
                  icon={<SparklesIcon className="w-4 h-4" />}
                /> */}
            

              <div className="mb-6">
                <h4 className="text-[#A0A0A5] text-sm font-medium mb-3 px-4">미디어 설정</h4>
                <div className="p-4 rounded-lg bg-[#222222]/50">
                   <div className="flex items-center justify-between mb-2">
                       <div className="flex items-center space-x-2">
                           <VideoCameraIcon className="w-4 h-4 text-[#FE7A25]" />
                           <span className="text-[#FFFFFF] text-sm">카메라</span>
                       </div>
                       <span className={`text-sm ${isCameraOn ? "text-[#FE7A25]" : "text-[#D22016]"}`}>
                           {isCameraOn ? "켜짐" : "꺼짐"}
                       </span>
                   </div>
                   <div className="flex items-center justify-between mb-2">
                       <div className="flex items-center space-x-2">
                           <MicrophoneIcon className="w-4 h-4 text-[#FE7A25]" />
                           <span className="text-[#FFFFFF] text-sm">마이크</span>
                       </div>
                       <span className={`text-sm ${isMicOn ? "text-[#FE7A25]" : "text-[#D22016]"}`}>
                           {isMicOn ? "켜짐" : "꺼짐"}
                       </span>
                   </div>
                </div>
              </div>
              
              <div className="p-4 border-t border-[#424245] text-center">
                <p className="text-[#A0A0A5] text-xs">더 많은 기능이 곧 추가될 예정입니다</p>
              </div>
            </div>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
};
