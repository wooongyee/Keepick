'use client';

import { useState } from 'react';
import Header from "@/components/layout/header";
import Sidebar from "@/components/layout/sidebar";
import { ProfileForm } from '@/features/profile';
import { UserProfile } from '@/features/profile';

export default function ProfilePage() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  
  const initialProfile: UserProfile = {
    profileImage: '/dummy/dummy1.jpg',
    email: 'user@example.com',
    socialType: 'naver',
    nickname: '사용자123',
    aiProfileImage: '/dummy/dummy2.jpg'
  };

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <Sidebar sidebarOpen={sidebarOpen} setSidebarOpen={setSidebarOpen} />
      <div
        className={`flex-1 flex flex-col transition-all duration-300 ${
          sidebarOpen ? "lg:ml-64" : "ml-0"
        }`}
      >
        <Header onMenuClick={() => setSidebarOpen(!sidebarOpen)} />

        <main className="flex-1 p-4 sm:p-6 lg:p-8">
          <ProfileForm initialProfile={initialProfile} />
        </main>
      </div>
    </div>
  );
}