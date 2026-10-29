import type { Socket } from "socket.io";

type Fetcher = typeof fetch;
type MemberAccess = { token: string; memberId: number; nickname: string };
export function createAccess(required: boolean, apiBase: string, request: Fetcher = fetch) {
  if (required && !/^https?:\/\//.test(apiBase)) throw new Error("API_INTERNAL_URL is required for authenticated realtime");
  async function api(token: string, path: string) {
    const response = await request(new URL(path, apiBase), {
      headers: { Authorization: `Bearer ${token}` }, signal: AbortSignal.timeout(3000),
    });
    if (!response.ok) throw new Error("로그인 또는 그룹 권한을 확인해 주세요.");
    return (await response.json()).data;
  }
  return {
    required,
    async authenticate(socket: Socket, next: (error?: Error) => void) {
      if (!required) { next(); return; }
      try {
        const token = socket.handshake.auth?.token;
        if (typeof token !== "string" || !token || token.length > 8192) throw new Error("로그인이 필요합니다.");
        const member = await api(token, "/api/members/me");
        if (!Number.isSafeInteger(member?.memberId) || typeof member.nickname !== "string") throw new Error("회원 정보를 확인할 수 없습니다.");
        socket.data.keepick = { token, memberId: member.memberId, nickname: member.nickname } satisfies MemberAccess;
        next();
      } catch { next(new Error("로그인이 필요합니다.")); }
    },
    async authorizeRoom(socket: Socket, roomId: unknown) {
      if (!required) return;
      const member = socket.data.keepick as MemberAccess | undefined;
      if (!member || typeof roomId !== "string" || !/^[1-9][0-9]{0,17}$/.test(roomId)) throw new Error("그룹 접근 권한이 없습니다.");
      const groups = await api(member.token, "/api/groups?status=ACCEPTED");
      if (!Array.isArray(groups) || !groups.some(group => String(group.groupId) === roomId && group.invitationStatus === "ACCEPTED")) {
        throw new Error("그룹 접근 권한이 없습니다.");
      }
    },
  };
}
export const realtimeAccess = createAccess(
  process.env.NODE_ENV === "production" || process.env.REALTIME_REQUIRE_AUTH === "true",
  process.env.API_INTERNAL_URL ?? "",
);
