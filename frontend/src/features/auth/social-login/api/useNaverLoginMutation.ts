import { useMutation } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useAppDispatch } from "@/shared/config/hooks";
import { setAuth } from "@/shared/store/features/auth/authSlice";

// 1. API 호출 함수 정의
const sendAuthCodeToBackend = async (code: string) => {
  const API_ENDPOINT = "https://api.keepick.com/api/auth/login/naver";
  const response = await fetch(API_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code }),
  });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || "서버 통신 중 오류가 발생했습니다.");
  }
  return response.json();
};

// 2. API 호출과 성공/실패 로직을 캡슐화한 커스텀 훅
export const useNaverLoginMutation = () => {
  const router = useRouter();
  const dispatch = useAppDispatch();

  return useMutation({
    mutationFn: sendAuthCodeToBackend,
    onSuccess: (data) => {
      // 성공 시 Redux 상태 업데이트와 라우팅을 여기서 처리
      dispatch(
        setAuth({
          // 실제로는 백엔드가 준 사용자 정보와 토큰을 사용해야 합니다.
          user: { id: 1, name: "User", email: "user@email.com" },
          accessToken: data.accessToken,
        })
      );
      router.replace("/");
    },
    onError: (err) => {
      // 실패 시 에러 처리와 라우팅
      console.error("로그인 처리 중 에러 발생:", err.message);
      router.replace(`/login?error=${encodeURIComponent(err.message)}`);
    },
  });
};
