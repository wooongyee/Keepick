// src/app/groupchat/[groupId]/page.tsx
import type { Metadata } from "next";
import { VideoConference } from "@/widgets/video-conference/ui/VideoConference";

// ✅ Promise 타입을 명시적으로 포함한 정확한 타입 정의
interface PageProps {
  params: Promise<{ groupId: string }>;
}

export async function generateMetadata({
  params,
}: PageProps): Promise<Metadata> {
  const { groupId: roomId } = await params;

  return {
    title: `그룹 통화 - ${roomId}`,
    description: `그룹 ${roomId}에서 화상 통화에 참여하세요.`,
  };
}

const GroupChatPage = async ({ params }: PageProps) => {
  const { groupId: roomId } = await params;

  return <VideoConference initialRoomId={roomId} />;
};

export default GroupChatPage;
