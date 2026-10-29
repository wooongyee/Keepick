// src/entities/emoji-reaction/model/slice.ts
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface EmojiReaction {
  id: string;
  emoji: string;
  userId: string;
  userName: string;
  timestamp: number; // [수정] Date 타입을 number로 변경하여 직렬화 문제를 해결
  duration?: number; // 표시 시간 (ms)
  source?: 'ai' | 'manual'; // AI 자동 감지 또는 수동 선택
  aiType?: 'gesture' | 'emotion'; // AI 타입 (제스처 또는 감정)
  confidence?: number; // AI 신뢰도
}

interface EmojiReactionState {
  activeReactions: { [userId: string]: EmojiReaction }; // 현재 활성화된 반응들
  reactionHistory: EmojiReaction[]; // 반응 히스토리 (최근 50개)
}

const initialState: EmojiReactionState = {
  activeReactions: {},
  reactionHistory: [],
};

const emojiReactionSlice = createSlice({
  name: "emojiReaction",
  initialState,
  reducers: {
    addReaction: (state, action: PayloadAction<EmojiReaction>) => {
      const reaction = action.payload;

      // 해당 사용자의 기존 반응을 새 반응으로 교체
      state.activeReactions[reaction.userId] = reaction;

      // 히스토리에 추가 (최대 50개 유지)
      state.reactionHistory.unshift(reaction);
      if (state.reactionHistory.length > 50) {
        state.reactionHistory = state.reactionHistory.slice(0, 50);
      }
    },

    expireReaction: (state, action: PayloadAction<{ userId: string; id: string }>) => {
      const { userId, id } = action.payload;
      if (state.activeReactions[userId]?.id === id) delete state.activeReactions[userId];
    },
    removeReaction: (state, action: PayloadAction<string>) => {
      const userId = action.payload;
      delete state.activeReactions[userId];
    },

    clearUserReaction: (state, action: PayloadAction<string>) => {
      const userId = action.payload;
      delete state.activeReactions[userId];
    },

    clearAllReactions: (state) => {
      state.activeReactions = {};
    },

    // 자동으로 만료된 반응들을 정리하는 액션
    cleanupExpiredReactions: (state) => {
      const now = Date.now(); // 현재 시간의 숫자 타임스탬프
      Object.keys(state.activeReactions).forEach((userId) => {
        const reaction = state.activeReactions[userId];

        // [수정] reaction.timestamp가 이미 숫자이므로 변환 과정이 필요 없음
        const reactionTime = reaction.timestamp;
        const duration = reaction.duration || 3000; // 기본 3초

        if (now - reactionTime > duration) {
          delete state.activeReactions[userId];
        }
      });
    },
  },
});

export const {
  addReaction,
  removeReaction,
  expireReaction,
  clearUserReaction,
  clearAllReactions,
  cleanupExpiredReactions,
} = emojiReactionSlice.actions;

export const emojiReactionReducer = emojiReactionSlice.reducer;
