'use client'

import * as React from "react"
import { motion, AnimatePresence } from "framer-motion"
import { cn } from "@/shared/lib/utils"
import { GoogleIcon, KakaoIcon, NaverIcon } from "@/shared/assets"

type SocialProvider = "google" | "kakao" | "naver"

interface SocialConfig {
  name: string
  icon: React.ComponentType<{ className?: string }>
  bgColor: string
  textColor: string
}

const socialConfigs: Record<SocialProvider, SocialConfig> = {
  google: {
    name: "Google",
    icon: GoogleIcon,
    bgColor: "bg-white",
    textColor: "text-black",
  },
  kakao: {
    name: "Kakao",
    icon: KakaoIcon,
    bgColor: "bg-[#FEE500]",
    textColor: "text-black",
  },
  naver: {
    name: "Naver",
    icon: NaverIcon,
    bgColor: "bg-[#03C75A]",
    textColor: "text-white",
  },
}

interface SocialLoginButtonProps {
  onGoogleLogin?: () => void
  onKakaoLogin?: () => void
  onNaverLogin?: () => void
  className?: string
  size?: "sm" | "md" | "lg"
  disabled?: boolean
}

export default function SocialLoginButton({
  onGoogleLogin,
  onKakaoLogin,
  onNaverLogin,
  className,
  size = "md",
  disabled = false
}: SocialLoginButtonProps) {
  const [isHovered, setIsHovered] = React.useState(false)
  
  const enabledProviders = process.env.NEXT_PUBLIC_AUTH_PROVIDERS?.split(",")
  const providers = (["google", "kakao", "naver"] as SocialProvider[])
    .filter((provider) => !enabledProviders || enabledProviders.includes(provider))
  
  const handleProviderClick = (provider: SocialProvider) => {
    if (disabled) return
    
    switch (provider) {
      case "google":
        onGoogleLogin?.()
        break
      case "kakao":
        onKakaoLogin?.()
        break
      case "naver":
        onNaverLogin?.()
        break
    }
  }

  const getSizeClasses = () => {
    switch (size) {
      case "sm":
        return "h-7 px-2 text-xs"
      case "md":
        return "h-9 px-3 text-sm"
      case "lg":
        return "h-10 px-4 text-sm"
      default:
        return "h-9 px-3 text-sm"
    }
  }

  const getIconSize = () => {
    switch (size) {
      case "sm":
        return "w-3 h-3"
      case "md":
        return "w-4 h-4"
      case "lg":
        return "w-5 h-5"
      default:
        return "w-4 h-4"
    }
  }

  const containerWidth = isHovered ? providers.length * 120 + (providers.length - 1) * 6 : 80 // 확장 시 너비

  return (
    <div className={cn("relative", className)}>
      <motion.div
        className="flex overflow-hidden rounded-lg transition-colors"
        style={{
          backgroundColor:'#111111',
        }}
        animate={{
          width: containerWidth,
        }}
        transition={{ duration: 0.3, ease: "easeInOut" }}
        onMouseEnter={() => setIsHovered(true)}
        onMouseLeave={() => setIsHovered(false)}
      >
        <AnimatePresence mode="wait">
          {!isHovered ? (
            // 기본 상태: 단일 로그인 버튼
            <motion.div
              key="default"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2 }}
              className={cn(
                "flex items-center justify-center w-full font-medium cursor-pointer text-white",
                getSizeClasses(),
                disabled ? "opacity-50 cursor-not-allowed" : ""
              )}
            >
              로그인
            </motion.div>
          ) : (
            // 확장 상태: 3개 소셜 로그인 버튼
            <motion.div
              key="expanded"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              transition={{ duration: 0.2, delay: 0.1 }}
              className="flex gap-2 p-2"
            >
              {providers.map((provider, index) => {
                const config = socialConfigs[provider]
                const Icon = config.icon

                return (
                  <motion.button
                    key={provider}
                    onClick={() => handleProviderClick(provider)}
                    disabled={disabled}
                    className={cn(
                      "flex items-center justify-center rounded font-medium transition-all duration-200",
                      getSizeClasses(),
                      config.bgColor,
                      config.textColor,
                      disabled ? "opacity-50 cursor-not-allowed" : "cursor-pointer hover:scale-105"
                    )}
                    style={{ width: "110px" }}
                    initial={{ x: 20, opacity: 0 }}
                    animate={{ x: 0, opacity: 1 }}
                    transition={{ duration: 0.2, delay: index * 0.05 }}
                    whileHover={!disabled ? { scale: 1.05 } : {}}
                    whileTap={!disabled ? { scale: 0.95 } : {}}
                  >
                    <Icon className={cn(getIconSize(), "mr-2")} />
                    {config.name}
                  </motion.button>
                )
              })}
            </motion.div>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  )
}
