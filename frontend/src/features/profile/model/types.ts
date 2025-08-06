export interface UserProfile {
  profileImage: string;
  email: string;
  socialType: 'naver' | 'kakao' | 'google';
  nickname: string;
  aiProfileImage: string;
}