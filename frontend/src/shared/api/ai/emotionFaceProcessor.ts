import { loadFaceMesh, initializeFaceMesh } from "./faceMeshLoader";
import { withTimeout } from "./asyncResource";
// src/shared/api/ai/emotionFaceProcessor.ts

import * as tf from "@tensorflow/tfjs";
import { EmotionResult, AiSystemConfig } from "@/shared/types/ai.types";

// Dynamic import types
// type FaceMeshModule = typeof import("@mediapipe/face_mesh");
type FaceMeshInstance = import("@mediapipe/face_mesh").FaceMesh;
type Results = import("@mediapipe/face_mesh").Results;

// MediaPipe Face Mesh의 Results에서 랜드마크 타입 추출
type FaceMeshLandmark = Results["multiFaceLandmarks"][number][number];

// Face Mesh 랜드마크 인덱스 (app.py의 IDX_FM과 정확히 일치)
const IDX_FM = {
  upper_eye: 159,
  lower_eye: 145,
  eye_left: 33,
  eye_right: 133,
  upper_lip: 13,
  lower_lip: 14,
  mouth_left: 61,
  mouth_right: 291,
  mouth_center: 0,
  brow: 65,
  eye_center: 168,
  nose: 1,
  chin: 152,
  cheek_l: 205,
  cheek_r: 425,
};

// 표정 라벨 및 인덱스 (app.py와 정확히 일치)
const LABELS_FACE = ["laugh", "serious", "surprise", "yawn", "none"];
const IDX_FACE = {
  laugh: 0,
  serious: 1,
  surprise: 2,
  yawn: 3,
  none: 4,
};

// app.py의 메타데이터 임계값들 (정확히 일치)
const MOUTH_H_Q25 = 0.015;
const EYE_OPEN_Q25 = 0.012;
const BROW_LIFT_Q25 = 0.015;
const SMIRK_T = 0.010;
const FROWN_T = 0.018;
const PUFF_T = 0.015;

// 이벤트 전송 임계값 (app.py의 SHOW_THRESH 참고, 필요에 따라 조정)
const EMOTION_THRESHOLDS = {
  laugh: 0.6,   // 웃음 임계값을 0.85로 상승
  serious: 0.65, // 진지함 임계값을 0.85로 상승
  surprise: 0.95, // 놀람은 이미 높음
  yawn: 0.60,    // 하품 임계값을 0.80으로 상승
  none: 0.50,    // none은 유지
};

// 연속 감지 방지를 위한 쿨다운 (초)
const EMOTION_COOLDOWN = {
  laugh: 3.0,   // 웃음 쿨다운을 8초로 증가
  serious: 3.0, // 진지함 쿨다운을 8초로 증가
  surprise: 3.0, // 놀람 쿨다운을 8초로 증가
  yawn: 3.0,    // 하품 쿨다운을 8초로 증가
  none: 1.0,    // none은 유지
};

export class EmotionFaceProcessor {
  private faceMesh: FaceMeshInstance | null = null;
  private expressionModel: tf.LayersModel | null = null;
  private expressionScalerMean: number[] | null = null;
  private expressionScalerScale: number[] | null = null;
  private lastExpressionProbs: number[] | null = null; // for smoothing
  private readonly PROBA_ALPHA = 0.3; // app.py와 동일

  // MediaPipe onResults 콜백에서 결과를 Promise로 전달하기 위한 변수
  private _faceMeshResultResolver: ((value: Results) => void) | null = null;

  // 디버깅 및 이벤트 제어
  private lastEmotionEventTime: Record<string, number> = {};
  private debugMode = false;
  private emotionHistory: Array<{ emotion: string; confidence: number; timestamp: number }> = [];

  // 초기화 시 AI 설정 주입
  private aiConfig: AiSystemConfig;

  constructor(initialConfig: AiSystemConfig) {
    this.aiConfig = initialConfig;
    // 개발 환경에서 디버그 모드 활성화
    this.debugMode = typeof process !== "undefined" && process.env && process.env.NODE_ENV === "development";
  }

  /**
   * EmotionFaceProcessor를 초기화하고 모델을 로드합니다.
   */
  public async init(modelPath: string, scalerPath: string, wasmPath: string): Promise<void> {
    // Client-side check
    if (typeof window === "undefined") {
      console.warn("EmotionFaceProcessor: Cannot initialize on server side");
      this.aiConfig.emotion.enabled = false;
      return;
    }

    try {
      // TFJS 백엔드 고정 & 준비
      await tf.ready();

      // Dynamic import of MediaPipe Face Mesh
      const FaceMesh = await loadFaceMesh();

      // FaceMesh 인스턴스
      this.faceMesh = new FaceMesh({
        locateFile: (file: string) => {
          // 필요시 CDN으로 대체 가능:
          // return `https://cdn.jsdelivr.net/npm/@mediapipe/face_mesh/${file}`;
          return `${wasmPath}/${file}`;
        },
      });

      this.faceMesh.setOptions({
        maxNumFaces: 1,
        refineLandmarks: false, // app.py와 동일
        minDetectionConfidence: 0.7, // app.py와 동일
        minTrackingConfidence: 0.7,
      });

      this.faceMesh.onResults((results: Results) => {
        if (this._faceMeshResultResolver) {
          const resolve = this._faceMeshResultResolver;
          this._faceMeshResultResolver = null;
          resolve(results);
        }
      });
      await initializeFaceMesh(this.faceMesh);
      console.log("EmotionFaceProcessor: Face Mesh initialized.");

      // TensorFlow.js 모델 및 스케일러 로드
      this.expressionModel = await tf.loadLayersModel(modelPath);

      const scalerResponse = await fetch(scalerPath);
      const scalerData = await scalerResponse.json();
      this.expressionScalerMean = scalerData.mean_;
      this.expressionScalerScale = scalerData.scale_;
      console.log("EmotionFaceProcessor: Expression model and scaler loaded.");

      // 강 검증: 스케일러 길이/유효성
      if (!this.expressionScalerMean || !this.expressionScalerScale) {
        throw new Error("Scaler (mean_/scale_) not loaded");
      }
      if (this.expressionScalerMean.length !== 12 || this.expressionScalerScale.length !== 12) {
        throw new Error(
          `Scaler length mismatch. expected 12 but got mean=${this.expressionScalerMean.length}, scale=${this.expressionScalerScale.length}`
        );
      }

      // 디버그 로깅
      if (this.debugMode) {
        console.log("Scaler mean length:", this.expressionScalerMean.length);
        console.log("Scaler scale length:", this.expressionScalerScale.length);
        console.log("First few mean values:", this.expressionScalerMean.slice(0, 5));
        console.log("First few scale values:", this.expressionScalerScale.slice(0, 5));
      }
    } catch (error) {
      console.error("EmotionFaceProcessor: Failed to load Face Mesh or models:", error);
      this.cleanup();
      throw error;
    }
  }

  /**
   * AI 설정을 업데이트합니다.
   */
  public updateConfig(config: Partial<AiSystemConfig>): void {
    this.aiConfig = {
      ...this.aiConfig,
      ...config,
      emotion: { ...this.aiConfig.emotion, ...(config as any).emotion },
    };
  }

  /**
   * 디버그 모드 토글
   */
  public setDebugMode(enabled: boolean): void {
    this.debugMode = enabled;
    console.log(`EmotionFaceProcessor debug mode: ${enabled ? "ON" : "OFF"}`);
  }

  /**
   * 감정 히스토리 조회 (디버깅용)
   */
  public getEmotionHistory(): Array<{ emotion: string; confidence: number; timestamp: number }> {
    const n = this.emotionHistory.length;
    return this.emotionHistory.slice(n > 50 ? n - 50 : 0);
  }

  /**
   * 입력 프레임에서 얼굴 표정을 감지합니다.
   */
  public async detectEmotion(inputImageData: ImageData, timestamp: number): Promise<EmotionResult | null> {
    if (typeof window === "undefined") {
      return null;
    }

    if (
      !this.aiConfig.emotion.enabled ||
      this.faceMesh === null ||
      this.expressionModel === null ||
      this.expressionScalerMean === null ||
      this.expressionScalerScale === null
    ) {
      return null;
    }

    // 오프스크린 캔버스에 프레임 올리기
    const canvas = document.createElement("canvas");
    canvas.width = inputImageData.width;
    canvas.height = inputImageData.height;
    const ctx = canvas.getContext("2d");
    if (!ctx) return null;
    ctx.putImageData(inputImageData, 0, 0);

    // MediaPipe 결과를 Promise로 기다림
    const resultsPromise = new Promise<Results>((resolve) => {
      this._faceMeshResultResolver = resolve;
    });

    const faceMesh = this.faceMesh;
    const results = await withTimeout((async () => {
      await faceMesh.send({ image: canvas });
      return resultsPromise;
    })(), 1500, "표정 인식 응답 시간이 초과됐습니다.").finally(() => {
      this._faceMeshResultResolver = null;
    });

    if (results.multiFaceLandmarks && results.multiFaceLandmarks.length > 0) {
      const faceLandmarks = results.multiFaceLandmarks[0];

      // 특징 추출
      const extractedFeatures = this.extractFeatures12(faceLandmarks);
      if (!extractedFeatures) {
        return null;
      }

      // 스케일링
      const scaledFeatures = this.scaleFeatures(extractedFeatures);

      // 예측 (tidy로 메모리 관리)
      const smoothedProbs = tf.tidy(() => {
        const inputTensor = tf.tensor2d([scaledFeatures], [1, scaledFeatures.length], "float32");
        const out = this.expressionModel as tf.LayersModel;
        const pred = out.predict(inputTensor) as tf.Tensor;
        const probsArr = Array.from(pred.dataSync() as Float32Array);
        return this.smoothProbs(probsArr);
      });

      // 후처리
      let topIdx = 0;
      let maxP = -1;
      for (let i = 0; i < smoothedProbs.length; i++) {
        if (smoothedProbs[i] > maxP) {
          maxP = smoothedProbs[i];
          topIdx = i;
        }
      }

      topIdx = this.gateSurprise(extractedFeatures, smoothedProbs, topIdx);
      const promoted = this.promoteSerious(extractedFeatures, smoothedProbs, topIdx);
      const finalIdx = promoted[0];
      const finalConf = promoted[1];

      const label = LABELS_FACE[finalIdx];
      const confidence = finalConf;

      // 가끔 확률 로그
      // if (this.debugMode && Math.random() < 0.05) {
      //   const top3 = smoothedProbs
      //     .map((p, i) => ({ label: LABELS_FACE[i], prob: p }))
      //     .sort((a, b) => b.prob - a.prob)
      //     .slice(0, 3);
      //   console.log("Top 3 emotions:", top3);
      //   console.log("Final result:", { label, confidence: confidence.toFixed(3) });
      // }

      // 히스토리 기록
      this.emotionHistory.push({ emotion: label, confidence, timestamp });
      if (this.emotionHistory.length > 100) {
        this.emotionHistory.shift();
      }

      // 임계/쿨다운 체크
      const threshold =
        (EMOTION_THRESHOLDS as any)[label] !== undefined ? (EMOTION_THRESHOLDS as any)[label] : 0.8;
      const cooldown =
        (EMOTION_COOLDOWN as any)[label] !== undefined ? (EMOTION_COOLDOWN as any)[label] : 3.0;

      const now = Date.now() / 1000;
      const lastEventTime = this.lastEmotionEventTime[label] || 0;

      if (confidence < threshold || now - lastEventTime < cooldown) {
        // if (this.debugMode && confidence >= threshold) {
        //   const elapsed = (now - lastEventTime).toFixed(1);
        //   console.log(`${label} detected but in cooldown (${elapsed}s / ${cooldown}s)`);
        // }
        return null;
      }

      // 쿨다운 갱신 후 결과 반환
      this.lastEmotionEventTime[label] = now;

      return {
        type: "emotion",
        label: label,
        confidence: confidence,
        faceLandmarks: faceLandmarks.map((lm) => [lm.x, lm.y, lm.z]),
        timestamp: timestamp,
      };
    }

    return null;
  }

  /**
   * app.py의 extract_features12와 정확히 동일한 로직
   */
  private extractFeatures12(landmarks: FaceMeshLandmark[]): number[] | null {
    try {
      const arr: number[][] = landmarks.map((lm) => [lm.x, lm.y, lm.z ? lm.z : 0]);

      const euclideanDist = (p1: number[], p2: number[]): number => {
        const dx = p1[0] - p2[0];
        const dy = p1[1] - p2[1];
        const dz = p1[2] - p2[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
      };

      const eyeOpen = euclideanDist(arr[IDX_FM.upper_eye], arr[IDX_FM.lower_eye]);
      const mouthH = euclideanDist(arr[IDX_FM.upper_lip], arr[IDX_FM.lower_lip]);
      const mouthW = euclideanDist(arr[IDX_FM.mouth_left], arr[IDX_FM.mouth_right]);
      const browLift = euclideanDist(arr[IDX_FM.brow], arr[IDX_FM.eye_center]);
      const eyeW = euclideanDist(arr[IDX_FM.eye_left], arr[IDX_FM.eye_right]);

      const eyeRatio = eyeOpen > 0 ? eyeW / eyeOpen : 0.0;
      const mouthRatio = mouthH > 0 ? mouthW / mouthH : 0.0;
      const browMouthRatio = mouthH > 0 ? browLift / mouthH : 0.0;

      const mouthCenterY = arr[IDX_FM.mouth_center][1];
      const leftY = arr[IDX_FM.mouth_left][1];
      const rightY = arr[IDX_FM.mouth_right][1];
      const mouthDroop = (leftY + rightY) / 2.0 - mouthCenterY;
      const mouthAsym = Math.abs(leftY - rightY);

      const cheekAvgZ = (arr[IDX_FM.cheek_l][2] + arr[IDX_FM.cheek_r][2]) / 2.0;
      const noseZ = arr[IDX_FM.nose][2];
      const cheekPuff = noseZ - cheekAvgZ;

      const faceScale = euclideanDist(arr[IDX_FM.nose], arr[IDX_FM.chin]);
      if (faceScale === 0 || !isFinite(faceScale)) {
        return [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
      }

      const dx = arr[IDX_FM.mouth_right][0] - arr[IDX_FM.mouth_left][0];
      const dy = arr[IDX_FM.mouth_right][1] - arr[IDX_FM.mouth_left][1];
      const mouthTilt = Math.atan2(dy, dx) / (Math.PI / 2);
      const eyeMouthCoupling = (eyeOpen / faceScale) / ((mouthH / faceScale) + 1e-6);

      const feats = [
        eyeOpen / faceScale, // 0
        mouthH / faceScale, // 1
        mouthW / faceScale, // 2
        browLift / faceScale, // 3
        eyeRatio, // 4
        mouthRatio, // 5
        browMouthRatio, // 6
        mouthDroop / faceScale, // 7
        mouthAsym / faceScale, // 8
        cheekPuff / faceScale, // 9
        mouthTilt, // 10
        eyeMouthCoupling, // 11
      ];

      const safe: number[] = new Array(feats.length);
      for (let i = 0; i < feats.length; i++) {
        const v = feats[i];
        safe[i] = isNaN(v) || !isFinite(v) ? 0 : v;
      }
      return safe;
    } catch (e) {
      // console.warn("Error extracting facial features:", e);
      return null;
    }
  }

  /**
   * app.py와 정확히 동일한 스케일링 + NaN 안전장치
   */
  private scaleFeatures(features: number[]): number[] {
    if (this.expressionScalerMean === null || this.expressionScalerScale === null) {
      // console.warn("Scaler data not loaded for scaling.");
      return features;
    }
    const mean = this.expressionScalerMean;
    const scale = this.expressionScalerScale;

    const out = new Array(features.length);
    for (let i = 0; i < features.length; i++) {
      const s = scale[i] === 0 ? 1e-8 : scale[i];
      const v = (features[i] - mean[i]) / (s + 1e-8);
      out[i] = Number.isFinite(v) ? v : 0;
    }
    return out;
  }

  /**
   * app.py와 동일한 EMA 스무딩
   */
  private smoothProbs(currentProbs: number[]): number[] {
    if (this.lastExpressionProbs === null) {
      this.lastExpressionProbs = currentProbs.slice();
    } else {
      const prev = this.lastExpressionProbs;
      const out = new Array(currentProbs.length);
      const alpha = this.PROBA_ALPHA;
      for (let i = 0; i < currentProbs.length; i++) {
        out[i] = (1 - alpha) * prev[i] + alpha * currentProbs[i];
      }
      this.lastExpressionProbs = out;
    }
    return this.lastExpressionProbs.slice();
  }

  /**
   * gate_surprise (입이 충분히 벌어졌을 때만 surprise 허용)
   */
  private gateSurprise(rawFeat: number[], probs: number[], topIdx: number): number {
    if (topIdx !== IDX_FACE.surprise) {
      return topIdx;
    }

    // surprise는 입이 충분히 벌어져야 함 (임계값을 더욱 높여서 엄격하게)
    const SURPRISE_MOUTH_THRESHOLD = 0.045; // 입을 훨씬 더 크게 벌려야 함
    const SURPRISE_EYE_THRESHOLD = 0.028;   // 눈도 훨씬 더 크게 떠야 함
    
    // 입이 충분히 벌어지지 않았거나 눈이 충분히 크게 뜨지 않았으면 surprise 거부
    if (rawFeat[1] < SURPRISE_MOUTH_THRESHOLD || rawFeat[0] < SURPRISE_EYE_THRESHOLD) {
      // 두 번째로 높은 확률의 감정으로 대체
      let best = -Infinity,
        bestIdx = -1;
      let second = -Infinity,
        secondIdx = -1;

      for (let i = 0; i < probs.length; i++) {
        const p = probs[i];
        if (p > best) {
          second = best;
          secondIdx = bestIdx;
          best = p;
          bestIdx = i;
        } else if (p > second) {
          second = p;
          secondIdx = i;
        }
      }
      return secondIdx >= 0 ? secondIdx : topIdx;
    }
    return topIdx;
  }

  /**
   * promote_serious (app.py와 동일)
   */
  private promoteSerious(rawFeat: number[], probs: number[], currentIdx: number): [number, number] {
    const seriousIdx = IDX_FACE.serious;
    const seriousP = probs[seriousIdx];

    const smirk = rawFeat[8] > SMIRK_T; // mouthAsym
    const frown = rawFeat[3] < FROWN_T; // browLift
    const puff = rawFeat[9] > PUFF_T; // cheekPuff

    if (seriousP >= 0.60 || ((smirk || frown || puff) && seriousP >= 0.45)) {
      return [seriousIdx, seriousP];
    }
    return [currentIdx, probs[currentIdx]];
  }

  /**
   * 임계값 동적 조정 (필요시 사용)
   */
  public updateEmotionThreshold(emotion: string, threshold: number): void {
    if ((EMOTION_THRESHOLDS as any)[emotion] !== undefined) {
      (EMOTION_THRESHOLDS as any)[emotion] = Math.max(0.1, Math.min(0.99, threshold));
      console.log(`Updated ${emotion} threshold to ${threshold}`);
    }
  }

  /**
   * 쿨다운 초기화 (테스트용)
   */
  public resetCooldowns(): void {
    this.lastEmotionEventTime = {};
    console.log("Emotion cooldowns reset");
  }

  /**
   * 리소스를 정리합니다.
   */
  public cleanup(): void {
    if (this.faceMesh) {
      const faceMesh = this.faceMesh;
      this.faceMesh = null;
      void faceMesh.close().catch(error => console.warn("FaceMesh close failed", error));
    }
    this._faceMeshResultResolver = null;
    if (this.expressionModel) {
      this.expressionModel.dispose();
      this.expressionModel = null;
    }
    this.expressionScalerMean = null;
    this.expressionScalerScale = null;
    this.lastExpressionProbs = null;
    this.lastEmotionEventTime = {};
    this.emotionHistory = [];
  }
}
