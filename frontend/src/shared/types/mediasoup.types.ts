import { Device, Producer } from "mediasoup-client/types";
import type {
  RtpCapabilities,
  RtpParameters,
  IceParameters,
  IceCandidate,
  DtlsParameters,
} from "mediasoup-client/types";

// ✅ 최신 버전에서는 Device 객체의 메서드들을 통해 타입을 추출해야 합니다
export interface MediasoupDevice {
  device: Device;
  rtpCapabilities?: RtpCapabilities; // FIX: any -> RtpCapabilities
}

// ✅ Transport 관련 - mediasoup-client의 내부 타입들 사용
export interface TransportOptions {
  id: string;
  iceParameters: IceParameters; // FIX: any -> IceParameters
  iceCandidates: IceCandidate[]; // FIX: any -> IceCandidate[]
  dtlsParameters: DtlsParameters; // FIX: any -> DtlsParameters
}

export interface ProducerInfo {
  id: string;
  kind: "audio" | "video"; // MediaKind는 단순 문자열 유니온
}

export interface ConsumerInfo {
  id: string;
  producerId: string;
  kind: "audio" | "video";
  rtpParameters: RtpParameters; // FIX: any -> RtpParameters
}

export interface PeerInfo {
  id: string;
  producers: Map<string, Producer>;
}

// ✅ 실제로 사용되는 이벤트 데이터 타입들
export interface JoinRoomData {
  room: string;
  rtpCapabilities: RtpCapabilities; // FIX: any -> RtpCapabilities
}

export interface ProduceData {
  transportId: string;
  kind: "audio" | "video";
  rtpParameters: RtpParameters; // FIX: any -> RtpParameters
  roomId: string;
}

export interface ConsumeData {
  transportId: string;
  producerId: string;
  rtpCapabilities: RtpCapabilities; // FIX: any -> RtpCapabilities
  roomId: string;
}

export interface TransportConnectData {
  transportId: string;
  dtlsParameters: DtlsParameters; // FIX: any -> DtlsParameters
}

// ✅ 서버에서 받는 응답 타입들
export interface TransportCreatedData {
  id: string;
  iceParameters: IceParameters; // FIX: any -> IceParameters
  iceCandidates: IceCandidate[]; // FIX: any -> IceCandidate[]
  dtlsParameters: DtlsParameters; // FIX: any -> DtlsParameters
}

export interface ProducerCreatedData {
  id: string;
}

export interface ConsumerCreatedData {
  id: string;
  producerId: string;
  kind: "audio" | "video";
  rtpParameters: RtpParameters; // FIX: any -> RtpParameters
}
