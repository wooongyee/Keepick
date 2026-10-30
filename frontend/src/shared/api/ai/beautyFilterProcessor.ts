import { loadFaceMesh, initializeFaceMesh } from "./faceMeshLoader";
import { withTimeout } from "./asyncResource";
// src/shared/api/ai/beautyFilterProcessor.ts
import { AiSystemConfig } from "@/shared/types/ai.types"; // AiSystemConfig 임포트

// Dynamic import types
type FaceMeshInstance = import("@mediapipe/face_mesh").FaceMesh;
type Results = import("@mediapipe/face_mesh").Results;

// MediaPipe Face Mesh의 Results에서 랜드마크 타입 추출
// @mediapipe/face_mesh 패키지의 랜드마크는 visibility가 optional일 수 있으므로 이 타입으로 처리합니다.
type FaceMeshLandmark = Results["multiFaceLandmarks"][number][number];

// 입술 랜드마크 인덱스 (app.py의 LIPS_IDX 참고)
const LIPS_IDX = [
  61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95,
];

export class BeautyFilterProcessor {
  private faceMesh: FaceMeshInstance | null = null; // 뷰티 필터용 Face Mesh 인스턴스 (따로 관리)
  private beautyGammaLUT: Uint8Array | null = null;
  private lastBeautyGamma = 1.4; // Initial gamma value, from app.py

  // MediaPipe onResults 콜백에서 결과 Promise용
  private _faceMeshResultResolver: ((value: Results) => void) | null = null;

  // 초기화 시 AI 설정 주입
  private aiConfig: AiSystemConfig;

  constructor(initialConfig: AiSystemConfig) {
    this.aiConfig = initialConfig;
  }

  /**
   * BeautyFilterProcessor를 초기화합니다.
   * @param wasmPath MediaPipe Face Mesh WASM 경로
   */
  public async init(wasmPath: string): Promise<void> {
    // Client-side check
    if (typeof window === 'undefined') {
      console.warn("BeautyFilterProcessor: Cannot initialize on server side");
      this.aiConfig.beauty.enabled = false;
      return;
    }

    try {
      // Dynamic import of MediaPipe Face Mesh - 브라우저 환경에서만
      if (typeof window === 'undefined') {
        console.warn("BeautyFilterProcessor: Cannot initialize MediaPipe on server side");
        this.aiConfig.beauty.enabled = false;
        return;
      }

      const FaceMesh = await loadFaceMesh();
      this.faceMesh = new FaceMesh({ locateFile: file => `${wasmPath}/${file}` });

      this.faceMesh.setOptions({
        maxNumFaces: 1, // 뷰티 필터는 일반적으로 단일 얼굴에 적용
        refineLandmarks: false,
        minDetectionConfidence: 0.5, // 뷰티 필터는 조금 더 관대한 감지 임계값 사용 가능
      });

      // FaceMesh의 onResults 콜백을 등록합니다.
      this.faceMesh.onResults((results) => {
        if (this._faceMeshResultResolver) {
          this._faceMeshResultResolver(results);
          this._faceMeshResultResolver = null; // Promise가 한 번 resolve되면 초기화
        }
      });
      await initializeFaceMesh(this.faceMesh);
      console.log("BeautyFilterProcessor: Face Mesh initialized for beauty.");
    } catch (error) {
      console.error("BeautyFilterProcessor: Failed to load Face Mesh:", error);
      this.cleanup();
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
      beauty: { ...this.aiConfig.beauty, ...config.beauty },
    };
  }

  /**
   * 입력 ImageData에 뷰티 필터를 적용합니다.
   * @param imageData 적용할 ImageData 객체
   * @returns 필터가 적용된 새 ImageData 객체 (또는 원본)
   */
  public async applyFilters(imageData: ImageData): Promise<ImageData> {
    // Client-side check
    if (typeof window === 'undefined') {
      return imageData;
    }

    if (!this.aiConfig.beauty.enabled || !this.faceMesh) {
      return imageData;
    }

    // 복사본을 만들어 원본 ImageData를 변경하지 않도록 합니다.
    const processedImageData = new ImageData(
      new Uint8ClampedArray(imageData.data),
      imageData.width,
      imageData.height
    );

    const canvas = document.createElement("canvas");
    canvas.width = imageData.width;
    canvas.height = imageData.height;
    const ctx = canvas.getContext("2d");
    if (!ctx) return processedImageData;
    ctx.putImageData(imageData, 0, 0); // ImageData를 Canvas에 그림

    // MediaPipe `send()` 호출 결과를 `Promise`로 기다립니다.
    const resultsPromise = new Promise<Results>((resolve) => {
      this._faceMeshResultResolver = resolve;
    });

    // Canvas 엘리먼트를 send()에 전달
    const faceMesh = this.faceMesh;
    const results = await withTimeout((async () => {
      await faceMesh.send({ image: canvas });
      return resultsPromise;
    })(), 1500, "뷰티 필터 응답 시간이 초과됐습니다.").finally(() => {
      this._faceMeshResultResolver = null;
    });

    const faceLandmarks = results.multiFaceLandmarks?.[0];

    // 1. 감마 보정
    const gammaConfig = this.aiConfig.beauty.gamma;
    if (gammaConfig !== undefined) {
      if (Math.abs(gammaConfig - this.lastBeautyGamma) > 0.003 || !this.beautyGammaLUT) {
        this.beautyGammaLUT = this.buildGammaLUT(gammaConfig);
        this.lastBeautyGamma = gammaConfig;
      }
      if (this.beautyGammaLUT) {
        for (let i = 0; i < processedImageData.data.length; i += 4) {
          processedImageData.data[i] = this.beautyGammaLUT[processedImageData.data[i]]; // R
          processedImageData.data[i + 1] = this.beautyGammaLUT[processedImageData.data[i + 1]]; // G
          processedImageData.data[i + 2] = this.beautyGammaLUT[processedImageData.data[i + 2]]; // B
        }
      }
    }

    if (faceLandmarks) {
      // 2. 립 컬러 (얼굴 랜드마크가 감지된 경우에만)
      const lipAlphaConfig = this.aiConfig.beauty.lipAlpha;
      const lipColorConfig = this.aiConfig.beauty.lipColor;
      if (lipAlphaConfig !== undefined && lipAlphaConfig > 0 && lipColorConfig) {
        this.applyLipColor(processedImageData, faceLandmarks, lipAlphaConfig, lipColorConfig);
      }

      // 3. 피부 스무딩 (얼굴 랜드마크가 감지된 경우에만)
      const smoothAmountConfig = this.aiConfig.beauty.smoothAmount;
      if (smoothAmountConfig !== undefined && smoothAmountConfig > 0) {
        // 이 부분은 Canvas/JS에서 성능상 가장 어려운 부분.
        // app.py의 edgePreservingFilter에 상응하는 고성능 구현은 WebGL/WASM이 필수적.
        // 여기서는 간단한 블러로 대체하거나, 실제 구현 시 외부 라이브러리/셰이더 사용 권장.
        this.applySkinSmoothing(processedImageData, faceLandmarks, smoothAmountConfig);
      }
    }

    return processedImageData;
  }

  /**
   * 감마 LUT를 생성합니다 (app.py의 build_gamma_lut 포팅).
   */
  private buildGammaLUT(gamma: number): Uint8Array {
    const g = Math.max(0.01, gamma);
    const inv = 1.0 / g;
    const table = new Uint8Array(256);
    for (let i = 0; i < 256; i++) {
      table[i] = Math.min(255, Math.floor(Math.pow(i / 255.0, inv) * 255.0));
    }
    return table;
  }

  /**
   * 입술에 색을 적용합니다 (app.py의 apply_lip_color_roi 포팅).
   * @param imageData 대상 ImageData
   * @param faceLandmarks 얼굴 랜드마크
   * @param alpha 투명도
   * @param color RGB 색상 배열
   */
  private applyLipColor(
    imageData: ImageData,
    faceLandmarks: FaceMeshLandmark[], // FaceMeshLandmark 타입 사용
    alpha: number,
    color: [number, number, number]
  ): void {
    if (alpha <= 0) return;

    const { data, width, height } = imageData;
    const lipPoints = LIPS_IDX.map((idx) => ({
      x: Math.floor(faceLandmarks[idx].x * width),
      y: Math.floor(faceLandmarks[idx].y * height),
    }));

    // 입술 마스크 캔버스 생성
    const lipMaskCanvas = document.createElement("canvas");
    lipMaskCanvas.width = width;
    lipMaskCanvas.height = height;
    const lipMaskCtx = lipMaskCanvas.getContext("2d");
    if (!lipMaskCtx) return;

    // 입술 영역 그리기
    lipMaskCtx.fillStyle = "white"; // 마스크는 흰색으로 그림
    lipMaskCtx.beginPath();
    if (lipPoints.length > 0) {
      lipMaskCtx.moveTo(lipPoints[0].x, lipPoints[0].y);
      for (let i = 1; i < lipPoints.length; i++) {
        lipMaskCtx.lineTo(lipPoints[i].x, lipPoints[i].y);
      }
    }

    lipMaskCtx.closePath();
    lipMaskCtx.fill();

    // 마스크 블러 (app.py의 blur_size와 동일한 효과를 내기 위해)
    const blurSize = 15; // app.py 값
    if (blurSize > 0) {
      lipMaskCtx.filter = `blur(${blurSize}px)`;
      lipMaskCtx.drawImage(lipMaskCanvas, 0, 0); // 필터 적용을 위해 다시 그림
    }

    const maskData = lipMaskCtx.getImageData(0, 0, width, height).data;

    for (let i = 0; i < data.length; i += 4) {
      const maskAlphaVal = maskData[i + 3] / 255.0; // 마스크의 알파 채널 사용
      const blendedAlpha = maskAlphaVal * alpha; // 마스크와 설정된 알파값 결합

      // 픽셀 블렌딩
      data[i] = Math.min(255, data[i] * (1 - blendedAlpha) + color[0] * blendedAlpha); // R
      data[i + 1] = Math.min(255, data[i + 1] * (1 - blendedAlpha) + color[1] * blendedAlpha); // G
      data[i + 2] = Math.min(255, data[i + 2] * (1 - blendedAlpha) + color[2] * blendedAlpha); // B
    }
  }

  /**
   * 피부 스무딩을 적용합니다 (app.py의 smooth_frame 및 get_skin_mask 포팅).
   * 경고: 이 구현은 성능 최적화가 되어 있지 않으며, app.py의 cv2.edgePreservingFilter만큼 정확하지 않습니다.
   * 실시간 사용 시 WebGL/WASM 기반의 최적화된 라이브러리 사용을 강력히 권장합니다.
   * @param imageData 대상 ImageData
   * @param faceLandmarks 얼굴 랜드마크 (피부 마스킹에 사용될 수 있으나 현재는 사용 안 함)
   * @param smoothAmount 스무딩 강도 (0-100)
   */
  private applySkinSmoothing(
    imageData: ImageData,
    faceLandmarks: FaceMeshLandmark[], // FaceMeshLandmark 타입 사용
    smoothAmount: number
  ): void {
    if (smoothAmount <= 0) return;

    const { width, height } = imageData;
    const tempCanvas = document.createElement("canvas");
    tempCanvas.width = width;
    tempCanvas.height = height;
    const tempCtx = tempCanvas.getContext("2d");
    if (!tempCtx) return;

    tempCtx.putImageData(imageData, 0, 0);

    // app.py의 `get_skin_mask` 로직은 복잡하므로 여기서는 간단한 블러 처리로 대체합니다.
    // 실제 피부 스무딩은 엣지 보존 필터(Edge Preserving Filter)가 필요하며,
    // 이를 Canvas filter로 완벽히 대체하기 어렵습니다.
    // 여기서는 전체 이미지에 blur를 적용하고, 원본과 블렌딩하는 방식으로 단순화합니다.
    const blurRadius = Math.floor(smoothAmount / 10); // smoothAmount를 blur radius로 변환 (예: 100 -> 10px)
    if (blurRadius > 0) {
      tempCtx.filter = `blur(${blurRadius}px)`;
      tempCtx.drawImage(tempCanvas, 0, 0); // 블러 필터 적용
    }

    const blurredImageData = tempCtx.getImageData(0, 0, width, height);
    const blurredData = blurredImageData.data;
    const originalData = imageData.data;

    // 원본과 블러된 이미지를 smoothAmount에 비례하여 블렌딩
    const blendRatio = smoothAmount / 100;
    for (let i = 0; i < originalData.length; i += 4) {
      originalData[i] = Math.round(
        originalData[i] * (1 - blendRatio) + blurredData[i] * blendRatio
      );
      originalData[i + 1] = Math.round(
        originalData[i + 1] * (1 - blendRatio) + blurredData[i + 1] * blendRatio
      );
      originalData[i + 2] = Math.round(
        originalData[i + 2] * (1 - blendRatio) + blurredData[i + 2] * blendRatio
      );
    }
    // imageData 객체는 참조로 전달되므로, 직접 값을 변경하면 호출자에게 반영됩니다.
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
    this.beautyGammaLUT = null;
  }
}
