"use client";

import { useEffect } from "react";
import { useSearchParams } from "next/navigation";
// 1. 방금 만든 커스텀 훅을 import 합니다.
import { useNaverLoginMutation } from "../api/useNaverLoginMutation";

export function NaverLoginHandler() {
  const searchParams = useSearchParams();

  // 2. 커스텀 훅을 호출하여 필요한 모든 것을 가져옵니다.
  // API 호출, 성공/실패 로직은 모두 훅 내부에 캡슐화되어 있습니다.
  const { mutate, isPending, isError, error } = useNaverLoginMutation();

  useEffect(() => {
    if (searchParams) {
      const code = searchParams.get("code");
      if (code) {
        // 3. 컴포넌트는 그저 "코드가 있으면 mutate를 실행한다"는 책임만 가집니다.
        mutate(code);
      }
    }
  }, [searchParams, mutate]);

  // --- UI 피드백 렌더링 (이 부분은 UI의 책임이므로 여기에 남습니다) ---
  if (isPending) {
    return <p>로그인 중입니다. 잠시만 기다려주세요...</p>;
  }

  if (isError) {
    return (
      <div>
        <p>로그인 중 오류가 발생했습니다.</p>
        <p>오류: {error.message}</p>
      </div>
    );
  }

  return null;
}
