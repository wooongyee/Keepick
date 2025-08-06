// 인증 상태 인터페이스 (사용자 데이터는 포함하지 않음)
export interface AuthState {
  isAuthenticated: boolean; // 인증 여부
  accessToken: string | null; // JWT 액세스 토큰
  refreshToken: string | null; // JWT 리프레시 토큰
  isLoading: boolean; // 인증 처리 로딩 상태
}