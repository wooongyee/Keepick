// src/widgets/video-conference/ui/GestureRecognizer.tsx
"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import * as tf from "@tensorflow/tfjs";
import { HandLandmarker, FilesetResolver } from "@mediapipe/tasks-vision";
import type {
  HandLandmarkerResult,
  NormalizedLandmark,
} from "@mediapipe/tasks-vision";
import { CpuChipIcon, EyeIcon, PowerIcon } from "@heroicons/react/24/solid";

// --- 설정 상수 ---
const SEQUENCE_LENGTH = 30;
const CONFIDENCE_THRESHOLD = 0.92;
const GESTURE_COOLDOWN = 3000;

// [개선] UI 표시를 위해 레이블에 이모지를 포함
const STATIC_LABELS = [
  "bad",
  "fist",
  "good",
  "gun",
  "heart",
  "none",
  "ok",
  "open_palm",
  "promise",
  "rock",
  "victory",
];
const KOREAN_STATIC_LABELS: { [key: string]: string } = {
  bad: "👎 따봉 반대",
  fist: "✊ 주먹",
  good: "👍 따봉",
  gun: "👉 총 모양",
  heart: "🫶 손가락 하트",
  none: "없음",
  ok: "👌 OK",
  open_palm: "✋ 손바닥",
  promise: "🤙 약속",
  rock: "🤘 락앤롤",
  victory: "✌️ 브이",
};

const DYNAMIC_LABELS = ["fire", "hi", "hit", "none", "nono", "nyan", "shot"];
const KOREAN_DYNAMIC_LABELS: { [key: string]: string } = {
  fire: "🔥 파이어",
  hi: "👋 안녕",
  hit: "💥 히트",
  none: "없음",
  nono: "🚫 안돼",
  nyan: "🐾 냥냥펀치",
  shot: "💖 샷",
};

// [추가] Gesture 상태를 위한 타입 정의
type GestureState = {
  label: string;
  emoji: string;
  statusIcon: React.ReactNode;
};

interface GestureRecognizerProps {
  mediaStream: MediaStream | null;
  isStaticOn: boolean;
  isDynamicOn: boolean;
  // [추가] 사용자 정보 (예: 로컬 유저 이름)
  userName?: string;
}

// [추가] 제스처 표시를 위한 내부 컴포넌트
const GestureDisplayCard: React.FC<{
  title: string;
  state: GestureState;
  position: "top-left" | "top-right";
}> = ({ title, state, position }) => {
  const styles: React.CSSProperties = {
    position: "absolute",
    zIndex: 20,
    backgroundColor: "rgba(17, 24, 39, 0.7)", // gray-900/70
    backdropFilter: "blur(4px)",
    color: "white",
    padding: "8px 16px", // px-4 py-2
    borderRadius: "8px", // rounded-lg
    boxShadow: "0 4px 6px -1px rgba(0,0,0,0.1), 0 2px 4px -2px rgba(0,0,0,0.1)", // shadow-lg
    display: "flex",
    alignItems: "center",
    gap: "12px", // gap-3
    animation: "pop-in 0.3s ease-out forwards",
    border: "1px solid rgba(255, 255, 255, 0.1)", // border-white/10
  };

  if (position === "top-left") {
    styles.top = "10px";
    styles.left = "10px";
  } else {
    // top-right
    styles.top = "10px";
    styles.right = "10px";
  }

  return (
    <div style={styles}>
      <div className="flex-shrink-0 text-teal-400">{state.statusIcon}</div>
      <div>
        <div className="text-xs text-gray-400">{title}</div>
        <div className="flex items-center gap-2">
          <span className="text-2xl">{state.emoji}</span>
          <span className="font-semibold">{state.label}</span>
        </div>
      </div>
    </div>
  );
};

export const GestureRecognizer: React.FC<GestureRecognizerProps> = ({
  mediaStream,
  isStaticOn,
  isDynamicOn,
  userName = "나 (You)", // 기본값 설정
}) => {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const staticModelRef = useRef<tf.LayersModel | null>(null);
  const dynamicModelRef = useRef<tf.LayersModel | null>(null);
  const handLandmarkerRef = useRef<HandLandmarker | null>(null);
  const sequenceRef = useRef<number[][]>([]);

  // [개선] UI 상태를 더 구조적인 객체로 관리 - 불필요한 리렌더링 방지
  const [staticGestureState, setStaticGestureState] = useState<GestureState>({
    label: "준비 중...",
    emoji: "",
    statusIcon: <CpuChipIcon className="w-5 h-5" />,
  });
  const [dynamicGestureState, setDynamicGestureState] = useState<GestureState>({
    label: "준비 중...",
    emoji: "",
    statusIcon: <CpuChipIcon className="w-5 h-5" />,
  });

  const [visualEffect, setVisualEffect] = useState<string | null>(null);
  const animationFrameId = useRef<number | null>(null);
  const lastVideoTimeRef = useRef<number>(-1);
  const lastEffectTimeRef = useRef<number>(0);

  // [개선] 상태 업데이트 로직을 useCallback으로 감싸 안정성 확보
  const updateStaticGesture = useCallback((newState: GestureState) => {
    setStaticGestureState((prev) => {
      // 이전 상태와 동일하면 업데이트하지 않아 불필요한 리렌더링 방지
      if (prev.label === newState.label && prev.emoji === newState.emoji) {
        return prev;
      }
      return newState;
    });
  }, []);

  const updateDynamicGesture = useCallback((newState: GestureState) => {
    setDynamicGestureState((prev) => {
      if (prev.label === newState.label && prev.emoji === newState.emoji) {
        return prev;
      }
      return newState;
    });
  }, []);

  // 정적/동적 기능 On/Off에 따른 UI 상태 변경
  useEffect(() => {
    if (!isStaticOn) {
      updateStaticGesture({
        label: "꺼짐",
        emoji: "🚫",
        statusIcon: <PowerIcon className="w-5 h-5" />,
      });
    } else if (staticModelRef.current) {
      updateStaticGesture({
        label: "인식 중",
        emoji: "👀",
        statusIcon: <EyeIcon className="w-5 h-5" />,
      });
    }
  }, [isStaticOn, updateStaticGesture]);

  useEffect(() => {
    if (!isDynamicOn) {
      updateDynamicGesture({
        label: "꺼짐",
        emoji: "🚫",
        statusIcon: <PowerIcon className="w-5 h-5" />,
      });
      sequenceRef.current = [];
    } else if (dynamicModelRef.current) {
      updateDynamicGesture({
        label: "움직여보세요",
        emoji: "🌊",
        statusIcon: <EyeIcon className="w-5 h-5" />,
      });
    }
  }, [isDynamicOn, updateDynamicGesture]);

  // 모델 초기화
  useEffect(() => {
    async function setupAllModels() {
      try {
        const vision = await FilesetResolver.forVisionTasks(
          "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
        );
        handLandmarkerRef.current = await HandLandmarker.createFromOptions(
          vision,
          {
            baseOptions: {
              modelAssetPath: `https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task`,
              delegate: "GPU",
            },
            runningMode: "VIDEO",
            numHands: 1,
          }
        );
        const [staticModel, dynamicModel] = await Promise.all([
          tf.loadLayersModel("/static_model/model.json"),
          tf.loadLayersModel("/dynamic_model/model.json"),
        ]);
        staticModelRef.current = staticModel;
        dynamicModelRef.current = dynamicModel;

        if (isStaticOn)
          updateStaticGesture({
            label: "인식 중",
            emoji: "👀",
            statusIcon: <EyeIcon className="w-5 h-5" />,
          });
        if (isDynamicOn)
          updateDynamicGesture({
            label: "움직여보세요",
            emoji: "🌊",
            statusIcon: <EyeIcon className="w-5 h-5" />,
          });
      } catch (error) {
        console.error("AI 모델 초기화 실패:", error);
        updateStaticGesture({
          label: "모델 로딩 실패",
          emoji: "💔",
          statusIcon: <CpuChipIcon className="w-5 h-5" />,
        });
        updateDynamicGesture({
          label: "모델 로딩 실패",
          emoji: "💔",
          statusIcon: <CpuChipIcon className="w-5 h-5" />,
        });
      }
    }
    setupAllModels();
    return () => {
      if (animationFrameId.current)
        cancelAnimationFrame(animationFrameId.current);
      handLandmarkerRef.current?.close();
      staticModelRef.current?.dispose();
      dynamicModelRef.current?.dispose();
    };
  }, [isStaticOn, isDynamicOn, updateStaticGesture, updateDynamicGesture]); // 의존성 추가

  // MediaStream 연결 및 예측 루프 시작
  useEffect(() => {
    if (mediaStream && videoRef.current) {
      videoRef.current.srcObject = mediaStream;
      const startLoopOnLoad = () => startPredictionLoop();
      videoRef.current.addEventListener("loadeddata", startLoopOnLoad);
      // [개선] 스트림 변경 시 이전 캔버스 내용 지우기
      clearCanvas();
      return () => {
        videoRef.current?.removeEventListener("loadeddata", startLoopOnLoad);
        if (animationFrameId.current)
          cancelAnimationFrame(animationFrameId.current);
      };
    }
    return () => {
      if (animationFrameId.current)
        cancelAnimationFrame(animationFrameId.current);
    };
  }, [mediaStream]);

  // 예측 루프
  const startPredictionLoop = useCallback(() => {
    const predict = async () => {
      const video = videoRef.current;
      const handLandmarker = handLandmarkerRef.current;
      const staticModel = staticModelRef.current;
      const dynamicModel = dynamicModelRef.current;

      if (
        !video ||
        !handLandmarker ||
        !staticModel ||
        !dynamicModel ||
        video.readyState < 2
      ) {
        animationFrameId.current = requestAnimationFrame(predict);
        return;
      }

      if (video.currentTime !== lastVideoTimeRef.current) {
        lastVideoTimeRef.current = video.currentTime;
        const handLandmarkerResult = handLandmarker.detectForVideo(
          video,
          Date.now()
        );

        if (
          handLandmarkerResult.landmarks &&
          handLandmarkerResult.landmarks.length > 0
        ) {
          const landmarks = handLandmarkerResult.landmarks[0];
          // drawLandmarks(landmarks); // 필요 시 랜드마크 그리기 활성화 (현재 opacity: 0)

          // 데이터 전처리 (두 모델이 공통으로 사용)
          // [개선] wrist를 landmarks[0]으로 사용하는 것이 아닌, 실제 wrist landmark (0번 인덱스) 사용
          const wrist = landmarks[0];
          const keypoints = landmarks.flatMap((lm) => [
            lm.x - wrist.x,
            lm.y - wrist.y,
            lm.z - wrist.z,
          ]);

          tf.tidy(() => {
            const inputTensor = tf.tensor2d([keypoints], [1, 63]);
            const prediction = staticModel.predict(inputTensor) as tf.Tensor;
            const label = STATIC_LABELS[prediction.argMax(-1).dataSync()[0]];
            const [emoji, ...text] = (
              KOREAN_STATIC_LABELS[label] || label
            ).split(" ");
            updateStaticGesture({
              label: text.join(" "),
              emoji,
              statusIcon: <EyeIcon className="w-5 h-5" />,
            });
          });

          sequenceRef.current.push(keypoints);
          sequenceRef.current = sequenceRef.current.slice(-SEQUENCE_LENGTH);

          if (sequenceRef.current.length === SEQUENCE_LENGTH) {
            tf.tidy(() => {
              const inputTensor = tf.tensor3d(
                [sequenceRef.current],
                [1, SEQUENCE_LENGTH, 63]
              );
              const prediction = dynamicModel.predict(inputTensor) as tf.Tensor;
              const confidence = Math.max(...prediction.dataSync());
              let label = "none";
              if (confidence >= CONFIDENCE_THRESHOLD) {
                label = DYNAMIC_LABELS[prediction.argMax(-1).dataSync()[0]];
                const currentTime = Date.now();
                if (
                  label !== "none" &&
                  label !== "nono" &&
                  currentTime - lastEffectTimeRef.current > GESTURE_COOLDOWN
                ) {
                  lastEffectTimeRef.current = currentTime;
                  const emoji = (KOREAN_DYNAMIC_LABELS[label] || "").split(
                    " "
                  )[0];
                  setVisualEffect(emoji);
                  setTimeout(() => setVisualEffect(null), 2000);
                }
              }
              const [emoji, ...text] = (
                KOREAN_DYNAMIC_LABELS[label] || label
              ).split(" ");
              updateDynamicGesture({
                label: text.join(" "),
                emoji,
                statusIcon: <EyeIcon className="w-5 h-5" />,
              });
            });
          }
        } else {
          // clearCanvas(); // 필요 시 랜드마크 그리기 활성화 (현재 opacity: 0)
          sequenceRef.current = [];
          // [개선] 손이 감지되지 않을 때 상태를 '인식 중'으로 되돌림 (isStaticOn/isDynamicOn이 켜져 있을 경우)
          if (isStaticOn)
            updateStaticGesture({
              label: "인식 중",
              emoji: "👀",
              statusIcon: <EyeIcon className="w-5 h-5" />,
            });
          if (isDynamicOn)
            updateDynamicGesture({
              label: "움직여보세요",
              emoji: "🌊",
              statusIcon: <EyeIcon className="w-5 h-5" />,
            });
        }
      }
      animationFrameId.current = requestAnimationFrame(predict);
    };
    predict();
  }, [isStaticOn, isDynamicOn, updateStaticGesture, updateDynamicGesture]); // 의존성 추가

  // 랜드마크 그리기 함수 (현재 캔버스 opacity 0으로 비활성)
  const drawLandmarks = useCallback((landmarks: NormalizedLandmark[]) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    landmarks.forEach((landmark) => {
      const x = landmark.x * canvas.width;
      const y = landmark.y * canvas.height;
      ctx.beginPath();
      ctx.arc(x, y, 5, 0, 2 * Math.PI);
      ctx.fillStyle = "aqua";
      ctx.fill();
    });
  }, []);

  // 캔버스 클리어 함수
  const clearCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
  }, []);

  if (!mediaStream) return <div></div>;

  return (
    <div style={{ position: "relative", width: "100%", height: "100%" }}>
      <style>{`
        @keyframes pop-in { 0% { transform: scale(0.8); opacity: 0; } 100% { transform: scale(1); opacity: 1; } }
        .animate-pop-in { animation: pop-in 0.3s ease-out forwards; }
        @keyframes fade-in-out-corner { 0%, 100% { opacity: 0; transform: scale(0.5); } 10%, 90% { opacity: 1; transform: scale(1); } }
        .visual-effect-corner {
          position: absolute; bottom: 5%; right: 5%; font-size: 5rem;
          text-shadow: 0 0 15px rgba(0,0,0,0.6); z-index: 10;
          animation: fade-in-out-corner 2s ease-in-out forwards;
        }
      `}</style>
      <video
        ref={videoRef}
        autoPlay
        playsInline
        muted
        style={{
          width: "100%",
          height: "100%",
          objectFit: "cover",
          transform: "scaleX(-1)",
        }}
      />
      <canvas
        ref={canvasRef}
        width="640"
        height="360"
        style={{
          position: "absolute",
          top: 0,
          left: 0,
          width: "100%",
          height: "100%",
          transform: "scaleX(-1)",
          opacity: 0, // 캔버스 내용이 보이지 않도록 설정
        }}
      />

      {visualEffect && (
        <div className="visual-effect-corner">{visualEffect}</div>
      )}

      {/* 정적 제스처 결과 (좌상단) */}
      <GestureDisplayCard
        title="정적 제스처"
        state={staticGestureState}
        position="top-left"
      />

      {/* 동적 제스처 결과 (우상단) */}
      <GestureDisplayCard
        title="동적 제스처"
        state={dynamicGestureState}
        position="top-right"
      />

      {/* 우하단은 visualEffect가 사용하므로 비워둠 */}
    </div>
  );
};
