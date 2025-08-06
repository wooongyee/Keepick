import { SocialProvider } from "../types";
// import { apiClient } from "@/shared/api/http";
// import { OAuthUrlResponse } from "@/shared/types/api";

export const handleSocialLogin = async (provider: SocialProvider) => {
  try {
    // 타입을 명시적으로 지정
    // 직접 브라우저 리다이렉트 - API 호출 불필요
    const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "";

    window.location.href = `${baseUrl}/api/oauth2/authorization/${provider}`;
  } catch (error) {
    console.error(`${provider} 로그인 요청 중 오류:`, error);
  }
};
