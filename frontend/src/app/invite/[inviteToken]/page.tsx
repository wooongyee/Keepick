import InviteClient from "./InviteClient"; // 방금 만든 자식 컴포넌트를 임포트합니다.

// props 타입은 공식 문서와 동일하게 Promise를 명시합니다.
type InvitePageProps = {
  params: Promise<{ inviteToken: string }>;
};

// 이 페이지는 async 서버 컴포넌트입니다.
export default async function InvitePage({ params }: InvitePageProps) {
  // await으로 Promise에서 실제 토큰 값을 추출합니다.
  const { inviteToken } = await params;

  // 이제 이 페이지는 UI 렌더링을 자식에게 위임합니다.
  // 실제 값으로 변환된 inviteToken을 props로 넘겨줍니다.
  return <InviteClient inviteToken={inviteToken} />;
}
