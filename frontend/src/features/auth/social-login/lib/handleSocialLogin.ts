import { SocialProvider } from "../model/types";

export const handleSocialLogin = (provider: SocialProvider) => {
  const redirectUri = process.env.NEXT_PUBLIC_NAVER_REDIRECT_URI!;
  let url = "";

  switch (provider) {
    case "naver": {
      const clientId = process.env.NEXT_PUBLIC_NAVER_CLIENT_ID!;
      const state = process.env.NAVER_STATE_SECRET!;

      url = `https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=${clientId}&redirect_uri=${redirectUri}&state=${state}`;
      break;
    }
    case "kakao":
      // 여기에 카카오 로그인 URL 생성 로직 추가
      console.log("Kakao login is not implemented yet.");
      return;
    case "google":
      // 여기에 구글 로그인 URL 생성 로직 추가
      console.log("Google login is not implemented yet.");
      return;
    default:
      throw new Error("Unsupported social provider");
  }

  // 생성된 URL로 사용자를 리다이렉트
  window.location.href = url;
};
