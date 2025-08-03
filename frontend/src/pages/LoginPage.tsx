import Image from "next/image";
import { LoginWidget } from "@/widgets/auth";
import { Suspense } from "react";

export default function LoginPage() {
  return (
    <div className="flex min-h-screen">
      {/* 왼쪽 섹션: 로그인 폼 (변경 없음) */}
      <div className="flex flex-1 flex-col justify-center px-4 py-12 sm:px-6 lg:flex-none lg:px-20 xl:px-24 bg-gray-50">
        <Suspense fallback={<div>로딩 중...</div>}>
          <LoginWidget />
        </Suspense>
      </div>

      {/* 오른쪽 섹션: 이미지 그리드 (수정된 부분) */}
      <div className="relative hidden w-0 flex-1 lg:block">
        {/* 2. <img>를 <Image>로 교체하고 fill 속성을 사용합니다. */}
        <Image
          src="https://plus.unsplash.com/premium_photo-1696972235468-f2c019227dc0?..."
          alt="Sample background image"
          fill // width/height 대신 fill 속성으로 부모 요소를 채웁니다.
          className="object-cover" // fill이 크기/위치를 담당하므로, object-cover만 남깁니다.
          priority // 페이지의 주요 이미지이므로 로딩 우선순위를 높입니다.
        />
      </div>
    </div>
  );
}
