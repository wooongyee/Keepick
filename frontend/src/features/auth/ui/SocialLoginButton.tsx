"use client";

import { handleSocialLogin } from "../model/handleSocialLogin";
import { NaverIcon } from "@/shared/assets/NaverIcon";
import { KakaoIcon } from "@/shared/assets/KakaoIcon";
import { GoogleIcon } from "@/shared/assets/GoogleIcon";
import { SocialLoginButtonProps } from "./types";

const providerStyles = {
  naver: "bg-[#03C75A] text-white hover:bg-[#03C75A]/90",
  kakao: "bg-[#FEE500] text-black hover:bg-[#FEE500]/90",
  google: "bg-white text-black border border-gray-300 hover:bg-gray-50",
};

const providerLabels = {
  naver: "네이버 로그인",
  kakao: "카카오 로그인",
  google: "Google 로그인",
};

const providerIcons = {
  naver: <NaverIcon />,
  kakao: <KakaoIcon />,
  google: <GoogleIcon />, 
};



export function SocialLoginButton({ provider }: SocialLoginButtonProps) {
  return (
    <button
      onClick={() => handleSocialLogin(provider)}
      className={`flex w-full items-center justify-center gap-3 rounded-md px-3 py-3 text-sm font-semibold
  focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 ${providerStyles[provider]}`}
    >
      <span className="h-6 w-6">{providerIcons[provider]}</span>
      <span>{providerLabels[provider]}</span>
    </button>
  );
}
