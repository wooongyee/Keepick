export interface User {
  id: string;
  name: string; // 사용자 이름 속성
  producers?: { producerId: string; kind: "audio" | "video" }[];
}

export interface JoinRoomData {
  room: string;
  email?: string;
}

export interface OfferData {
  sdp: RTCSessionDescriptionInit;
  senderID: string;
}

export interface AnswerData {
  sdp: RTCSessionDescriptionInit;
  senderID: string;
}

export interface CandidateData {
  candidate: RTCIceCandidateInit;
  senderID: string;
}

export interface UserExitData {
  id: string;
}

export interface ErrorData {
  message: string;
}
