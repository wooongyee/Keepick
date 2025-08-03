"use client"; // useSearchParams를 사용하기 위해 클라이언트 컴포넌트로 전환

import { useSearchParams } from "next/navigation";
import { SocialLoginButton } from "@/features/auth";

// 에러 메시지를 표시할 컴포넌트
function ErrorMessage() {
  const searchParams = useSearchParams();
  const error = searchParams?.get("error");

  if (!error) return null;

  return (
    <div className="my-4 rounded-md bg-red-50 p-4">
      <p className="text-sm font-medium text-red-800">
        로그인 실패: {decodeURIComponent(error)}
      </p>
    </div>
  );
}

export function LoginWidget() {
  return (
    <div className="mx-auto w-full max-w-sm lg:w-96">
      <div>
        <h2 className="text-3xl font-bold tracking-tight text-gray-900">
          Sign in
        </h2>
        <p className="mt-2 text-sm text-gray-600">Welcome to KeePick!</p>
      </div>

      <div className="mt-8">
        {/* 에러 메시지 컴포넌트 추가 */}
        <ErrorMessage />
        <div className="space-y-4">
          <SocialLoginButton provider="kakao" />
          <SocialLoginButton provider="naver" />
        </div>
      </div>
    </div>
  );
}
