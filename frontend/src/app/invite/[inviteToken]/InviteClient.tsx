"use client"; // 이 컴포넌트는 브라우저에서 실행됩니다.

import { useEffect } from "react";

// 부모에게서 평범한 문자열인 inviteToken을 props로 받습니다.
export default function InviteClient({ inviteToken }: { inviteToken: string }) {
  useEffect(() => {
    // 이 로직은 이제 안전하게 브라우저에서 실행됩니다.
    const appUri = `keepick://invite/${inviteToken}`;
    const playStoreUrl =
      "https://play.google.com/store/apps/details?id=com.ssafy.keepick";

    // 앱 실행 시도
    window.location.href = appUri;

    // 2.5초 후 앱이 열리지 않았다면 스토어로 보내는 안전장치
    const timer = setTimeout(() => {
      if (!document.hidden) {
        window.location.href = playStoreUrl;
      }
    }, 2500);

    return () => clearTimeout(timer);
  }, [inviteToken]);

  // 사용자가 잠시 보게 될 로딩 화면
  return (
    <div style={{ padding: "40px", textAlign: "center" }}>
      <h1>KeePick 그룹 초대</h1>
      <p style={{ fontSize: "18px", marginTop: "20px", color: "#555" }}>
        KeePick 앱을 실행하고 있습니다...
      </p>
      <p style={{ color: "#888" }}>
        앱이 자동으로 열리지 않으면 잠시만 기다려주세요.
      </p>
    </div>
  );
}
