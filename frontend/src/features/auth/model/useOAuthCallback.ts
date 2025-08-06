// features/auth/hooks/useOAuthCallback.ts
import { useSearchParams, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useAppDispatch } from "@/shared/config/hooks";
import { setTokens, setAuthLoading } from "./authSlice";
import { setUser, setUserLoading } from "@/entities/user";
import { authApi } from "../api/authApi";

// OAuth2 콜백 처리를 위한 커스텀 훅
export const useOAuthCallback = () => {
  const searchParams = useSearchParams();
  const router = useRouter();
  const dispatch = useAppDispatch();

  useEffect(() => {
    const run = async () => {
      // URL에서 토큰들과 에러 추출
      const accessToken =
        searchParams?.get("token") || searchParams?.get("accessToken");
      const refreshToken = searchParams?.get("refreshToken");
      const error = searchParams?.get("error");

      // 🔍 디버깅 로그 추가
      // console.log("🔍 OAuth Callback 상태:", {
      //   accessToken: !!accessToken,
      //   refreshToken: !!refreshToken,
      //   error,
      //   searchParams: searchParams?.toString(),
      // });

      if (error) {
        console.error("OAuth2 Error:", error);
        router.replace("/login?error=" + encodeURIComponent(error));
        return;
      }

      if (accessToken) {
        // console.log("✅ 토큰 발견, 저장 시작");

        // 1. 토큰 저장 먼저
        dispatch(
          setTokens({
            accessToken,
            refreshToken: refreshToken || undefined,
          })
        );

        // 2. 사용자 정보 가져오기
        await fetchUserInfo(); // await 추가

        // 3. URL 정리 (마지막에)
        router.replace("/");
      }
    };

    run();
  }, [searchParams, router, dispatch]);

  // 사용자 정보를 API에서 가져와서 Redux에 저장
  const fetchUserInfo = async () => {
    // 로딩 상태 시작
    dispatch(setUserLoading(true));
    dispatch(setAuthLoading(true));

    try {
      // /api/members/me 호출하여 사용자 정보 조회
      const data = await authApi.getCurrentUser();
      // entities/user에 사용자 정보 저장
      dispatch(setUser(data.user));
    } catch (error) {
      console.error("User info fetch error:", error);
      // 사용자 정보 가져오기 실패 시 에러 페이지로
      router.replace("/login?error=fetch_failed");
    } finally {
      // 로딩 상태 종료
      dispatch(setUserLoading(false));
      dispatch(setAuthLoading(false));
    }
  };
};
