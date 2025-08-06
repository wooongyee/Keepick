"use client"

import type React from "react"

import { useRouter } from "next/navigation"

export default function ChatPage() {
  const router = useRouter()

  const handleBackClick = () => {
    router.push("/")
  }

  const participants = [
    { name: "참가자 1", color: "from-indigo-400 to-blue-500" },
    { name: "참가자 2", color: "from-green-400 to-emerald-500" },
    { name: "참가자 3", color: "from-orange-400 to-red-500" },
    { name: "참가자 4", color: "from-purple-400 to-pink-500" },
    { name: "참가자 5", color: "from-yellow-400 to-orange-500" },
    { name: "참가자 6", color: "from-teal-400 to-cyan-500" },
    { name: "참가자 7", color: "from-rose-400 to-pink-500" },
  ]

  const photos = Array.from({ length: 10 }, (_, i) => ({
    id: i + 1,
    color: [
      "bg-purple-200",
      "bg-green-200",
      "bg-blue-200",
      "bg-orange-200",
      "bg-gray-200",
      "bg-indigo-200",
      "bg-pink-200",
    ][i % 7],
  }))


  return (
    <div className="h-screen bg-[var(--bg-dark)]">
      <div className="flex flex-col p-6 gap-6 h-full">
        {/* Top Bar */}
        <div className="flex justify-between items-center bg-white p-4 rounded-3xl shadow-lg">
          <div className="flex items-center gap-3">
            <button
              onClick={handleBackClick}
              className="text-2xl text-[var(--text-dark)] hover:text-[var(--primary-color)] transition-colors cursor-pointer"
            >
              <span>←</span>
            </button>
            <div>
              <h1 className="font-montserrat text-2xl font-bold text-[var(--text-dark)]">그룹 채팅방: 프로젝트 킵픽</h1>
              <p className="text-sm text-gray-600">사진 정리 & 실시간 소통</p>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-3 cursor-pointer">
              <div className="w-9 h-9 bg-[var(--primary-color)] rounded-full flex items-center justify-center text-white font-bold">
                W
              </div>
              <span className="font-semibold text-[var(--text-dark)]">wmwogus</span>
              <span className="text-sm text-gray-500">▼</span>
            </div>
            <button className="text-2xl text-[var(--text-dark)]">
              <span>☰</span>
            </button>
          </div>
        </div>

        {/* Video Grid */}
        <div className="flex-1 grid grid-cols-[repeat(auto-fit,minmax(200px,1fr))] auto-rows-[minmax(150px,1fr)] gap-4 bg-[#2c3e50] rounded-3xl p-4 shadow-inner">
          {participants.map((participant, index) => (
            <div
              key={index}
              className={`bg-gradient-to-br ${participant.color} rounded-2xl flex items-center justify-center text-white font-semibold text-lg relative overflow-hidden group cursor-pointer`}
            >
              <img
                src={`/placeholder.svg?height=300&width=400&text=${participant.name}`}
                alt={participant.name}
                className="w-full h-full object-cover"
              />
              <div className="absolute bottom-2 left-2 bg-black/40 text-white px-2 py-1 rounded text-sm font-medium opacity-0 group-hover:opacity-100 transition-opacity">
                {participant.name}
              </div>
            </div>
          ))}
        </div>

        {/* Photo Tray */}
        <div className="h-32 bg-white rounded-3xl shadow-lg flex items-center p-3 gap-3 overflow-x-auto">
          {photos.map((photo) => (
            <div
              key={photo.id}
              className={`flex-shrink-0 w-26 h-26 ${photo.color} rounded-2xl shadow-md cursor-grab hover:-translate-y-1 hover:shadow-lg transition-all flex items-center justify-center`}
              draggable
            >
              <img
                src={`/placeholder.svg?height=100&width=100&text=사진${photo.id}`}
                alt={`사진 ${photo.id}`}
                className="w-full h-full object-cover rounded-2xl"
              />
            </div>
          ))}
        </div>
      </div>
    </div>
  )
}
