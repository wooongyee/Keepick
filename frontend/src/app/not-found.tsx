// app/not-found.tsx
import Link from "next/link";
import { ArrowLeftIcon, BeakerIcon } from "@heroicons/react/24/outline";

export default function NotFound() {
  return (
    <main className="flex min-h-screen w-full items-center justify-center bg-gray-50 p-6">
      <div className="text-center">
        {/* 아이콘 */}
        <BeakerIcon className="mx-auto h-24 w-24 text-teal-400 mb-6" />

        {/* 메인 헤딩 */}
        <h1 className="text-4xl font-extrabold text-gray-800 tracking-tight sm:text-5xl">
          아직 준비 중인 기능입니다
        </h1>

        {/* 설명 문구 */}
        <p className="mt-4 text-lg text-gray-600 max-w-md mx-auto">
          더 나은 서비스를 위해 열심히 만들고 있어요. 조금만 기다려주시면 멋진
          모습으로 찾아뵙겠습니다!
        </p>

        {/* 홈으로 돌아가기 버튼 */}
        <div className="mt-10">
          <Link
            href="/"
            className="inline-flex items-center gap-2 rounded-lg bg-teal-500 px-6 py-3 text-base font-semibold text-white shadow-sm hover:bg-teal-600 transition-colors"
          >
            <div />
            홈으로 돌아가기
          </Link>
        </div>
      </div>
    </main>
  );
}
