// 토큰 갱신 응답 인터페이스
export interface TokenRefreshResponse {
  accessToken: string;
  refreshToken?: string; // 새 refreshToken (선택적)
}

// 사용자 정보 응답 인터페이스
export interface UserResponse {
  user: {
    id: number;
    email: string;
    nickname: string;
    profileUrl?: string;
    createdAt: string;
    updatedAt: string;
  };
}