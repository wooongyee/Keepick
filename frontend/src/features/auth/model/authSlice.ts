// features/auth/model/authSlice.ts
import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { AuthState } from "./types";


// 초기 상태 정의
const initialState: AuthState = {
  isAuthenticated: false,
  accessToken: null,
  refreshToken: null,
  isLoading: false,
};

// 인증 관련 Redux Slice 생성
const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    // 토큰들 설정 (로그인 성공 시)
    setTokens: (
      state,
      action: PayloadAction<{
        accessToken: string;
        refreshToken?: string;
      }>
    ) => {
      state.accessToken = action.payload.accessToken;
      state.isAuthenticated = true;

      // localStorage에 accessToken 영구 저장 (새로고침 대비)
      if (typeof window !== "undefined") {
        localStorage.setItem("accessToken", action.payload.accessToken);
      }

      // refreshToken이 있으면 저장
      if (action.payload.refreshToken) {
        state.refreshToken = action.payload.refreshToken;
        // localStorage에 refreshToken 영구 저장
        if (typeof window !== "undefined") {
          localStorage.setItem("refreshToken", action.payload.refreshToken);
        }
      }
    },

    // accessToken만 업데이트 (토큰 갱신 시)
    updateAccessToken: (state, action: PayloadAction<string>) => {
      state.accessToken = action.payload;
      // localStorage도 함께 업데이트
      if (typeof window !== "undefined") {
        localStorage.setItem("accessToken", action.payload);
      }
    },

    // 인증 정보 완전 초기화 (로그아웃 시)
    clearAuth: (state) => {
      state.isAuthenticated = false;
      state.accessToken = null;
      state.refreshToken = null;
      state.isLoading = false;

      // localStorage에서도 완전 제거
      if (typeof window !== "undefined") {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");

      }
    },

    // 인증 처리 로딩 상태 설정
    setAuthLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },
  },
});

// 액션과 리듀서 내보내기
export const { setTokens, updateAccessToken, clearAuth, setAuthLoading } =
  authSlice.actions;
export default authSlice.reducer;
