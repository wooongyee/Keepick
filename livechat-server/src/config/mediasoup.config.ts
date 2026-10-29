import { RtpCodecCapability } from "mediasoup/node/lib/rtpParametersTypes";
import { portFromEnv } from "./env";

const rtcMinPort = portFromEnv("MEDIASOUP_MIN_PORT", 10000);
const rtcMaxPort = portFromEnv("MEDIASOUP_MAX_PORT", 10100);
if (rtcMaxPort < rtcMinPort) {
  throw new Error("MEDIASOUP_MAX_PORT must be at least MEDIASOUP_MIN_PORT");
}

export const mediasoupConfig = {
  worker: {
    logLevel: "warn" as const,
    rtcMinPort,
    rtcMaxPort,
  },
  webRtcTransport: {
    listenIps: [{
      ip: process.env.MEDIASOUP_LISTEN_IP ?? "127.0.0.1",
      announcedIp: process.env.MEDIASOUP_ANNOUNCED_IP ?? "127.0.0.1",
    }],
    enableUdp: true,
    enableTcp: true,
    preferUdp: true,
  },
  mediaCodecs: [
    {
      kind: "audio",
      mimeType: "audio/opus",
      clockRate: 48000,
      channels: 2,
    },
    {
      kind: "video",
      mimeType: "video/VP8",
      clockRate: 90000,
      parameters: {
        "x-google-start-bitrate": 1000,
      },
    },
    {
      kind: "video",
      mimeType: "video/VP9",
      clockRate: 90000,
      parameters: {
        "profile-id": 2,
        "x-google-start-bitrate": 1000,
      },
    },
    {
      kind: "video",
      mimeType: "video/h264",
      clockRate: 90000,
      parameters: {
        "packetization-mode": 1,
        "profile-level-id": "4d0032",
        "level-asymmetry-allowed": 1,
        "x-google-start-bitrate": 1000,
      },
    },
  ] as RtpCodecCapability[],
};
