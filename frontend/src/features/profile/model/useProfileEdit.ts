'use client';

import { useState } from 'react';
import { UserProfile } from './types';

export function useProfileEdit(initialProfile: UserProfile) {
  const [userProfile, setUserProfile] = useState<UserProfile>(initialProfile);
  const [nicknameInput, setNicknameInput] = useState(initialProfile.nickname);

  const handleNicknameCheck = async (nickname: string) => {
    return new Promise<boolean>((resolve) => {
      setTimeout(() => {
        // 실제로는 API 호출
        resolve(true);
      }, 1000);
    });
  };

  const handleNicknameApply = () => {
    setUserProfile({
      ...userProfile,
      nickname: nicknameInput
    });
    alert('닉네임이 변경되었습니다.');
  };

  const handleProfileImageChange = () => {
    alert('프로필 사진 변경 기능');
  };

  const handleAiProfileImageChange = () => {
    alert('AI 프로필 사진 변경 기능');
  };

  const getSocialTypeLabel = (type: string) => {
    switch (type) {
      case 'naver': return '네이버';
      case 'kakao': return '카카오';
      case 'google': return '구글';
      default: return type;
    }
  };

  return {
    userProfile,
    nicknameInput,
    setNicknameInput,
    handleNicknameCheck,
    handleNicknameApply,
    handleProfileImageChange,
    handleAiProfileImageChange,
    getSocialTypeLabel,
  };
}