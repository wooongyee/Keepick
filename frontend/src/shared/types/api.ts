// 공통 API 응답 인터페이스
export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message?: string;
}

// // 인증 관련 타입들
// export interface AuthUser {
//   id: number;
//   name: string;
//   email: string;
//   profileImage?: string;
// }

// export interface AuthResponse {
//   user: AuthUser;
// }

export interface OAuthUrlResponse {
  authUrl: string;
}

// 에러 응답 타입
export interface ApiError {
  message: string;
  code?: string;
  details?: unknown;
}
