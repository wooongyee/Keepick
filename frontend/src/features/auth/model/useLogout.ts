import { useAppDispatch } from "@/shared/config/hooks";
import { clearAuth } from "./authSlice";
import { clearUser } from "@/entities/user";
import { useRouter } from "next/navigation";

export const useLogout = () => {
  const dispatch = useAppDispatch();
  const router = useRouter();

  const logout = () => {
    dispatch(clearAuth()); // localStorage 토큰 제거
    dispatch(clearUser()); // 사용자 정보 제거 
    router.push("/login");
  };

  return logout;
};
