// src/shared/api/mediasoupManager.ts
import { Device } from "mediasoup-client";
import { Transport, RtpCapabilities } from "mediasoup-client/types";
import { AppDispatch } from "@/shared/config/store";
import { mediaTrackManager } from "./mediaTrackManager";
import { screenShareManager } from "./screenShareManager"; // 🆕 화면 공유 매니저 추가
import {
  setDeviceLoaded,
  setRtpCapabilities,
  setTransports,
  setTransportConnected,
  addRemotePeer,
  removeRemotePeer,
  resetMediaState,
} from "@/entities/video-conference/media/model/mediaSlice";
import { webrtcHandler } from "./socket";
import { ProducerAppData } from "@/shared/types/webrtc.types";
import { initializeAISystem, cleanupAISystem, frontendAiProcessor } from "./ai";
import { AiSystemConfig } from "@/shared/types/ai.types";
import { setAiEnabled, setAiRuntime } from "@/entities/video-conference/ai/model/aiSlice";
import { abortError } from "./ai/asyncResource";

class MediasoupManager {
  private device: Device | null = null;
  private sendTransport: Transport | null = null;
  private recvTransport: Transport | null = null;
  private dispatch: AppDispatch | null = null;
  private currentRoomId: string = "";
  private isLocalMediaStarted = false; // 로컬 미디어 시작 상태 추가
  
  private cameraSourceTrack: MediaStreamTrack | null = null;
  private aiVideoActive = false;
  private activeAiConfig: AiSystemConfig | null = null;
  private aiRequestVersion = 0;
  private aiQueue: Promise<void> = Promise.resolve();
  // Transport 복구 관리
  private transportRecoveryInProgress = false;
  private lastTransportFailureTime = 0;
  private transportRecoveryDelay = 5000; // 5초

  /**
   * MediasoupManager를 초기화합니다.
   * AI 모델은 사용자가 기능을 켤 때 별도로 초기화합니다.
   * @param dispatch Redux dispatch 함수
   */
  public async init(dispatch: AppDispatch): Promise<void> {
    this.dispatch = dispatch;
    mediaTrackManager.init(dispatch);
    if (this.device) return; // Model loading belongs to the explicit AI action.

    try {
      console.log("🚀 Initializing MediaSoup...");

      // Device 생성 및 초기화
      this.device = new Device();
      console.log("✅ MediaSoup Device created");
    } catch (error) {
      console.error("❌ MediaSoup initialization failed:", error);
      throw error;
    }
  }

  // RTP Capabilities 로드
  public async loadDevice(rtpCapabilities: RtpCapabilities): Promise<void> {
    if (!this.device || !this.dispatch) {
      throw new Error("Device not initialized");
    }

    // 이미 로드된 경우 스킵
    if (this.device.loaded) {
      console.log("⚠️ Device already loaded, skipping...");
      return;
    }

    try {
      await this.device.load({ routerRtpCapabilities: rtpCapabilities });

      this.dispatch(setRtpCapabilities(rtpCapabilities));
      this.dispatch(setDeviceLoaded(true));

      // 🆕 Device 로드 완료 후 ScreenShareManager 초기화
      if (this.device) {
        screenShareManager.init(this.dispatch, this.device);
        console.log("🖥️ ScreenShareManager initialized with device");
      }

      console.log("✅ Device loaded with RTP capabilities");
    } catch (error) {
      console.error("❌ Failed to load device:", error);
      throw error;
    }
  }

  // Transport 생성
  public async createTransports(roomId: string): Promise<void> {
    if (!this.device || !this.dispatch) {
      throw new Error("Device not initialized");
    }

    this.currentRoomId = roomId;

    try {
      // Send Transport 생성
      const sendTransportData = await webrtcHandler.createProducerTransport({ roomId });
      console.log("📤 Send transport data:", sendTransportData);
      this.sendTransport = this.device.createSendTransport(sendTransportData);
      this.setupSendTransportEvents(roomId);

      // Recv Transport 생성
      const recvTransportData = await webrtcHandler.createConsumerTransport({ roomId });
      console.log("📥 Recv transport data:", recvTransportData);
      this.recvTransport = this.device.createRecvTransport(recvTransportData);
      this.setupRecvTransportEvents();

      // MediaTrackManager에 Transport 설정
      mediaTrackManager.setTransports(this.sendTransport, this.recvTransport, roomId);

      this.dispatch(
        setTransports({
          sendId: this.sendTransport.id,
          recvId: this.recvTransport.id,
        })
      );

      console.log("✅ Transports created successfully");
    } catch (error) {
      console.error("❌ Failed to create transports:", error);
      throw error;
    }
  }

  // 로컬 미디어 시작
  /**
   * 로컬 미디어를 시작합니다.
   * @param enableAI 기본 미디어 준비 후 AI를 활성화할지 여부
   * @param aiConfig 선택한 AI 기능 설정
   */
  public async startLocalMedia(enableAI: boolean = false, aiConfig?: Partial<AiSystemConfig>): Promise<void> {
    try {
      // 이미 미디어가 시작된 경우 스킵
      if (this.isLocalMediaStarted) {
        console.log("🔄 Media already started.");
        return;
      }

      const stream = await navigator.mediaDevices.getUserMedia({
        video: { width: 1280, height: 720 },
        audio: true,
      });

      // 개별 트랙으로 Producer 생성
      const audioTrack = stream.getAudioTracks()[0];
      const videoTrack = stream.getVideoTracks()[0];

      if (audioTrack) {
        await mediaTrackManager.addLocalTrack(audioTrack, "local", "camera");
      }

      if (videoTrack) {
        this.cameraSourceTrack = videoTrack;
        // Producer.replaceTrack stops its old track by default. Only give it
        // clones; the manager retains the camera source throughout the call.
        await mediaTrackManager.addLocalTrack(videoTrack.clone(), "local", "camera");
      }
      if (enableAI && aiConfig) await this.toggleAIDuringConference(true, aiConfig);

      this.isLocalMediaStarted = true;
      console.log("✅ Local media started");
    } catch (error) {
      console.error("❌ Failed to start local media:", error);
      throw error;
    }
  }


  /**
   * AI 설정만 업데이트합니다 (트랙 교체 없이).
   * @param aiConfig AI 시스템 설정
   */
  public updateAIConfig(aiConfig: Partial<AiSystemConfig>): Promise<void> {
    return this.toggleAIDuringConference(true, aiConfig);
  }

  private async restoreCamera(): Promise<void> {
    if (!this.aiVideoActive) {
      frontendAiProcessor.stopProcessing();
      return;
    }
    const current = mediaTrackManager.getLocalCameraTrackInfo("video");
    const source = this.cameraSourceTrack;
    if (!current || !source || source.readyState === "ended") throw new Error("원본 카메라를 복구하지 못했습니다.");
    const track = source.clone();
    track.enabled = current.track.enabled;
    try { await mediaTrackManager.replaceLocalTrack(current.trackId, track); }
    catch (error) { track.stop(); throw error; }
    this.aiVideoActive = false;
    frontendAiProcessor.stopProcessing();
  }

  public toggleAIDuringConference(enabled: boolean, aiConfig?: Partial<AiSystemConfig>): Promise<void> {
    const version = ++this.aiRequestVersion;
    frontendAiProcessor.cancelInitialization();
    if (!this.aiVideoActive) frontendAiProcessor.stopProcessing();
    const operation = this.aiQueue.then(async () => {
      if (version !== this.aiRequestVersion) return;
      if (!enabled) { await this.restoreCamera(); return; }
      if (!this.dispatch || !this.cameraSourceTrack) throw new Error("카메라가 준비된 뒤 AI를 켜 주세요.");
      const dispatch = this.dispatch;
      const config: AiSystemConfig = {
        gesture: aiConfig?.gesture ?? { static: { enabled: true, confidence: 0.7 }, dynamic: { enabled: true, confidence: 0.7 } },
        emotion: aiConfig?.emotion ?? { enabled: false, confidence: 0.5 },
        beauty: aiConfig?.beauty ?? { enabled: false },
      };
      try {
        await initializeAISystem(dispatch, config);
        if (version !== this.aiRequestVersion) throw abortError();
        // Feature changes update the runtime while retaining the same output
        // track, instead of recursively processing the already processed video.
        this.activeAiConfig = config;
        if (this.aiVideoActive) return;
        const current = mediaTrackManager.getLocalCameraTrackInfo("video");
        if (!current) throw new Error("송출 카메라 트랙이 없습니다.");
        const output = await frontendAiProcessor.processVideoTrack(this.cameraSourceTrack!, (error) => {
          dispatch(setAiRuntime({ status: "error", error: error.message }));
          dispatch(setAiEnabled(false));
        });
        if (version !== this.aiRequestVersion) {
          frontendAiProcessor.stopProcessing();
          throw abortError();
        }
        output.enabled = current.track.enabled;
        await mediaTrackManager.replaceLocalTrack(current.trackId, output);
        this.aiVideoActive = true;
      } catch (error) {
        if (version !== this.aiRequestVersion) return;
        await this.restoreCamera();
        dispatch(setAiEnabled(false));
        dispatch(setAiRuntime({ status: "error", error: error instanceof Error ? error.message : "AI를 시작하지 못했습니다." }));
        throw error;
      }
    });
    this.aiQueue = operation.catch(() => {});
    return operation;
  }

  // 소비 요청을 mediaTrackManager에 위임
  public async consumeProducer(data: {
    producerId: string;
    producerSocketId: string;
    kind?: "audio" | "video";
    appData?: { type?: string; trackType?: string; peerName?: string };
  }): Promise<void> {
    const { producerId, producerSocketId, kind, appData } = data;

    if (!this.device) {
      console.error("[SKIP] Device not initialized. Skipping consumption.");
      return;
    }

    try {
      const isScreenShare = appData?.type === "screenshare" || appData?.type === "screen" || appData?.trackType === "screen";
      const trackType = isScreenShare ? "screen" : "camera";

      console.log(`[CONSUME] Delegating consume for producer: ${producerId} (${kind}, ${trackType})`);

      if (isScreenShare) {
        await screenShareManager.consumeScreenShare(
          this.currentRoomId,
          producerId,
          producerSocketId,
          appData?.peerName || "Unknown User"
        );
      } else {
        await mediaTrackManager.consumeAndAddRemoteTrack(
          producerId,
          producerSocketId,
          kind || "video",
          this.device.rtpCapabilities,
          trackType
        );
      }

      console.log(`[SUCCESS] Consumer creation delegated for producer: ${producerId}`);
    } catch (error) {
      console.error(`❌ Failed to delegate consume for producer ${producerId}:`, error);
      
      // Transport 연결 문제인 경우 복구 시도
      if (this.isTransportError(error)) {
        console.log(`🔄 Transport error detected, attempting recovery...`);
        await this.handleTransportRecovery();
        // 복구 후 재시도
        try {
          const isScreenShare = appData?.type === "screenshare" || appData?.type === "screen" || appData?.trackType === "screen";
          const trackType = isScreenShare ? "screen" : "camera";
          
          if (isScreenShare) {
            await screenShareManager.consumeScreenShare(
              this.currentRoomId,
              producerId,
              producerSocketId,
              appData?.peerName || "Unknown User"
            );
          } else {
            await mediaTrackManager.consumeAndAddRemoteTrack(
              producerId,
              producerSocketId,
              kind || "video",
              this.device.rtpCapabilities,
              trackType
            );
          }
          console.log(`✅ Recovery successful after transport recovery for producer: ${producerId}`);
          return;
        } catch (retryError) {
          console.error(`❌ Recovery failed even after transport recovery:`, retryError);
        }
      }
      
      throw error;
    }
  }

  // 피어 추가
  public addPeer(socketId: string, peerName: string): void {
    if (!this.dispatch) return;

    this.dispatch(
      addRemotePeer({
        socketId,
        peerId: socketId,
        peerName,
      })
    );

    console.log(`👥 Peer added: ${peerName} (${socketId})`);
  }

  // 🆕 피어 제거 (화면 공유도 함께 정리)
  public removePeer(socketId: string): void {
    if (!this.dispatch) return;

    console.log(`🧹 Starting peer removal cleanup for ${socketId}`);

    // 1. MediaTrackManager에서 해당 피어의 모든 트랙 제거
    mediaTrackManager.removeRemoteTracksByPeer(socketId);

    // 2. ScreenShareManager에서도 정리
    screenShareManager.removeRemoteScreenShare("unknown", socketId);

    // 3. Redux 상태에서 피어 제거
    this.dispatch(removeRemotePeer(socketId));

    console.log(`✅ Peer removal completed for ${socketId}.`);
  }

  // Producer 종료 처리
  public handleProducerClosed(producerId: string): void {
    console.log(`🔌 Producer ${producerId} closed - delegating cleanup to managers.`);

    const trackInfo = mediaTrackManager.getTrackByProducerId(producerId);
    if (!trackInfo) {
      console.warn(`⚠️ No track found for producer ${producerId} - already cleaned up?`);
      return;
    }

    if (trackInfo.trackType === "screen") {
      screenShareManager.removeRemoteScreenShare(producerId, trackInfo.peerId);
    } else {
      mediaTrackManager.removeTrackByProducerId(producerId);
    }
  }

  // 🆕 원격 Producer pause 처리
  public handleRemoteProducerPaused(producerId: string, socketId: string): void {
    console.log(`⏸️ Remote producer ${producerId} paused from ${socketId}`);
    
    const consumerInfo = mediaTrackManager.getRemoteTrackByProducerId(producerId);
    if (consumerInfo) {
      try {
        if (consumerInfo.consumer) {
          consumerInfo.consumer.pause();
          console.log(`⏸️ Consumer paused for producer ${producerId}`);
          
          // Redux 상태 업데이트
          mediaTrackManager.updateRemoteTrackState(socketId, consumerInfo.kind, { enabled: false });
        }
      } catch (error) {
        console.error(`❌ Failed to pause consumer for producer ${producerId}:`, error);
      }
    }
  }

  // 🆕 원격 Producer resume 처리
  public handleRemoteProducerResumed(producerId: string, socketId: string): void {
    console.log(`▶️ Remote producer ${producerId} resumed from ${socketId}`);
    
    const consumerInfo = mediaTrackManager.getRemoteTrackByProducerId(producerId);
    if (consumerInfo) {
      try {
        if (consumerInfo.consumer) {
          consumerInfo.consumer.resume();
          console.log(`▶️ Consumer resumed for producer ${producerId}`);
          
          // Redux 상태 업데이트
          mediaTrackManager.updateRemoteTrackState(socketId, consumerInfo.kind, { enabled: true });
        }
      } catch (error) {
        console.error(`❌ Failed to resume consumer for producer ${producerId}:`, error);
      }
    }
  }


  // 로컬 트랙 토글
  public toggleLocalTrack(kind: "audio" | "video"): void {
    // 🆕 카메라 전용 메서드 사용 (화면 공유와 완전 분리)
    const track = mediaTrackManager.getLocalCameraTrack(kind);
    const trackInfo = mediaTrackManager.getLocalCameraTrackInfo(kind);

    if (track && trackInfo) {
      const newEnabled = !track.enabled;
      if (kind === "video" && this.cameraSourceTrack) this.cameraSourceTrack.enabled = newEnabled;
      mediaTrackManager.enableLocalTrack(trackInfo.trackId, newEnabled);
      console.log(`🔄 Local camera ${kind} ${newEnabled ? "enabled" : "disabled"}`);
    } else {
      console.warn(`⚠️ Local camera ${kind} track not found`);
    }
  }

  // 디바이스 변경
  public async changeDevice(kind: "audio" | "video", deviceId: string): Promise<void> {
    const resumeAI = kind === "video" && this.aiVideoActive;
    const config = this.activeAiConfig;
    if (kind === "video") await this.toggleAIDuringConference(false);
    const version = this.aiRequestVersion;
    const stream = await navigator.mediaDevices.getUserMedia({ [kind]: { deviceId: { exact: deviceId } } });
    const source = stream.getTracks()[0];
    try {
      if (version !== this.aiRequestVersion) throw abortError();
      const current = mediaTrackManager.getLocalCameraTrackInfo(kind);
      if (!current) throw new Error("교체할 미디어 트랙이 없습니다.");
      const output = kind === "video" ? source.clone() : source;
      output.enabled = current.track.enabled;
      try { await mediaTrackManager.replaceLocalTrack(current.trackId, output); }
      catch (error) { output.stop(); throw error; }
      if (kind === "video") {
        this.cameraSourceTrack?.stop();
        source.enabled = output.enabled;
        this.cameraSourceTrack = source;
      }
    } catch (error) {
      stream.getTracks().forEach(track => track.stop());
      throw error;
    }
    if (resumeAI && config) await this.toggleAIDuringConference(true, config);
  }

  // 정리
  /**
   * MediasoupManager와 관련된 모든 리소스를 정리합니다.
   * AI 시스템도 함께 정리합니다.
   */
  public cleanup(): void {
    console.log("🧹 Cleaning up MediaSoup...");

    this.aiRequestVersion++;
    frontendAiProcessor.cancelInitialization();
    this.aiVideoActive = false;
    this.activeAiConfig = null;
    this.cameraSourceTrack?.stop();
    this.cameraSourceTrack = null;
    this.aiQueue = Promise.resolve();

    // 🆕 ScreenShareManager 정리
    screenShareManager.cleanup();

    // 트랙 매니저 정리
    mediaTrackManager.cleanup();

    // AI 시스템 정리 호출
    cleanupAISystem();

    // Transport 정리
    if (this.sendTransport) {
      this.sendTransport.close();
      this.sendTransport = null;
    }
    if (this.recvTransport) {
      this.recvTransport.close();
      this.recvTransport = null;
    }
    
    // Transport 복구 상태 초기화
    this.transportRecoveryInProgress = false;
    this.lastTransportFailureTime = 0;

    // Device 정리
    this.device = null;

    // 로컬 미디어 상태 초기화
    this.isLocalMediaStarted = false;

    // Redux 상태 초기화
    if (this.dispatch) {
      this.dispatch(resetMediaState());
    }

    this.currentRoomId = "";
    this.dispatch = null;

    console.log("✅ MediaSoup cleanup completed");
  }

  // Transport 에러 감지
  private isTransportError(error: unknown): boolean {
    if (error instanceof Error) {
      const transportErrors = [
        'transport', 'connection', 'dtls', 'ice', 'failed to connect',
        'transport closed', 'transport failed', 'dtls failed'
      ];
      
      const errorMessage = error.message.toLowerCase();
      return transportErrors.some(keyword => errorMessage.includes(keyword));
    }
    return false;
  }

  // Transport 복구 로직
  private async handleTransportRecovery(): Promise<void> {
    // 이미 복구 중이거나 최근에 복구한 경우 스킵
    const now = Date.now();
    if (this.transportRecoveryInProgress || (now - this.lastTransportFailureTime) < this.transportRecoveryDelay) {
      console.log(`⚠️ Skipping transport recovery (in progress: ${this.transportRecoveryInProgress}, recent: ${(now - this.lastTransportFailureTime) < this.transportRecoveryDelay})`);
      return;
    }

    this.transportRecoveryInProgress = true;
    this.lastTransportFailureTime = now;

    try {
      console.log(`🔄 Starting transport recovery...`);
      
      // 1단계: 기존 transport 상태 체크
      const sendState = this.sendTransport?.connectionState;
      const recvState = this.recvTransport?.connectionState;
      
      console.log(`🔍 Transport states - Send: ${sendState}, Recv: ${recvState}`);
      
      // 2단계: 문제가 있는 transport 재연결
      if (recvState === 'failed' || recvState === 'disconnected' || recvState === 'closed') {
        console.log(`🔄 Recreating receive transport...`);
        await this.recreateReceiveTransport();
      }
      
      if (sendState === 'failed' || sendState === 'disconnected' || sendState === 'closed') {
        console.log(`🔄 Recreating send transport...`);
        await this.recreateSendTransport();
      }
      
      console.log(`✅ Transport recovery completed successfully`);
      
    } catch (recoveryError) {
      console.error(`❌ Transport recovery failed:`, recoveryError);
      const errorMessage = recoveryError instanceof Error ? recoveryError.message : String(recoveryError);
      throw new Error(`Transport recovery failed: ${errorMessage}`);
    } finally {
      this.transportRecoveryInProgress = false;
    }
  }

  // Receive transport 재생성
  private async recreateReceiveTransport(): Promise<void> {
    if (!this.device || !this.dispatch) {
      throw new Error("Device not initialized for transport recreation");
    }

    try {
      // 기존 transport 정리
      if (this.recvTransport) {
        this.recvTransport.close();
      }

      // 새 transport 생성
      const recvTransportData = await webrtcHandler.createConsumerTransport({ 
        roomId: this.currentRoomId 
      });
      
      this.recvTransport = this.device.createRecvTransport(recvTransportData);
      this.setupRecvTransportEvents();

      // MediaTrackManager에 새 transport 설정
      if (this.sendTransport) {
        mediaTrackManager.setTransports(this.sendTransport, this.recvTransport, this.currentRoomId);
      }

      console.log(`✅ Receive transport recreated successfully`);
      
    } catch (error) {
      console.error(`❌ Failed to recreate receive transport:`, error);
      throw error;
    }
  }

  // Send transport 재생성
  private async recreateSendTransport(): Promise<void> {
    if (!this.device || !this.dispatch) {
      throw new Error("Device not initialized for transport recreation");
    }

    try {
      // 기존 transport 정리
      if (this.sendTransport) {
        this.sendTransport.close();
      }

      // 새 transport 생성
      const sendTransportData = await webrtcHandler.createProducerTransport({ 
        roomId: this.currentRoomId 
      });
      
      this.sendTransport = this.device.createSendTransport(sendTransportData);
      this.setupSendTransportEvents(this.currentRoomId);

      // MediaTrackManager에 새 transport 설정
      if (this.recvTransport) {
        mediaTrackManager.setTransports(this.sendTransport, this.recvTransport, this.currentRoomId);
      }

      console.log(`✅ Send transport recreated successfully`);
      
    } catch (error) {
      console.error(`❌ Failed to recreate send transport:`, error);
      throw error;
    }
  }

  // Transport 상태 강제 검증 (필요시 호출)
  public async verifyAndRecoverTransports(): Promise<void> {
    if (!this.sendTransport || !this.recvTransport) {
      throw new Error("Transports not initialized");
    }

    const sendState = this.sendTransport.connectionState;
    const recvState = this.recvTransport.connectionState;
    
    console.log(`🔍 Transport verification - Send: ${sendState}, Recv: ${recvState}`);
    
    if (sendState === 'failed' || recvState === 'failed' || 
        sendState === 'disconnected' || recvState === 'disconnected') {
      console.log(`⚠️ Transport verification failed, initiating recovery...`);
      await this.handleTransportRecovery();
    } else {
      console.log(`✅ Transport verification passed`);
    }
  }

  // Send Transport 이벤트 설정
  private setupSendTransportEvents(roomId: string): void {
    if (!this.sendTransport) return;

    this.sendTransport.on("connect", async ({ dtlsParameters }, callback, errback) => {
      try {
        await webrtcHandler.connectTransport({
          transportId: this.sendTransport!.id,
          dtlsParameters,
        });
        callback();
        console.log("✅ Send transport connected");
      } catch (error) {
        console.error("❌ Send transport connect failed:", error);
        errback(error as Error);
      }
    });

    this.sendTransport.on(
      "produce",
      async ({ kind, rtpParameters, appData }, callback, errback) => {
        try {
          const { id } = await webrtcHandler.produce({
            transportId: this.sendTransport!.id,
            kind,
            rtpParameters,
            roomId,
            appData: appData as unknown as ProducerAppData | undefined, // 🆕 appData 전달 (화면 공유 정보 포함)
          });
          callback({ id });
          console.log(
            `✅ Producer created: ${id} (${kind})${
              appData?.type === "screen" ? " [SCREEN SHARE]" : ""
            }`
          );
        } catch (error) {
          console.error("❌ Produce failed:", error);
          errback(error as Error);
        }
      }
    );

    this.sendTransport.on("connectionstatechange", (state) => {
      console.log(`🔗 Send transport state: ${state}`);

      if (this.dispatch) {
        const functionallyConnected = state === "connected" || state === "failed";
        this.dispatch(setTransportConnected(functionallyConnected));
      }

      if (state === "failed") {
        console.warn("⚠️ Send transport state is failed, but may still be functional");
        this.sendTransport?.getStats().then((stats) => {
          console.log("Send transport stats:", stats);
        });
      }
    });

    // ICE gathering state 변경 추적
    this.sendTransport.on("icegatheringstatechange", (iceState) => {
      console.log(`🧊 Send transport ICE gathering state: ${iceState}`);
    });

    // ICE candidate error 추적
    this.sendTransport.on("icecandidateerror", (error) => {
      console.error(`❌ Send transport ICE candidate error:`, error);
    });
  }

  // Recv Transport 이벤트 설정
  private setupRecvTransportEvents(): void {
    if (!this.recvTransport) return;

    this.recvTransport.on("connect", async ({ dtlsParameters }, callback, errback) => {
      try {
        await webrtcHandler.connectTransport({
          transportId: this.recvTransport!.id,
          dtlsParameters,
        });
        callback();
        console.log("✅ Recv transport connected");
      } catch (error) {
        console.error("❌ Recv transport connect failed:", error);
        errback(error as Error);
      }
    });

    this.recvTransport.on("connectionstatechange", (state) => {
      console.log(`🔗 Recv transport state: ${state}`);

      if (this.dispatch && this.sendTransport) {
        const bothConnected =
          (state === "connected" && this.sendTransport.connectionState === "connected") ||
          (this.sendTransport.connectionState === "connected" && state === "connected");
        this.dispatch(setTransportConnected(bothConnected));
      }

      if (state === "failed") {
        console.error("❌ Recv transport connection failed");
        this.recvTransport?.getStats().then((stats) => {
          console.log("Recv transport stats:", stats);
        });
      }
    });

    // ICE gathering state 변경 추적
    this.recvTransport.on("icegatheringstatechange", (iceState) => {
      console.log(`🧊 Recv transport ICE gathering state: ${iceState}`);
    });

    // ICE candidate error 추적
    this.recvTransport.on("icecandidateerror", (error) => {
      console.error(`❌ Recv transport ICE candidate error:`, error);
    });
  }

  // Getters
  public getDevice(): Device | null {
    return this.device;
  }

  public isDeviceLoaded(): boolean {
    return this.device?.loaded ?? false;
  }

  // 로컬 미디어 시작 여부 확인
  public getIsLocalMediaStarted(): boolean {
    return this.isLocalMediaStarted;
  }

  // [신규] Producer ID를 받아 종료하는 범용 메서드
  public async stopProduction(producerId: string): Promise<void> {
    // 서버에 `close_producer` 이벤트를 전송
    await webrtcHandler.closeProducer({ producerId });
    // 로컬 Producer 객체는 mediaTrackManager에서 관리하므로 여기선 서버에 알리기만 함
    console.log(`Sent close_producer for ${producerId} to server.`);
  }
}

export const mediasoupManager = new MediasoupManager();
