// src/shared/api/ai/frontendAiProcessor.ts

import { AppDispatch } from "@/shared/config/store";
import {
  GestureResult,
  EmotionResult,
  AiSystemConfig,
  GestureCallback,
  EmotionCallback,
} from "@/shared/types/ai.types";

import { emotionCaptureManager } from "./emotionCaptureManager";
import { EmotionFaceProcessor } from "./emotionFaceProcessor";
import { BeautyFilterProcessor } from "./beautyFilterProcessor";
import { GestureProcessor } from "./gestureProcessor";
import * as tf from "@tensorflow/tfjs";
import { abortable, abortError, withTimeout } from "./asyncResource";
import { FACE_MESH_ASSETS } from "./faceMeshLoader";
import { setAiRuntime } from "@/entities/video-conference/ai/model/aiSlice";

// --- AI 모델 경로 상수 정의 ---
const MODELS_BASE_PATH = "/models";

// 표정 인식 모델 경로
const EXPRESSION_MODEL_PATH = `${MODELS_BASE_PATH}/expression/model.json`;
const EXPRESSION_SCALER_PATH = `${MODELS_BASE_PATH}/expression/scaler_v3.json`;

// 제스처 인식 모델 경로
const STATIC_GESTURE_MODEL_PATH = `${MODELS_BASE_PATH}/static-gesture/model.json`;
const DYNAMIC_GESTURE_MODEL_PATH = `${MODELS_BASE_PATH}/dinamic-gesture/model.json`;

// MediaPipe WASM 파일 CDN 경로
const FACE_MESH_WASM_PATH = FACE_MESH_ASSETS;
const TASKS_VISION_WASM_PATH = "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.22-rc.20250304/wasm";

// ✨ 이미지 오버레이를 위한 경로 상수 추가
const STATIC_IMAGE_BASE_PATH = "/images/gestures/static";
const DYNAMIC_IMAGE_BASE_PATH = "/images/gestures/dynamic";
const EMOTION_IMAGE_BASE_PATH = "/images/gestures/emotion";

interface AiRuntime {
  key: string;
  config: AiSystemConfig;
  gesture: GestureProcessor | null;
  emotion: EmotionFaceProcessor | null;
  beauty: BeautyFilterProcessor | null;
  images: Map<string, HTMLImageElement>;
}

interface VideoSession {
  source: MediaStreamTrack;
  output: MediaStreamTrack | null;
  video: HTMLVideoElement;
  canvas: HTMLCanvasElement;
  frame: number | null;
  cancel: (() => void) | null;
  onError?: (error: Error) => void;
}

class FrontendAiProcessor {
  private dispatch: AppDispatch | null = null;
  private aiConfig: AiSystemConfig = {
    gesture: {
      static: { enabled: true, confidence: 0.75 },
      dynamic: { enabled: true, confidence: 0.9 },
    },
    emotion: { enabled: false, confidence: 0.6 },
    beauty: { enabled: false, gamma: 1.4, lipAlpha: 0.2, smoothAmount: 30, lipColor: [255, 0, 0] },
  };

  private emotionFaceProcessor: EmotionFaceProcessor | null = null;
  private beautyFilterProcessor: BeautyFilterProcessor | null = null;
  private gestureProcessor: GestureProcessor | null = null;
  private isInitialized = false;
  private runtime: AiRuntime | null = null;
  private runtimeVersion = 0;
  private initializationEpoch = 0;
  private initializationQueue: Promise<void> = Promise.resolve();
  private sdkLoadQueue: Promise<void> = Promise.resolve();
  private initializationAbort: AbortController | null = null;
  private videoSession: VideoSession | null = null;
  private analysisTask: Promise<void> | null = null;
  private analysisFailed = false;
  private beautyFrame: ImageData | null = null;

  private onGestureResultCallback: GestureCallback | null = null;
  private onEmotionResultCallback: EmotionCallback | null = null;

  private lastFrameTime = 0;
  private frameInterval = 100;
  
  // 백그라운드 분석용 변수들
  private backgroundVideoElement: HTMLVideoElement | null = null;
  private backgroundAnalysisActive = false;
  private backgroundAnalysisLoop: number | null = null;
  
  // AI 결과 표시 속도 제어
  private lastGestureResultTime = 0;
  private lastEmotionResultTime = 0;
  private readonly GESTURE_RESULT_INTERVAL = 1500;
  private readonly EMOTION_RESULT_INTERVAL = 5000; // 감정 인식 간격을 5초로 증가

  // ✨ 오버레이 (이미지 포함)
  private activeOverlays: Map<string, {
    image: HTMLImageElement;
    x: number;
    y: number;
    timestamp: number;
    duration: number;
    opacity: number;
    scale: number;
  }> = new Map();

  private readonly STATIC_GESTURE_DURATION = 1500;
  private readonly DYNAMIC_GESTURE_DURATION = 1500;
  private readonly ANIMATION_FADE_DURATION = 150;

  private activeSourceTrack: MediaStreamTrack | null = null;
  private activeProcessedTrack: MediaStreamTrack | null = null;

  // ✨ 로드된 이미지들을 저장할 Map 객체
  private loadedImages: Map<string, HTMLImageElement> = new Map();

  // ✨ 🚨 수정: app.py STATIC_IMG_MAP과 일치하도록 이미지 경로 수정
  private async preloadImages(config: AiSystemConfig, signal: AbortSignal): Promise<Map<string, HTMLImageElement>> {
    const imagePaths: { [key: string]: string } = {
      // Static gestures - app.py STATIC_IMG_MAP과 정확히 일치
      // 🚨 중요: fist와 open_palm 제거 (app.py에서 오버레이되지 않음)
      bad: `${STATIC_IMAGE_BASE_PATH}/bad.png`,
      good: `${STATIC_IMAGE_BASE_PATH}/good.png`,
      gun: `${STATIC_IMAGE_BASE_PATH}/gun.png`,
      heart: `${STATIC_IMAGE_BASE_PATH}/heart.png`,
      ok: `${STATIC_IMAGE_BASE_PATH}/ok.png`,
      promise: `${STATIC_IMAGE_BASE_PATH}/promise.png`,
      rock: `${STATIC_IMAGE_BASE_PATH}/rock.png`,
      victory: `${STATIC_IMAGE_BASE_PATH}/victory.png`,
      
      // Dynamic gestures - app.py DYN_LABELS와 일치
      fire: `${DYNAMIC_IMAGE_BASE_PATH}/fire.png`,
      hi: `${DYNAMIC_IMAGE_BASE_PATH}/hi.png`,
      hit: `${DYNAMIC_IMAGE_BASE_PATH}/hit.png`,
      nono: `${DYNAMIC_IMAGE_BASE_PATH}/nono.png`,
      nyan: `${DYNAMIC_IMAGE_BASE_PATH}/nyan.png`,
      shot: `${DYNAMIC_IMAGE_BASE_PATH}/shot.png`,
      
      // Emotions
      laugh: `${EMOTION_IMAGE_BASE_PATH}/laugh.png`,
      serious: `${EMOTION_IMAGE_BASE_PATH}/serious.png`,
      surprise: `${EMOTION_IMAGE_BASE_PATH}/surprise.png`,
      yawn: `${EMOTION_IMAGE_BASE_PATH}/yawn.png`,
    };

    const images = new Map<string, HTMLImageElement>();
    const promises = Object.entries(imagePaths).filter(([, src]) =>
      src.startsWith(STATIC_IMAGE_BASE_PATH) ? config.gesture.static.enabled :
      src.startsWith(DYNAMIC_IMAGE_BASE_PATH) ? config.gesture.dynamic.enabled : config.emotion.enabled
    ).map(([key, src]) => new Promise<void>((resolve, reject) => {
      const img = new Image();
      const clean = () => {
        img.onload = null;
        img.onerror = null;
        signal.removeEventListener("abort", cancel);
      };
      const cancel = () => { clean(); img.src = ""; reject(abortError()); };
      if (signal.aborted) { cancel(); return; }
      signal.addEventListener("abort", cancel, { once: true });
      img.onload = () => { clean(); images.set(key, img); resolve(); };
      img.onerror = () => { clean(); reject(new Error("AI 효과 이미지를 불러오지 못했습니다.")); };
      img.src = src;
    }));
    await Promise.all(promises);
    return images;
  }

  public cancelInitialization(): void {
    const wasLoading = !!this.initializationAbort;
    this.initializationEpoch++;
    this.initializationAbort?.abort();
    this.initializationAbort = null;
    if (wasLoading) this.dispatch?.(setAiRuntime({ status: this.runtime ? "ready" : "idle" }));
  }

  private disposeRuntime(runtime: AiRuntime): void {
    runtime.gesture?.cleanup();
    runtime.emotion?.cleanup();
    runtime.beauty?.cleanup();
    runtime.images.clear();
  }

  public init(dispatch: AppDispatch, config: AiSystemConfig = this.aiConfig): Promise<void> {
    this.dispatch = dispatch;
    const desired = JSON.parse(JSON.stringify(config)) as AiSystemConfig;
    const key = JSON.stringify(desired);
    const epoch = this.initializationEpoch;
    const operation = this.initializationQueue.then(async () => {
      if (epoch !== this.initializationEpoch) throw abortError();
      if (this.runtime?.key === key && !this.analysisFailed) {
        dispatch(setAiRuntime({ status: "ready" }));
        return;
      }
      dispatch(setAiRuntime({ status: "loading" }));
      const controller = new AbortController();
      this.initializationAbort = controller;
      const candidate: AiRuntime = {
        key, config: desired, images: new Map(),
        gesture: desired.gesture.static.enabled || desired.gesture.dynamic.enabled ? new GestureProcessor(desired) : null,
        emotion: desired.emotion.enabled ? new EmotionFaceProcessor(desired) : null,
        beauty: desired.beauty.enabled ? new BeautyFilterProcessor(desired) : null,
      };
      let committed = false;
      // SDK downloads cannot all be aborted. Keep late loads serialized as well
      // as user requests so a cancelled legacy loader cannot corrupt a retry.
      const loading = this.sdkLoadQueue.then(async () => {
        controller.signal.throwIfAborted();
        if (candidate.gesture || candidate.emotion) {
          try { if (await tf.setBackend("webgl") === false) await tf.setBackend("cpu"); }
          catch { await tf.setBackend("cpu"); }
          await tf.ready();
        }
        controller.signal.throwIfAborted();
        const outcomes = await Promise.allSettled([
          (async () => {
            // Legacy Solutions and Tasks Vision both install Emscripten globals.
            // Finish each face graph before another SDK can replace those globals.
            await candidate.emotion?.init(EXPRESSION_MODEL_PATH, EXPRESSION_SCALER_PATH, FACE_MESH_WASM_PATH);
            controller.signal.throwIfAborted();
            await candidate.beauty?.init(FACE_MESH_WASM_PATH);
            controller.signal.throwIfAborted();
            await candidate.gesture?.init(TASKS_VISION_WASM_PATH);
          })(),
          this.preloadImages(desired, controller.signal).then(images => { candidate.images = images; }),
        ]);
        const failure = outcomes.find((result): result is PromiseRejectedResult => result.status === "rejected");
        if (failure) throw failure.reason;
      });
      this.sdkLoadQueue = loading.catch(() => {});
      try {
        await withTimeout(abortable(loading, controller.signal), 20000, "AI 준비 시간이 초과됐습니다. 기본 영상은 계속 사용할 수 있습니다.");
        if (epoch !== this.initializationEpoch) throw abortError();
        const previous = this.runtime;
        this.runtime = candidate;
        this.runtimeVersion++;
        this.aiConfig = desired;
        this.gestureProcessor = candidate.gesture;
        this.emotionFaceProcessor = candidate.emotion;
        this.beautyFilterProcessor = candidate.beauty;
        this.loadedImages = candidate.images;
        this.isInitialized = true;
        this.analysisFailed = false;
        this.beautyFrame = null;
        committed = true;
        emotionCaptureManager.init(dispatch);
        if (previous) this.disposeRuntime(previous);
        dispatch(setAiRuntime({ status: "ready" }));
      } catch (error) {
        controller.abort();
        this.disposeRuntime(candidate);
        // MediaPipe/TF loads may finish after cancellation; dispose again only
        // after every candidate task settles, without touching a newer runtime.
        void loading.then(() => this.disposeRuntime(candidate), () => this.disposeRuntime(candidate));
        if (epoch === this.initializationEpoch) {
          dispatch(setAiRuntime({ status: "error", error: error instanceof Error ? error.message : "AI를 준비하지 못했습니다." }));
        }
        throw error;
      } finally {
        if (this.initializationAbort === controller) this.initializationAbort = null;
        if (!committed) candidate.images.clear();
      }
    });
    this.initializationQueue = operation.catch(() => {});
    return operation;
  }

  public updateConfig(config: Partial<AiSystemConfig>): Promise<void> {
    if (!this.dispatch) return Promise.reject(new Error("AI 시스템이 준비되지 않았습니다."));
    return this.init(this.dispatch, {
      ...this.aiConfig, ...config,
      gesture: { ...this.aiConfig.gesture, ...config.gesture },
      emotion: { ...this.aiConfig.emotion, ...config.emotion },
      beauty: { ...this.aiConfig.beauty, ...config.beauty },
    });
  }

  public setGestureCallback(callback: GestureCallback | null): void {
    this.onGestureResultCallback = callback;
  }

  public setEmotionCallback(callback: EmotionCallback | null): void {
    this.onEmotionResultCallback = callback;
  }

  public startBackgroundAnalysis(originalTrack: MediaStreamTrack): void {
    if (originalTrack.kind !== "video") {
      console.warn("Only video tracks can be analyzed.");
      return;
    }

    console.log("🤖 Starting background AI analysis...");
    this.stopBackgroundAnalysis();

    this.backgroundVideoElement = document.createElement("video");
    this.backgroundVideoElement.srcObject = new MediaStream([originalTrack]);
    this.backgroundVideoElement.autoplay = true;
    this.backgroundVideoElement.muted = true;
    this.backgroundVideoElement.style.display = "none";

    this.backgroundVideoElement.onloadedmetadata = () => {
      this.backgroundAnalysisActive = true;
      this.runBackgroundAnalysisLoop();
      console.log("✅ Background AI analysis started");
    };

    this.backgroundVideoElement.play();
  }

  public stopBackgroundAnalysis(): void {
    console.log("🛑 Stopping background AI analysis...");
    this.backgroundAnalysisActive = false;
    
    if (this.backgroundAnalysisLoop) {
      cancelAnimationFrame(this.backgroundAnalysisLoop);
      this.backgroundAnalysisLoop = null;
    }

    if (this.backgroundVideoElement) {
      this.backgroundVideoElement.pause();
      this.backgroundVideoElement.srcObject = null;
      this.backgroundVideoElement = null;
    }

    console.log("✅ Background AI analysis stopped");
  }

  private runBackgroundAnalysisLoop(): void {
    if (!this.backgroundAnalysisActive || !this.backgroundVideoElement) {
      // console.log("⚠️ Background analysis not active or video element missing");
      return;
    }

    // console.log("🔄 Starting background analysis loop...");

    const processFrame = async () => {
      if (!this.backgroundAnalysisActive || !this.backgroundVideoElement) {
        // console.log("🛑 Background analysis loop stopped");
        return;
      }

      const now = performance.now();
      const needsProcessing = now - this.lastFrameTime >= this.frameInterval;

      if (needsProcessing && this.isInitialized) {
        // console.log("🔄 Processing frame in background...");
        this.lastFrameTime = now;
        try {
          await this.runAIProcessors(this.backgroundVideoElement, now);
        } catch (e) {
          console.error("❌ Error in background AI processing:", e);
        }
      }

      if (this.backgroundAnalysisActive && this.backgroundVideoElement) this.backgroundAnalysisLoop = requestAnimationFrame(processFrame);
    };

    processFrame();
  }

  public async processVideoTrack(originalTrack: MediaStreamTrack, onError?: (error: Error) => void): Promise<MediaStreamTrack> {
    if (originalTrack.kind !== "video" || originalTrack.readyState === "ended") {
      throw new Error("사용할 수 있는 카메라 트랙이 없습니다.");
    }
    if (!this.isInitialized) throw new Error("AI 시스템이 준비되지 않았습니다.");
    this.stopProcessing();
    const video = document.createElement("video");
    video.muted = true;
    video.playsInline = true;
    const canvas = document.createElement("canvas");
    canvas.width = originalTrack.getSettings().width || 640;
    canvas.height = originalTrack.getSettings().height || 480;
    const ctx = canvas.getContext("2d");
    if (!ctx) throw new Error("AI 영상 출력을 만들지 못했습니다.");
    const session: VideoSession = {
      source: originalTrack.clone(), output: null, video, canvas, frame: null, cancel: null, onError,
    };
    this.videoSession = session;
    this.activeSourceTrack = session.source;
    video.srcObject = new MediaStream([session.source]);
    try {
      const cancelled = new Promise<never>((_, reject) => { session.cancel = () => reject(abortError()); });
      await withTimeout(Promise.race([video.play(), cancelled]), 3000, "AI 영상 준비 시간이 초과됐습니다.");
      session.cancel = null;
      if (this.videoSession !== session) throw abortError();
      session.output = canvas.captureStream(30).getVideoTracks()[0];
      if (!session.output) throw new Error("AI 영상 트랙을 만들지 못했습니다.");
      this.activeProcessedTrack = session.output;
      const render = () => {
        if (this.videoSession !== session) return;
        if (session.output?.readyState === "ended") { this.stopProcessing(); return; }
        session.source.enabled = session.output?.enabled ?? true;
        const now = performance.now();
        if (video.readyState >= 2) {
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          if (this.aiConfig.beauty.enabled && this.beautyFrame && !this.analysisFailed) ctx.putImageData(this.beautyFrame, 0, 0);
          this.renderOverlays(ctx, canvas, now);
          if (session.source.enabled && !this.analysisTask && !this.analysisFailed && now - this.lastFrameTime >= this.frameInterval) {
            this.lastFrameTime = now;
            const version = this.runtimeVersion;
            const beauty = this.beautyFilterProcessor;
            const image = this.aiConfig.beauty.enabled && beauty ? ctx.getImageData(0, 0, canvas.width, canvas.height) : null;
            const task = withTimeout((async () => {
              if (image && beauty) {
                const filtered = await beauty.applyFilters(image);
                if (this.videoSession === session && version === this.runtimeVersion) this.beautyFrame = filtered;
              }
              if (this.videoSession === session && version === this.runtimeVersion) await this.runAIProcessors(video, now);
            })(), 2000, "AI 처리 응답 시간이 초과됐습니다.").catch((error: unknown) => {
              if (this.videoSession !== session || version !== this.runtimeVersion) return;
              this.analysisFailed = true;
              this.beautyFrame = null;
              this.activeOverlays.clear();
              const failure = error instanceof Error ? error : new Error("AI 처리 중 오류가 발생했습니다.");
              this.dispatch?.(setAiRuntime({ status: "error", error: failure.message }));
              session.onError?.(failure);
            }).finally(() => { if (this.analysisTask === task) this.analysisTask = null; });
            this.analysisTask = task;
          }
        }
        if (this.videoSession === session) session.frame = requestAnimationFrame(render);
      };
      render();
      return session.output;
    } catch (error) {
      this.releaseVideoSession(session);
      throw error;
    }
  }

  private releaseVideoSession(session: VideoSession): void {
    session.cancel?.();
    session.cancel = null;
    if (session.frame !== null) cancelAnimationFrame(session.frame);
    session.video.pause();
    session.video.srcObject = null;
    session.source.stop();
    session.output?.stop();
    session.canvas.width = 0;
    session.canvas.height = 0;
    if (this.videoSession === session) {
      this.videoSession = null;
      this.activeSourceTrack = null;
      this.activeProcessedTrack = null;
    }
  }

  private async runAIProcessors(videoElement: HTMLVideoElement, timestamp: number): Promise<void> {
    if (!this.isInitialized) {
      // console.log("⚠️ AI processors not initialized");
      return;
    }
    
    const version = this.runtimeVersion;
    const emotionProcessor = this.emotionFaceProcessor;
    const gestureProcessor = this.gestureProcessor;
    // 감정 인식 처리
    if (this.aiConfig.emotion.enabled && emotionProcessor) {
      const faceCanvas = document.createElement("canvas");
      faceCanvas.width = videoElement.videoWidth;
      faceCanvas.height = videoElement.videoHeight;
      const ctx = faceCanvas.getContext("2d");
      if (!ctx) return;
      ctx.drawImage(videoElement, 0, 0, faceCanvas.width, faceCanvas.height);
      const faceImageData = ctx.getImageData(0, 0, faceCanvas.width, faceCanvas.height);
      const emotionResult = await emotionProcessor.detectEmotion(faceImageData, timestamp);
      
      if (version !== this.runtimeVersion) return;
      if (emotionResult && this.onEmotionResultCallback) {
        const timeSinceLastEmotion = timestamp - this.lastEmotionResultTime;
        if (timeSinceLastEmotion >= this.EMOTION_RESULT_INTERVAL || emotionResult.label !== "none") {
          this.onEmotionResultCallback(emotionResult);
          this.lastEmotionResultTime = timestamp;
          // this.addEmotionOverlay(emotionResult, timestamp); // 감정 이미지 오버레이 비활성화
        }
      }
    }

    // 제스처 인식 처리
    if (
      (this.aiConfig.gesture.static.enabled || this.aiConfig.gesture.dynamic.enabled) &&
      gestureProcessor
    ) {
      const gestureResult = await gestureProcessor.detectGestures(videoElement, timestamp);
      
      if (version !== this.runtimeVersion) return;
      if (gestureResult && this.onGestureResultCallback) {
        const timeSinceLastGesture = timestamp - this.lastGestureResultTime;
        const hasValidGesture = gestureResult.static.label !== "none" || gestureResult.dynamic.label !== "none";
        
        if (timeSinceLastGesture >= this.GESTURE_RESULT_INTERVAL || hasValidGesture) {
          this.onGestureResultCallback(gestureResult);
          this.lastGestureResultTime = timestamp;
          this.addGestureOverlay(gestureResult, timestamp);
        }
      }
    }
  }

  // ✨ 렌더링 함수 수정: drawImage 사용
  private renderOverlays(
    ctx: CanvasRenderingContext2D,
    canvas: HTMLCanvasElement,
    now: number
  ): void {
    this.activeOverlays.forEach((item, key) => {
      const elapsed = now - item.timestamp;
      const remaining = item.duration - elapsed;
      if (remaining > 0) {
        this.updateGestureAnimation(item, elapsed);
        
        const x = item.x * canvas.width;
        const y = item.y * canvas.height;
        
        const baseSize = Math.min(canvas.width, canvas.height) * 0.3;
        const imgWidth = baseSize * item.scale;
        const imgHeight = (baseSize * (item.image.height / item.image.width)) * item.scale;

        ctx.save();
        ctx.globalAlpha = item.opacity;
        
        // ✨ drawImage를 사용하여 이미지 렌더링
        ctx.drawImage(
          item.image,
          x - imgWidth / 2, // 중앙 정렬
          y - imgHeight / 2,
          imgWidth,
          imgHeight
        );
        
        ctx.restore();
      } else {
        this.activeOverlays.delete(key);
      }
    });
  }

  private updateGestureAnimation(item: {
    image: HTMLImageElement;
    x: number;
    y: number;
    timestamp: number;
    duration: number;
    opacity: number;
    scale: number;
  }, elapsed: number): void {
    const fadeInDuration = this.ANIMATION_FADE_DURATION;
    const fadeOutDuration = this.ANIMATION_FADE_DURATION;
    const totalDuration = item.duration;

    if (elapsed < fadeInDuration) {
        // Fade In
        const progress = elapsed / fadeInDuration;
        item.opacity = progress;
        item.scale = 1.0 + 0.5 * (1 - progress); // 시작할 때 크게
    } else if (elapsed < totalDuration - fadeOutDuration) {
        // Display
        item.opacity = 1.0;
        item.scale = 1.0;
    } else {
        // Fade Out
        const fadeOutElapsed = elapsed - (totalDuration - fadeOutDuration);
        const progress = fadeOutElapsed / fadeOutDuration;
        item.opacity = 1.0 - progress;
        item.scale = 1.0 - 0.2 * progress; // 사라지면서 작게
    }
  }

  // ✨ 수정: 실제 손 위치를 사용한 제스처 오버레이
  private addGestureOverlay(gestureResult: GestureResult, timestamp: number): void {
    const processGesture = (
        gesture: { label: string; confidence: number } | null,
        type: 'static' | 'dynamic'
    ) => {
        // shot은 특별히 높은 임계값, 정적 제스처도 조금 높임
        const confidenceThreshold = gesture?.label === 'shot' ? 0.98 : (type === 'static' ? 0.8 : 0.85);
        if (!gesture || gesture.label === "none" || gesture.confidence < confidenceThreshold) return;

        const image = this.getImageForLabel(gesture.label);
        if (!image) return; // 🚨 app.py처럼 이미지 없으면 오버레이 안 함

        // ✨ 실제 손 위치 추출
        let handX = 0.5; // 기본값 (중앙)
        let handY = 0.5; // 기본값 (중앙)
        
        // console.log(`🖐️ [${type}] ${gesture.label} - landmarks:`, gestureResult.landmarks?.length || 0);
        
        if (gestureResult.landmarks && gestureResult.landmarks.length > 0) {
          // 첫 번째 손의 손목 좌표 (랜드마크 0번)를 사용
          // landmarks는 [hand1_landmark0, hand1_landmark1, ...] 형태
          // 각 랜드마크는 [x, y, z] 배열
          const wristLandmark = gestureResult.landmarks[0]; // 손목 (landmark 0)
          // console.log(`🎯 손목 랜드마크:`, wristLandmark);
          
          if (wristLandmark && wristLandmark.length >= 2) {
            handX = wristLandmark[0]; // 정규화된 x 좌표 (0-1)
            handY = wristLandmark[1]; // 정규화된 y 좌표 (0-1)
            
            // console.log(`📍 원본 손 위치: (${handX.toFixed(3)}, ${handY.toFixed(3)})`);
            
            // 손목에서 손 위쪽으로 오버레이 위치 조정 (Y축 위로 이동)
            handY = handY - 0.15; // 손목에서 위로 15% 올리기
            
            // 화면 경계 체크 및 보정 (오버레이가 화면 밖으로 나가지 않도록)
            const margin = 0.1; // 10% 여백
            handX = Math.max(margin, Math.min(1 - margin, handX));
            handY = Math.max(margin, Math.min(1 - margin, handY));
            
            // console.log(`🎯 최종 오버레이 위치: (${handX.toFixed(3)}, ${handY.toFixed(3)})`);
          } else {
            // console.warn(`⚠️ 손목 랜드마크 데이터 부족:`, wristLandmark);
          }
        } else {
          // console.warn(`⚠️ 랜드마크 데이터 없음, 기본값 사용: (${handX}, ${handY})`);
        }

        const key = `${type}_${gesture.label}_${timestamp}`;
        this.activeOverlays.set(key, {
            image,
            x: handX, // 실제 손 위치 사용
            y: handY, // 실제 손 위치 사용
            timestamp,
            duration: type === 'static' ? this.STATIC_GESTURE_DURATION : this.DYNAMIC_GESTURE_DURATION,
            opacity: 0,
            scale: 5,
        });
    };

    if (this.aiConfig.gesture.static.enabled) processGesture(gestureResult.static, 'static');
    if (this.aiConfig.gesture.dynamic.enabled) processGesture(gestureResult.dynamic, 'dynamic');
  }

  private addEmotionOverlay(emotionResult: EmotionResult, timestamp: number): void {
    if (emotionResult.label === "none" || emotionResult.confidence < 0.8) return; // 신뢰도 임계값을 0.8로 상승

    const image = this.getImageForLabel(emotionResult.label);
    if (!image) return;

    // 얼굴 위치 기본값 (화면 중앙 상단)
    let faceX = 0.5; // 화면 중앙
    let faceY = 0.3; // 화면 상단 30% 지점
    
    // 얼굴 랜드마크가 있으면 실제 얼굴 위치 사용
    if (emotionResult.faceLandmarks && emotionResult.faceLandmarks.length > 0) {
      // 얼굴 랜드마크의 중심점 계산
      const landmarks = emotionResult.faceLandmarks;
      const avgX = landmarks.reduce((sum, lm) => sum + lm[0], 0) / landmarks.length;
      const avgY = landmarks.reduce((sum, lm) => sum + lm[1], 0) / landmarks.length;
      
      faceX = avgX;
      faceY = avgY - 0.1; // 얼굴 위쪽에 표시
      
      // 화면 경계 체크
      const margin = 0.1;
      faceX = Math.max(margin, Math.min(1 - margin, faceX));
      faceY = Math.max(margin, Math.min(1 - margin, faceY));
    }

    const key = `emotion_${emotionResult.label}_${timestamp}`;
    this.activeOverlays.set(key, {
        image,
        x: faceX,
        y: faceY,
        timestamp,
        duration: 2000,
        opacity: 0,
        scale: 5,
    });
  }

  // ✨ 🚨 수정: app.py와 동일하게 fist/open_palm은 null 반환
  private getImageForLabel(label: string): HTMLImageElement | null {
    // app.py의 STATIC_IMG_MAP에 없는 제스처들은 오버레이하지 않음
    if (label === 'fist' || label === 'open_palm') {
      // console.log(`🚨 ${label} detected but no overlay image (matches app.py behavior)`);
      return null;
    }
    
    return this.loadedImages.get(label) || null;
  }

  public stopProcessing(): void {
    this.runtimeVersion++;
    if (this.videoSession) this.releaseVideoSession(this.videoSession);
    this.beautyFrame = null;
    this.activeOverlays.clear();
    this.stopBackgroundAnalysis();
  }

  public cleanup(): void {
    this.cancelInitialization();
    this.stopProcessing();
    this.runtimeVersion++;
    if (this.runtime) this.disposeRuntime(this.runtime);
    this.runtime = null;
    this.gestureProcessor = null;
    this.emotionFaceProcessor = null;
    this.beautyFilterProcessor = null;
    this.isInitialized = false;
    this.analysisFailed = false;
    this.analysisTask = null;
    this.lastFrameTime = 0;
    this.lastGestureResultTime = 0;
    this.lastEmotionResultTime = 0;
    this.onGestureResultCallback = null;
    this.onEmotionResultCallback = null;
    this.dispatch?.(setAiRuntime({ status: "idle" }));
    this.dispatch = null;
    emotionCaptureManager.cleanup();
    this.initializationQueue = Promise.resolve();
  }
}

export const frontendAiProcessor = new FrontendAiProcessor();
