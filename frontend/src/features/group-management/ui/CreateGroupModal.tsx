'use client'

import { useState, useRef } from 'react'
import { useRouter } from 'next/navigation'
import { Upload, X } from 'lucide-react'
import { Modal, ModalContent, ModalHeader, ModalBody, ModalFooter } from '@/shared/ui/modal/Modal'
import { Input } from '@/shared/ui/shadcn/input'
import { Button } from '@/shared/ui/shadcn/button'
import { useGroupManagement } from '../model/useGroupManagement'
import { formValidators } from '@/entities/group'
import { useImageUpload } from '@/features/image-upload'

interface CreateGroupModalProps {
  isOpen: boolean
  onClose: () => void
}

export default function CreateGroupModal({ isOpen, onClose }: CreateGroupModalProps) {
  const router = useRouter()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [groupName, setGroupName] = useState('')
  const [groupDescription, setGroupDescription] = useState('')
  const [thumbnailFile, setThumbnailFile] = useState<File | null>(null)
  const [thumbnailPreview, setThumbnailPreview] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const { createGroup } = useGroupManagement()
  const { uploadImage, isUploading } = useImageUpload()

  // 썸네일 파일 선택 핸들러
  const handleThumbnailSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (file) {
      // 이미지 파일 검증
      if (!file.type.startsWith('image/')) {
        setError('이미지 파일만 선택할 수 있습니다.')
        return
      }
      
      setThumbnailFile(file)
      
      // 미리보기 생성
      const reader = new FileReader()
      reader.onload = (e) => {
        setThumbnailPreview(e.target?.result as string)
      }
      reader.readAsDataURL(file)
      setError(null)
    }
  }

  // 썸네일 제거 핸들러
  const handleThumbnailRemove = () => {
    setThumbnailFile(null)
    setThumbnailPreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
  }

  const handleSubmit = async () => {
    // entities의 폼 검증 사용
    const validation = formValidators.validateGroupNameRealTime(groupName)
    if (!validation.isValid) {
      setError(validation.message || '그룹 이름이 올바르지 않습니다.')
      return
    }

    setError(null)

    try {
      let thumbnailUrl: string | undefined = undefined

      // 선택한 사진만 업로드하고 기본 이미지는 화면에서 표시합니다.
      if (thumbnailFile) {
        console.log('썸네일 이미지 업로드 시작...')
        const uploadResult = await uploadImage(thumbnailFile)
        thumbnailUrl = uploadResult.publicUrl
        console.log('썸네일 업로드 완료:', thumbnailUrl)
      }

      // 그룹 생성 요청 데이터 준비
      const requestData: Parameters<typeof createGroup.mutateAsync>[0] = {
        name: groupName.trim()
      }

      if (groupDescription.trim()) {
        requestData.description = groupDescription.trim()
      }

      if (thumbnailUrl) requestData.groupThumbnailUrl = thumbnailUrl

      console.log('그룹 생성 요청 데이터:', requestData)

      // Tanstack Query mutation 사용 (자동 로딩 상태 관리, 캐시 업데이트, 에러 핸들링)
      const newGroup = await createGroup.mutateAsync(requestData)

      // 성공 시 그룹 페이지로 이동
      router.push(`/group/${newGroup.groupId}`)
      
      // 모달 초기화
      setGroupName('')
      setGroupDescription('')
      setThumbnailFile(null)
      setThumbnailPreview(null)
      if (fileInputRef.current) {
        fileInputRef.current.value = ''
      }
      onClose()
    } catch (error) {
      // 에러는 useGroupManagement에서 이미 처리됨 (토스트 메시지 표시)
      // 여기서는 모달 특화 에러만 처리
      if (error && typeof error === 'object' && 'message' in error) {
        setError(error.message as string)
      }
    }
  }

  const handleClose = () => {
    setGroupName('')
    setGroupDescription('')
    setThumbnailFile(null)
    setThumbnailPreview(null)
    if (fileInputRef.current) {
      fileInputRef.current.value = ''
    }
    setError(null)
    onClose()
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose} placement="top-center" size="md">
      <ModalContent>
        <ModalHeader>
          <h2 className="text-xl font-semibold text-white">새 그룹 만들기</h2>
        </ModalHeader>
        
        <ModalBody>
          <div className="space-y-6">
            <p className="text-sm text-gray-400 text-center">
              추억을 공유하고 싶은 사람들과 함께 하세요!
            </p>
            
            {/* 그룹 썸네일 */}
            <div className="space-y-3">
              <label className="text-sm font-medium text-gray-300 text-center block">
                그룹 썸네일 (선택)
              </label>
              <div className="flex flex-col items-center gap-4">
                <div className="w-32 h-32 bg-gray-700 rounded-xl overflow-hidden flex items-center justify-center border-2 border-gray-600 hover:border-gray-500 transition-colors">
                  {thumbnailPreview ? (
                    <img
                      src={thumbnailPreview}
                      alt="썸네일 미리보기"
                      className="w-full h-full object-cover"
                    />
                  ) : (
                    <img
                      src="/placeholder/photo-placeholder.svg"
                      alt="썸네일 없음"
                      className="w-16 h-16 opacity-40"
                    />
                  )}
                </div>
                <div className="flex gap-3">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => fileInputRef.current?.click()}
                    className="text-gray-300 border-gray-600 hover:bg-gray-700 px-6"
                  >
                    <Upload size={16} className="mr-2" />
                    이미지 선택
                  </Button>
                  {thumbnailPreview && (
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={handleThumbnailRemove}
                      className="text-red-400 border-red-600 hover:bg-red-900/20 px-4"
                    >
                      <X size={16} className="mr-1" />
                      제거
                    </Button>
                  )}
                </div>
              </div>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleThumbnailSelect}
                className="hidden"
              />
              <p className="text-xs text-gray-500 text-center">JPG, PNG, GIF 파일 (최대 5MB)</p>
            </div>
            
            {/* 그룹 이름 */}
            <div className="space-y-2">
              <label htmlFor="groupName" className="text-sm font-medium text-gray-300">
                그룹 이름 <span className="text-orange-400">*</span>
              </label>
              <Input
                id="groupName"
                value={groupName}
                onChange={(e) => setGroupName(e.target.value)}
                placeholder="그룹 이름을 입력하세요"
                className="bg-gray-800 border-gray-600 text-white placeholder-gray-400 focus:border-orange-500"
                maxLength={50}
                onKeyPress={(e) => {
                  if (e.key === 'Enter' && !createGroup.isPending) {
                    handleSubmit()
                  }
                }}
              />
            </div>

            {/* 그룹 설명 */}
            <div className="space-y-2">
              <label htmlFor="groupDescription" className="text-sm font-medium text-gray-300">
                그룹 설명 (선택)
              </label>
              <textarea
                id="groupDescription"
                value={groupDescription}
                onChange={(e) => {
                  if (e.target.value.length <= 100) {
                    setGroupDescription(e.target.value)
                  }
                }}
                placeholder="그룹에 대한 간단한 설명을 입력하세요"
                className="w-full bg-gray-800 border-gray-600 text-white placeholder-gray-400 focus:border-orange-500 rounded-md px-3 py-2 text-sm resize-none focus:outline-none focus:ring-0"
                rows={3}
                maxLength={100}
              />
              <div className="flex justify-between items-center">
                <span className="text-xs text-gray-500">
                  {groupDescription.length}/100자
                </span>
                {groupDescription.length > 80 && (
                  <span className="text-xs text-orange-400">
                    {100 - groupDescription.length}자 남음
                  </span>
                )}
              </div>
            </div>

            {error && (
              <p className="text-sm text-red-400 text-center">{error}</p>
            )}
          </div>
        </ModalBody>
        
        <ModalFooter>
          <Button
            variant="ghost"
            onClick={handleClose}
            disabled={createGroup.isPending || isUploading}
            className="text-gray-300 hover:text-white hover:bg-gray-700"
          >
            취소
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={createGroup.isPending || isUploading || !groupName.trim()}
            className="bg-orange-500 hover:bg-orange-600 text-white"
          >
            {isUploading ? '이미지 업로드 중...' : 
             createGroup.isPending ? '그룹 생성 중...' : '확인'}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  )
}