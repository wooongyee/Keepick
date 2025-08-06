// features/auth/api/authApi.ts
import { apiClient } from "@/shared/api/http";
import { TokenRefreshResponse, UserResponse } from "./types";


// 인증 관련 API 함수들
export const authApi = {
  // 현재 사용자 정보 조회
  getCurrentUser: async (): Promise<UserResponse> => {
    const response = await apiClient.get<UserResponse>("/api/members/me");
    return response.data;
  },

  // refreshToken으로 accessToken 갱신
  refreshToken: async (refreshToken: string): Promise<TokenRefreshResponse> => {
    // apiClient를 사용하지 않음 (순환 참조 방지)
    const response = await fetch(
      `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/auth/refresh`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken }),
      }
    );

    if (!response.ok) {
      throw new Error("Token refresh failed");
    }

    return response.json();
  },

  // 로그아웃 API 호출
  logout: async (): Promise<void> => {
    await apiClient.post("/api/logout");
  },
};
