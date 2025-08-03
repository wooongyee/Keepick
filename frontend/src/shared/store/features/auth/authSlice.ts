import { createSlice, PayloadAction } from "@reduxjs/toolkit";

// 우리 앱에서 사용할 사용자 정보의 타입
interface User {
  id: number;
  name: string;
  email: string;
}

// 이 Slice가 관리할 상태(state)의 타입
interface AuthState {
  isAuthenticated: boolean;
  user: User | null;
  accessToken: string | null;
}

// 초기 상태
const initialState: AuthState = {
  isAuthenticated: false,
  user: null,
  accessToken: null,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  // 상태를 변경하는 함수들을 정의합니다.
  reducers: {
    // 로그인 성공 시 호출될 리듀서
    setAuth: (
      state,
      action: PayloadAction<{ user: User; accessToken: string }>
    ) => {
      state.isAuthenticated = true;
      state.user = action.payload.user;
      state.accessToken = action.payload.accessToken;
    },
    // 로그아웃 시 호출될 리듀서
    clearAuth: (state) => {
      state.isAuthenticated = false;
      state.user = null;
      state.accessToken = null;
    },
  },
});

// 다른 컴포넌트에서 사용할 수 있도록 액션과 리듀서를 export 합니다.
export const { setAuth, clearAuth } = authSlice.actions;
export default authSlice.reducer;
