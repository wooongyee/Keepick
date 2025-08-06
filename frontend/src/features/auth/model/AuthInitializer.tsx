"use client";

import { useAppDispatch, useAppSelector } from "@/shared/config/hooks";
import { setTokens, clearAuth, setAuthLoading } from "./authSlice";
import { setUser, clearUser, setUserLoading } from "@/entities/user";
import { authApi } from "../api/authApi";
import { useEffect } from "react";
import { usePathname, redirect } from "next/navigation";

interface AuthInitializerProps {
  children: React.ReactNode;
}

export function AuthInitializer({ children }: AuthInitializerProps) {
  const dispatch = useAppDispatch();
  const pathname = usePathname();
  const { isAuthenticated, isLoading: authLoading } = useAppSelector(
    (state) => state.auth
  ); // 인증 로딩 상태 추가
  const { currentUser, isLoading: userLoading } = useAppSelector(
    (state) => state.user
  ); // 사용자 로딩 상태 추가

  // 유저 정보 가져오는 함수
  const fetchUserInfo = async () => {
    dispatch(setUserLoading(true));
    dispatch(setAuthLoading(true));

    try {
      const data = await authApi.getCurrentUser();
      dispatch(setUser(data.user));
    } catch (error) {
      console.error("사용자 정보 조회 실패:", error);
      dispatch(clearAuth());
      dispatch(clearUser());
    } finally {
      dispatch(setUserLoading(false));
      dispatch(setAuthLoading(false));
    }
  };

  useEffect(() => {
    // localStorage에서 토큰 복원
    if (typeof window !== "undefined") {
      const accessToken = localStorage.getItem("accessToken");
      const refreshToken = localStorage.getItem("refreshToken");

      if (accessToken && !isAuthenticated) {
        dispatch(
          setTokens({ accessToken, refreshToken: refreshToken || undefined })
        );
        fetchUserInfo();
      }
    }
  }, [isAuthenticated, dispatch]);

  const hasToken =
    typeof window !== "undefined" && localStorage.getItem("accessToken");
  const isInitializing = authLoading || userLoading;

  // 🔍 디버깅 로그 추가
  // console.log("🔍 AuthInitializer 상태:", {
  //   pathname,
  //   hasToken: !!hasToken,
  //   isAuthenticated,
  //   currentUser: !!currentUser,
  //   authLoading,
  //   userLoading,
  //   isInitializing,
  //   willRedirect: pathname !== "/login" && !hasToken && !isInitializing,
  // });

  // 인증이 필요한 보호된 경로에서만 리다이렉트
  const protectedPaths = ["/profile", "/group", "/chat"];
  const isProtectedPath = pathname
    ? protectedPaths.some((path) => pathname.startsWith(path))
    : false;

  if (isProtectedPath && !hasToken && !isInitializing) {
    // console.log('🚨 보호된 경로에서 리다이렉트 실행!');
    redirect("/login");
  }
  return <>{children}</>;
}
