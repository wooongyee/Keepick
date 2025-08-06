"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { VersatileInput } from "@/shared/ui";
import { NaverIcon } from "@/shared/assets/NaverIcon";
import { useProfileEdit } from "../model/useProfileEdit";
import { UserProfile } from "../model/types";
import { useAppSelector } from "@/shared/config/hooks";
import Image from "next/image";

interface ProfileFormProps {
  initialProfile?: UserProfile;
}

  export function ProfileForm({ initialProfile }: ProfileFormProps) {
    const { currentUser } = useAppSelector((state) => state.user);

    // Redux 데이터를 우선 사용, prop이 있으면 fallback
    const profileData: UserProfile = initialProfile || {
      profileImage: currentUser?.profileUrl || '/dummy/dummy1.jpg',
      email: currentUser?.email || 'user@example.com',
      socialType: 'naver',
      nickname: currentUser?.nickname || '사용자123',
      aiProfileImage: '/dummy/dummy2.jpg'
    };

    const {
      userProfile,
      nicknameInput,
      setNicknameInput,
      handleNicknameCheck,
      handleNicknameApply,
      handleProfileImageChange,
      handleAiProfileImageChange,
    } = useProfileEdit(profileData);

  return (
    <div className="container mx-auto max-w-4xl">
      <h1 className="text-2xl font-bold mb-6 text-gray-900">나의 프로필</h1>

      {/* 기본 프로필 */}
      <Card className="mb-6">
        <CardHeader>
          <CardTitle className="text-gray-900">기본 프로필</CardTitle>
        </CardHeader>
        <CardContent className="space-y-6">
          <div className="flex items-start space-x-6">
            <div className="flex flex-col items-center space-y-3">
              <Image
                src={userProfile.profileImage}
                alt="프로필 사진"
                width={256}
                height={256}
                className="w-32 h-32 rounded-full object-cover"
                quality={90}
              />
              <Button
                size="sm"
                variant="outline"
                className="bg-white border-gray-300 text-gray-900 hover:bg-gray-50"
                onClick={handleProfileImageChange}
              >
                프로필 사진 변경
              </Button>
            </div>
            <div className="flex-1 space-y-4">
              <VersatileInput
                label="이메일"
                labelIcon={<NaverIcon />}
                value={userProfile.email}
                readOnly={true}
              />
              <VersatileInput
                label="닉네임"
                value={nicknameInput}
                onChange={setNicknameInput}
                placeholder="닉네임을 입력하세요"
                showActionButton={true}
                actionButtonText="중복체크"
                actionButtonLoadingText="확인중..."
                onActionClick={handleNicknameCheck}
                successMessage="사용 가능한 닉네임입니다."
                errorMessage="이미 사용중인 닉네임입니다."
                showApplyButton={true}
                applyButtonText="적용하기"
                onApplyClick={handleNicknameApply}
              />
            </div>
          </div>
        </CardContent>
      </Card>

      {/* AI 인식 프로필 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-gray-900">AI 인식 프로필</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-start space-x-6">
            <div className="flex flex-col items-center space-y-3">
              <Image
                src={userProfile.aiProfileImage}
                alt="AI 인식 프로필"
                width={256}
                height={256}
                className="w-32 h-32 rounded-full object-cover"
                quality={90}
              />
              <Button
                size="sm"
                variant="outline"
                className="bg-white border-gray-300 text-gray-900 hover:bg-gray-50"
                onClick={handleAiProfileImageChange}
              >
                AI 프로필 변경
              </Button>
            </div>
            <div className="flex-1">
              <div className="bg-pink-50 border border-pink-200 rounded-lg p-4">
                <p className="text-sm text-pink-800">
                  AI 인식 프로필은 얼굴 프로필 사진 인식을 위한 사진입니다
                </p>
              </div>
              <Button
                variant="outline"
                className="mt-3 bg-white border-gray-300 text-gray-900 hover:bg-gray-50"
              >
                적절한 사진입니다!
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
