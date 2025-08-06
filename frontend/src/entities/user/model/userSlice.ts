// entities/user/model/userSlice.ts
import { createSlice, PayloadAction } from "@reduxjs/toolkit";

// 사용자 엔티티 인터페이스 정의
export interface User {
  id: number;
  email: string;
  nickname: string;
  profileUrl?: string; // 선택적 프로필 이미지
  createdAt: string;
  updatedAt: string;
}

// 사용자 상태 인터페이스
interface UserState {
  currentUser: User | null; // 현재 로그인한 사용자 정보
  isLoading: boolean; // 사용자 정보 로딩 상태
}

// 초기 상태 정의
const initialState: UserState = {
  currentUser: null,
  isLoading: false,
};

// 사용자 관련 Redux Slice 생성
const userSlice = createSlice({
  name: "user",
  initialState,
  reducers: {
    // 사용자 정보 설정 (로그인 성공 시)
    setUser: (state, action: PayloadAction<User>) => {
      state.currentUser = action.payload;
      state.isLoading = false; // 로딩 완료
    },

    // 사용자 정보 초기화 (로그아웃 시)
    clearUser: (state) => {
      state.currentUser = null;
      state.isLoading = false;
    },

    // 사용자 정보 로딩 상태 설정
    setUserLoading: (state, action: PayloadAction<boolean>) => {
      state.isLoading = action.payload;
    },

    // 사용자 정보 부분 업데이트 (프로필 수정 시)
    updateUser: (state, action: PayloadAction<Partial<User>>) => {
      if (state.currentUser) {
        // 기존 사용자 정보와 새 정보 병합
        state.currentUser = { ...state.currentUser, ...action.payload };
      }
    },
  },
});

// 액션과 리듀서 내보내기
export const { setUser, clearUser, setUserLoading, updateUser } =
  userSlice.actions;
export default userSlice.reducer;
