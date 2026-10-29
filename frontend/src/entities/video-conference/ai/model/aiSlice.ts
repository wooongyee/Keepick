// src/entities/video-conference/ai/model/aiSlice.ts

import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { GestureData, EmotionData, CapturedFrame } from "@/shared/types/ai.types";

interface AiState {
  isAiEnabled: boolean;
  status: "idle" | "loading" | "ready" | "error";
  error: string | null;
  detectedGestures: GestureData[];
  detectedEmotions: EmotionData[];
  capturedEmotionFrames: CapturedFrame[];

  isStaticGestureDetectionEnabled: boolean;
  isDynamicGestureDetectionEnabled: boolean;
  isEmotionDetectionEnabled: boolean;
  isBeautyFilterEnabled: boolean;

  emotionConfidenceThreshold: number;
  beautyFilterConfig: {
    gamma: number;
    lipAlpha: number;
    smoothAmount: number;
    lipColor: [number, number, number];
  };
}

const initialState: AiState = {
  isAiEnabled: false,
  status: "idle",
  error: null,
  detectedGestures: [],
  detectedEmotions: [],
  capturedEmotionFrames: [],
  isStaticGestureDetectionEnabled: true,
  isDynamicGestureDetectionEnabled: true,
  isEmotionDetectionEnabled: true,
  isBeautyFilterEnabled: false,
  emotionConfidenceThreshold: 0.6,
  beautyFilterConfig: {
    gamma: 1.4,
    lipAlpha: 0.2,
    smoothAmount: 30,
    lipColor: [255, 0, 0],
  },
};

const aiSlice = createSlice({
  name: "ai",
  initialState,
  reducers: {
    setAiRuntime: (state, action: PayloadAction<{ status: AiState["status"]; error?: string | null }>) => {
      state.status = action.payload.status;
      state.error = action.payload.error ?? null;
    },
    setAiEnabled: (state, action: PayloadAction<boolean>) => {
      state.isAiEnabled = action.payload;
    },
    addDetectedGesture: (state, action: PayloadAction<GestureData>) => {
      state.detectedGestures.push(action.payload);
      state.detectedGestures = state.detectedGestures.slice(-10);
    },
    addDetectedEmotion: (state, action: PayloadAction<EmotionData>) => {
      state.detectedEmotions.push(action.payload);
      state.detectedEmotions = state.detectedEmotions.slice(-10);
    },
    addCapturedEmotionFrame: (state, action: PayloadAction<CapturedFrame>) => {
      state.capturedEmotionFrames.push(action.payload);
      state.capturedEmotionFrames = state.capturedEmotionFrames.slice(-5);
    },
    toggleStaticGestureDetection: (state) => {
      state.isStaticGestureDetectionEnabled = !state.isStaticGestureDetectionEnabled;
    },
    toggleDynamicGestureDetection: (state) => {
      state.isDynamicGestureDetectionEnabled = !state.isDynamicGestureDetectionEnabled;
    },
    toggleEmotionDetection: (state) => {
      state.isEmotionDetectionEnabled = !state.isEmotionDetectionEnabled;
    },
    toggleBeautyFilter: (state) => {
      state.isBeautyFilterEnabled = !state.isBeautyFilterEnabled;
    },
    updateBeautyFilterConfig: (
      state,
      action: PayloadAction<Partial<AiState["beautyFilterConfig"]>>
    ) => {
      state.beautyFilterConfig = { ...state.beautyFilterConfig, ...action.payload };
    },
    clearCapturedEmotionFrames: (state) => {
      state.capturedEmotionFrames = [];
    },
    setEmotionConfidenceThreshold: (state, action: PayloadAction<number>) => {
      state.emotionConfidenceThreshold = action.payload;
    },
  },
});

export const {
  setAiRuntime,
  setAiEnabled,
  addDetectedGesture,
  addDetectedEmotion,
  addCapturedEmotionFrame,
  toggleStaticGestureDetection,
  toggleDynamicGestureDetection,
  toggleEmotionDetection,
  toggleBeautyFilter,
  updateBeautyFilterConfig,
  clearCapturedEmotionFrames,
  setEmotionConfidenceThreshold,
} = aiSlice.actions;

export default aiSlice.reducer;
