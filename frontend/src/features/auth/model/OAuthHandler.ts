"use client";

import { useOAuthCallback } from "@/features/auth/model/useOAuthCallback";

// 클라이언트 전용 OAuthHandler

export default function OAuthHandler() {
  useOAuthCallback(); // 토큰 파싱만 처리
  return null; // UI는 렌더링하지 않음
}