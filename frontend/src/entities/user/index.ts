// entities/user/index.ts
// 사용자 관련 모든 요소를 한 곳에서 내보내기 (배럴 패턴)

export { default as userReducer } from "./model/userSlice";
export {
  setUser,
  clearUser,
  setUserLoading,
  updateUser,
} from "./model/userSlice";
export type { User } from "./model/userSlice";
