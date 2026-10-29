// src/shared/api/ai/gestureProcessor.ts

import {
  HandLandmarker,
  FilesetResolver,
  GestureRecognizer,
  NormalizedLandmark,
} from "@mediapipe/tasks-vision";
import { GestureResult, AiSystemConfig } from "@/shared/types/ai.types";
import * as tf from "@tensorflow/tfjs";

export class GestureProcessor {
  private handLandmarker: HandLandmarker | null = null;
  private gestureRecognizer: GestureRecognizer | null = null;
  private aiConfig: AiSystemConfig;
  
  // TensorFlow.js 모델들
  private staticGestureModel: tf.LayersModel | null = null;
  private dynamicGestureModel: tf.LayersModel | null = null;
  
  // 모델 경로 상수
  private readonly STATIC_GESTURE_MODEL_PATH = "/models/static-gesture/model.json";
  private readonly DYNAMIC_GESTURE_MODEL_PATH = "/models/dinamic-gesture/model.json"; // 실제 폴더명에 맞춰 수정

  // 참고 코드의 안정화 상수들 추가
  private readonly PX_HIGH = 0.10; // 움직임 임계값 (약간만 낮춤)
  private readonly STATIC_CONF_T = 0.75; // 정적 제스처 신뢰도 임계값
  private readonly STATIC_VOTE_K = 10; // 다수결 투표 수 (더 많은 투표 요구)
  private readonly STATIC_HOLD_SEC = 2.0; // 정적 제스처 유지 시간 (2초)
  private readonly STATIC_COOLDOWN = 5.0; // 정적 제스처 쿨다운(초) - 5초
  private readonly SEQ_LEN = 30; // 동적 제스처 시퀀스 길이 (원래대로 복원)
  private readonly DYN_CONF_T = 0.85; // 동적 제스처 신뢰도 임계값 (약간만 낮춤)
  private readonly MOVE3D_T = 0.05; // 3D 움직임 임계값 (약간만 낮춤)
  private readonly DYN_COOLDOWN = 5.0; // 동적 제스처 쿨다운(초) - 5초

  // 손별 상태 관리
  private handStates: Map<string, {
    staticVote: string[]; // 정적 제스처 투표 버퍼
    lastStaticLabel: string;
    staticLabelStart: number;
    lastStaticTime: Map<string, number>; // 정적 제스처별 마지막 감지 시간
    moveHist2d: number[]; // 2D 움직임 히스토리
    prevWrist2: [number, number] | null;
    prevWrist3: [number, number, number] | null;
    dynamicSequence: number[][]; // 동적 제스처 시퀀스
    lastDynTime: Map<string, number>; // 동적 제스처별 마지막 감지 시간
    wristNorm: [number, number] | null;
    indexNorm: [number, number] | null;
  }> = new Map();

  private noHandCount = 0;
  private readonly NOHAND_CLEAR_FR = 10;

  constructor(initialConfig: AiSystemConfig) {
    this.aiConfig = initialConfig;
  }

  /**
   * GestureProcessor를 초기화하고 MediaPipe 손 인식 모델 및 TensorFlow.js 모델들을 로드합니다.
   * @param wasmPath MediaPipe Tasks Vision WASM 경로 (예: "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm")
   */
  public async init(wasmPath: string): Promise<void> {
    // Client-side check
    if (typeof window === 'undefined') {
      console.warn("GestureProcessor: Cannot initialize on server side");
      this.aiConfig.gesture.static.enabled = false;
      this.aiConfig.gesture.dynamic.enabled = false;
      return;
    }

    console.log("🤖 Initializing GestureProcessor models...");

    try {
      const vision = await FilesetResolver.forVisionTasks(wasmPath);

      // MediaPipe 모델들과 TensorFlow.js 모델들을 병렬로 로드
      const results = await Promise.allSettled([
        this.initMediaPipeModels(vision), this.initTensorFlowModels(),
      ]);
      const failed = results.find((result): result is PromiseRejectedResult => result.status === "rejected");
      if (failed) throw failed.reason;

      console.log("✅ GestureProcessor: All models initialized successfully.");
    } catch (error) {
      this.cleanup();
      throw error;
    }
  }

  /**
   * MediaPipe 모델들을 초기화합니다.
   */
  private async initMediaPipeModels(vision: any): Promise<void> {
    // HandLandmarker 생성
    try {
      this.handLandmarker = await HandLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
          delegate: "GPU", // 기본적으로 GPU 시도
        },
        runningMode: "VIDEO",
        numHands: 2,
        minHandDetectionConfidence: 0.7,
        minHandPresenceConfidence: 0.7,
        minTrackingConfidence: 0.7,
      });
      console.log("GestureProcessor: HandLandmarker created successfully with GPU delegate.");
    } catch (gpuError) {
      console.warn(
        "GestureProcessor: GPU delegate failed for HandLandmarker, trying CPU:",
        gpuError
      );
      this.handLandmarker = await HandLandmarker.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task",
          delegate: "CPU", // GPU 실패 시 CPU 폴백
        },
        runningMode: "VIDEO",
        numHands: 2,
        minHandDetectionConfidence: 0.7,
        minHandPresenceConfidence: 0.7,
        minTrackingConfidence: 0.7,
      });
      console.log("GestureProcessor: HandLandmarker created successfully with CPU delegate.");
    }

    // GestureRecognizer 생성 (동적 제스처용으로 유지)
    try {
      this.gestureRecognizer = await GestureRecognizer.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            "https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task",
          delegate: "GPU", // 기본적으로 GPU 시도
        },
        runningMode: "VIDEO",
        numHands: 2,
        minHandDetectionConfidence: 0.7,
        minHandPresenceConfidence: 0.7,
        minTrackingConfidence: 0.7,
      });
      console.log("GestureProcessor: GestureRecognizer created successfully with GPU delegate.");
    } catch (gpuError) {
      console.warn(
        "GestureProcessor: GPU delegate failed for GestureRecognizer, trying CPU:",
        gpuError
      );
      this.gestureRecognizer = await GestureRecognizer.createFromOptions(vision, {
        baseOptions: {
          modelAssetPath:
            "https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/1/gesture_recognizer.task",
          delegate: "CPU", // GPU 실패 시 CPU 폴백
        },
        runningMode: "VIDEO",
        numHands: 2,
        minHandDetectionConfidence: 0.7,
        minHandPresenceConfidence: 0.7,
        minTrackingConfidence: 0.7,
      });
      console.log("GestureProcessor: GestureRecognizer created successfully with CPU delegate.");
    }
  }

  /**
   * TensorFlow.js 모델들을 초기화합니다.
   */
  private async initTensorFlowModels(): Promise<void> {
    try {
      // TensorFlow.js 백엔드 설정
      await tf.ready();
      console.log(`TensorFlow.js backend: ${tf.getBackend()}`);

      // 정적 제스처 모델 로드
      if (this.aiConfig.gesture.static.enabled) {
        try {
          console.log(`Loading static gesture model from: ${this.STATIC_GESTURE_MODEL_PATH}`);
          this.staticGestureModel = await tf.loadLayersModel(this.STATIC_GESTURE_MODEL_PATH);
          console.log("✅ Static gesture model loaded successfully");
        } catch (error) {
          console.warn("❌ Failed to load static gesture model:", error);
          throw error;
        }
      }

      // 동적 제스처 모델 로드
      if (this.aiConfig.gesture.dynamic.enabled) {
        try {
          console.log(`Loading dynamic gesture model from: ${this.DYNAMIC_GESTURE_MODEL_PATH}`);
          this.dynamicGestureModel = await tf.loadLayersModel(this.DYNAMIC_GESTURE_MODEL_PATH);
          console.log("✅ Dynamic gesture model loaded successfully");
        } catch (error) {
          console.warn("❌ Failed to load dynamic gesture model:", error);
          throw error;
        }
      }
    } catch (error) {
      console.error("❌ TensorFlow.js initialization failed:", error);
      throw error;
    }
  }

  /**
   * AI 설정을 업데이트합니다.
   * @param config 업데이트할 AI 설정
   */
  public updateConfig(config: Partial<AiSystemConfig>): void {
    this.aiConfig = {
      ...this.aiConfig,
      ...config,
      gesture: { ...this.aiConfig.gesture, ...config.gesture },
    };
  }

  /**
   * 손 랜드마크를 손목 기준 상대 좌표로 변환
   * 참고 코드의 hand_keypoints_relative 함수 포팅
   */
  private handKeypointsRelative(landmarks: NormalizedLandmark[]): number[] {
    const wrist = landmarks[0];
    const relative: number[] = [];
    
    for (const lm of landmarks) {
      relative.push(
        lm.x - wrist.x,
        lm.y - wrist.y,
        lm.z - wrist.z
      );
    }
    
    return relative;
  }

  /**
   * 손 상태 초기화
   */
  private initHandState(handKey: string) {
    this.handStates.set(handKey, {
      staticVote: [],
      lastStaticLabel: "none",
      staticLabelStart: 0,
      lastStaticTime: new Map(),
      moveHist2d: [],
      prevWrist2: null,
      prevWrist3: null,
      dynamicSequence: [],
      lastDynTime: new Map(),
      wristNorm: null,
      indexNorm: null,
    });
  }

  /**
   * 정적 제스처 감지 (TensorFlow.js 모델 사용)
   * @param landmarks 손 랜드마크 (21개 포인트)
   * @returns 정적 제스처 결과 또는 null
   */
  private async detectStaticGesture(landmarks: NormalizedLandmark[]): Promise<{ label: string; confidence: number } | null> {
    if (!this.staticGestureModel || !this.aiConfig.gesture.static.enabled) {
      return null;
    }

    try {
      // 손목 기준 상대 좌표로 변환
      const relativeKeypoints = this.handKeypointsRelative(landmarks);
      
      // TensorFlow.js 모델에 맞는 형태로 데이터 준비 (1차원 배열을 2차원 텐서로)
      const inputTensor = tf.tensor2d([relativeKeypoints], [1, relativeKeypoints.length]);
      
      // 모델 추론
      const prediction = this.staticGestureModel.predict(inputTensor) as tf.Tensor;
      const predictionData = await prediction.data();
      
      // 메모리 정리
      inputTensor.dispose();
      prediction.dispose();
      
      // 가장 높은 확률의 클래스 찾기
      const maxProbability = Math.max(...Array.from(predictionData));
      const predictedClassIndex = Array.from(predictionData).indexOf(maxProbability);
      
      // 정적 제스처 레이블 매핑 (11개 클래스)
      const staticGestureLabels = [
        "bad", "fist", "good", "gun", "heart", "none", 
        "ok", "open_palm", "promise", "rock", "victory"
      ];
      
      const predictedLabel = staticGestureLabels[predictedClassIndex] || "none";
      
      // 디버깅 로그 (나중에 제거 가능)
      // if (maxProbability > 0.5) {
      //   console.log(`Static gesture detected: ${predictedLabel} (${(maxProbability * 100).toFixed(1)}%)`);
      // }
      
      return {
        label: predictedLabel,
        confidence: maxProbability
      };
    } catch (error) {
      console.error("Static gesture detection error:", error);
      return null;
    }
  }

  /**
   * 동적 제스처 감지 (TensorFlow.js 모델 사용)
   * @param sequence 움직임 시퀀스 (SEQ_LEN x 63 차원)
   * @returns 동적 제스처 결과 또는 null
   */
  private async detectDynamicGesture(sequence: number[][]): Promise<{ label: string; confidence: number } | null> {
    if (!this.dynamicGestureModel || !this.aiConfig.gesture.dynamic.enabled || sequence.length !== this.SEQ_LEN) {
      return null;
    }

    try {
      // 시퀀스 데이터를 3차원 텐서로 변환 [1, SEQ_LEN, 63]
      const sequenceArray = sequence.map(frame => frame); // 2D 배열
      const inputTensor = tf.tensor3d([sequenceArray], [1, this.SEQ_LEN, sequenceArray[0].length]);
      
      // 모델 추론
      const prediction = this.dynamicGestureModel.predict(inputTensor) as tf.Tensor;
      const predictionData = await prediction.data();
      
      // 메모리 정리
      inputTensor.dispose();
      prediction.dispose();
      
      // 가장 높은 확률의 클래스 찾기
      const maxProbability = Math.max(...Array.from(predictionData));
      const predictedClassIndex = Array.from(predictionData).indexOf(maxProbability);
      
      // 동적 제스처 레이블 매핑 (7개 클래스)
      const dynamicGestureLabels = [
        "fire", "hi", "hit", "none", "nono", "nyan", "shot"
      ];
      
      const predictedLabel = dynamicGestureLabels[predictedClassIndex] || "none";
      
      // 디버깅 로그 (나중에 제거 가능)
      // if (maxProbability > 0.5) {
      //   console.log(`Dynamic gesture detected: ${predictedLabel} (${(maxProbability * 100).toFixed(1)}%)`);
      // }
      
      return {
        label: predictedLabel,
        confidence: maxProbability
      };
    } catch (error) {
      console.error("Dynamic gesture detection error:", error);
      return null;
    }
  }

  /**
   * 입력 비디오 요소에서 손 랜드마크 및 제스처를 감지합니다.
   * @param videoElement 비디오 요소 (HTMLVideoElement) 또는 Canvas (HTMLCanvasElement)
   * @param timestamp 현재 타임스탬프 (ms)
   * @returns 감지된 제스처 결과 (GestureResult) 또는 null
   */
  public async detectGestures(
    videoElement: HTMLVideoElement | HTMLCanvasElement,
    timestamp: number
  ): Promise<GestureResult | null> {
    // Client-side check
    if (typeof window === 'undefined') {
      return null;
    }

    // Check if we have the required components for the enabled features
    const needsHandLandmarker = this.aiConfig.gesture.static.enabled || this.aiConfig.gesture.dynamic.enabled;
    
    if (needsHandLandmarker && !this.handLandmarker) {
      return null;
    }

    // 제스처 인식 처리
    if (this.aiConfig.gesture.static.enabled || this.aiConfig.gesture.dynamic.enabled) {
      try {
        const handResults = this.handLandmarker!.detectForVideo(videoElement, timestamp);

        if (handResults && handResults.landmarks && handResults.landmarks.length > 0) {
          // landmarks 변환: NormalizedLandmark[][] -> number[][]
          const processedLandmarks: number[][] = handResults.landmarks.flatMap(
            (hand: NormalizedLandmark[]) => hand.map((lm: NormalizedLandmark) => [lm.x, lm.y, lm.z])
          );

          const result: GestureResult = {
            type: "gesture",
            static: { label: "none", confidence: 0 },
            dynamic: { label: "none", confidence: 0 },
            landmarks: processedLandmarks,
            timestamp: timestamp,
          };

          const currentTime = timestamp / 1000; // 초 단위 변환

          // 각 손에 대해 처리
          for (let handIndex = 0; handIndex < handResults.landmarks.length; handIndex++) {
            const handLandmarks = handResults.landmarks[handIndex];
            const handKey = `hand_${handIndex}`;

            // 손 상태 초기화 (필요시)
            if (!this.handStates.has(handKey)) {
              this.initHandState(handKey);
            }

            const handState = this.handStates.get(handKey)!;

            // 손목 좌표 추출 (랜드마크 0번)
            const wrist = handLandmarks[0];
            const currentWrist2: [number, number] = [wrist.x, wrist.y];
            const currentWrist3: [number, number, number] = [wrist.x, wrist.y, wrist.z];

            // 움직임 감지
            let isMoving = false;
            if (handState.prevWrist2 && handState.prevWrist3) {
              const move2d = Math.sqrt(
                Math.pow(currentWrist2[0] - handState.prevWrist2[0], 2) +
                Math.pow(currentWrist2[1] - handState.prevWrist2[1], 2)
              );
              const move3d = Math.sqrt(
                Math.pow(currentWrist3[0] - handState.prevWrist3[0], 2) +
                Math.pow(currentWrist3[1] - handState.prevWrist3[1], 2) +
                Math.pow(currentWrist3[2] - handState.prevWrist3[2], 2)
              );

              handState.moveHist2d.push(move2d);
              if (handState.moveHist2d.length > 8) {
                handState.moveHist2d.shift();
              }

              const avgMove2d = handState.moveHist2d.reduce((a, b) => a + b, 0) / handState.moveHist2d.length;
              // 동적 제스처를 위한 균형잡힌 움직임 감지
              const recentMoves = handState.moveHist2d.slice(-2); // 최근 2프레임
              const hasMovement = recentMoves.some(m => m > this.PX_HIGH * 0.4); // 적당한 임계값
              isMoving = (avgMove2d > this.PX_HIGH * 0.6) || move3d > this.MOVE3D_T * 0.6 || hasMovement;
            }

            // 정적 제스처 처리
            if (this.aiConfig.gesture.static.enabled && !isMoving) {
              const staticResult = await this.detectStaticGesture(handLandmarks);
              if (staticResult && staticResult.confidence > this.STATIC_CONF_T) {
                // 투표 시스템
                handState.staticVote.push(staticResult.label);
                if (handState.staticVote.length > this.STATIC_VOTE_K) {
                  handState.staticVote.shift();
                }

                // 다수결 확인
                const voteCounts: { [key: string]: number } = {};
                handState.staticVote.forEach(vote => {
                  voteCounts[vote] = (voteCounts[vote] || 0) + 1;
                });

                const maxVotes = Math.max(...Object.values(voteCounts));
                const majorityLabel = Object.keys(voteCounts).find(key => voteCounts[key] === maxVotes);

                if (majorityLabel && majorityLabel !== "none") {
                  if (handState.lastStaticLabel !== majorityLabel) {
                    handState.lastStaticLabel = majorityLabel;
                    handState.staticLabelStart = currentTime;
                  } else if (currentTime - handState.staticLabelStart >= this.STATIC_HOLD_SEC) {
                    // 쿨다운 체크
                    const lastTime = handState.lastStaticTime.get(majorityLabel) || 0;
                    if (currentTime - lastTime >= this.STATIC_COOLDOWN) {
                      result.static = {
                        label: majorityLabel,
                        confidence: staticResult.confidence
                      };
                      handState.lastStaticTime.set(majorityLabel, currentTime);
                    }
                  }
                }
              }
            }

            // 동적 제스처 처리 - 실제 움직임이 있을 때만 인식
            if (this.aiConfig.gesture.dynamic.enabled && isMoving) {
              // 손목 기준 상대 좌표 계산
              const relativeKeypoints = this.handKeypointsRelative(handLandmarks);
              handState.dynamicSequence.push(relativeKeypoints);

              if (handState.dynamicSequence.length > this.SEQ_LEN) {
                handState.dynamicSequence.shift();
              }

              // 시퀀스가 충분히 쌓였을 때 동적 제스처 분류
              if (handState.dynamicSequence.length === this.SEQ_LEN) {
                const dynamicResult = await this.detectDynamicGesture(handState.dynamicSequence);
                if (dynamicResult) {
                  // shot 제스처는 특별히 높은 임계값 적용
                  const confidenceThreshold = dynamicResult.label === 'shot' ? 0.98 : this.DYN_CONF_T;
                  
                  if (dynamicResult.confidence > confidenceThreshold) {
                    // 쿨다운 체크
                    const lastTime = handState.lastDynTime.get(dynamicResult.label) || 0;
                    if (currentTime - lastTime >= this.DYN_COOLDOWN) {
                      result.dynamic = {
                        label: dynamicResult.label,
                        confidence: dynamicResult.confidence
                      };
                      handState.lastDynTime.set(dynamicResult.label, currentTime);
                      // console.log(`🎯 동적 제스처 인식: ${dynamicResult.label} (${(dynamicResult.confidence * 100).toFixed(1)}%)`);
                    }
                  }
                }
              }
            }

            // 이전 프레임 정보 업데이트
            handState.prevWrist2 = currentWrist2;
            handState.prevWrist3 = currentWrist3;
          }

          // 손이 감지되었으므로 noHandCount 리셋
          this.noHandCount = 0;

          return result;
        } else {
          // 손이 감지되지 않은 경우
          this.noHandCount++;
          if (this.noHandCount >= this.NOHAND_CLEAR_FR) {
            // 모든 손 상태 초기화
            this.handStates.clear();
            this.noHandCount = 0;
          }
        }
      } catch (error) {
        // console.debug("GestureProcessor: Hand detection error:", error);
      }
    }
    return null;
  }

  /**
   * 리소스를 정리합니다.
   */
  public cleanup(): void {
    // Client-side check
    if (typeof window === 'undefined') {
      return;
    }

    // MediaPipe 모델들 정리
    if (this.handLandmarker) {
      try {
        this.handLandmarker.close();
      } catch (error) {
        console.warn("Error closing handLandmarker:", error);
      }
      this.handLandmarker = null;
    }
    if (this.gestureRecognizer) {
      try {
        this.gestureRecognizer.close();
      } catch (error) {
        console.warn("Error closing gestureRecognizer:", error);
      }
      this.gestureRecognizer = null;
    }

    // TensorFlow.js 모델들 정리
    if (this.staticGestureModel) {
      try {
        this.staticGestureModel.dispose();
      } catch (error) {
        console.warn("Error disposing static gesture model:", error);
      }
      this.staticGestureModel = null;
    }
    if (this.dynamicGestureModel) {
      try {
        this.dynamicGestureModel.dispose();
      } catch (error) {
        console.warn("Error disposing dynamic gesture model:", error);
      }
      this.dynamicGestureModel = null;
    }

    // 손 상태 정리
    this.handStates.clear();
    this.noHandCount = 0;

    console.log("GestureProcessor cleaned up.");
  }
}
