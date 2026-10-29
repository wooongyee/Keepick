const { test } = require("node:test");
const assert = require("node:assert/strict");
const { randomUUID } = require("node:crypto");
const { io } = require("socket.io-client");

const base = process.env.SIGNALING_URL ?? "http://127.0.0.1:18443";
const origin = "http://127.0.0.1:3108";
function reply(socket, event, send) {
  return new Promise((resolve, reject) => {
    const finish = (error, data) => {
      clearTimeout(timer);
      socket.off(event, received);
      socket.off("connect_error", failed);
      error ? reject(error) : resolve(data);
    };
    const received = (data) => finish(null, data);
    const failed = (error) => finish(error);
    const timer = setTimeout(() => finish(new Error("Timeout: " + event)), 5000);
    socket.once(event, received);
    socket.once("connect_error", failed);
    send();
  });
}
async function client(t, requestOrigin = origin) {
  const socket = io(base, {
    autoConnect: false, transports: ["websocket"], reconnection: false,
    extraHeaders: { Origin: requestOrigin },
  });
  t.after(() => socket.disconnect());
  await reply(socket, "connect", () => socket.connect());
  return socket;
}
const join = (socket, roomId, userName) =>
  reply(socket, "joined_room", () => socket.emit("join_room", { roomId, userName }));
const roomInfo = (socket, roomId) =>
  reply(socket, "room_info", () => socket.emit("get_room_info", { roomId }));

test("worker health and allowed/blocked HTTP origins", async () => {
  const health = await fetch(base + "/health", { headers: { Origin: origin } });
  assert.equal(health.status, 200);
  assert.deepEqual(await health.json(), { service: "keepick-realtime", ready: true });
  assert.equal(health.headers.get("access-control-allow-origin"), origin);
  for (const allowed of [origin, "http://localhost:3107", "http://127.0.0.1:3107"]) {
    const preflight = await fetch(base + "/socket.io/?EIO=4&transport=polling", {
      method: "OPTIONS", headers: { Origin: allowed, "Access-Control-Request-Method": "GET" },
    });
    assert.equal(preflight.status, 204);
    assert.equal(preflight.headers.get("access-control-allow-origin"), allowed);
    assert.equal(preflight.headers.get("access-control-allow-credentials"), "true");
  }
  const blocked = await fetch(base + "/health", { headers: { Origin: "https://blocked.invalid" } });
  assert.equal(blocked.headers.get("access-control-allow-origin"), null);
});

test("disallowed browser origin cannot open a WebSocket", async (t) => {
  const socket = io(base, { autoConnect: false, transports: ["websocket"],
    reconnection: false, extraHeaders: { Origin: "https://blocked.invalid" } });
  t.after(() => socket.disconnect());
  await assert.rejects(reply(socket, "connect", () => socket.connect()), /websocket error/i);
  assert.equal(socket.connected, false);
});

test("A/B join the same real mediasoup room and receive presence", async (t) => {
  const roomId = "signaling-" + randomUUID();
  const a = await client(t, "http://localhost:3107"), b = await client(t);
  const first = await join(a, roomId, "Client A");
  assert.equal(first.peers.length, 0);
  assert.ok(first.rtpCapabilities.codecs.length > 0);
  const noticed = reply(a, "user_joined", () => {});
  const second = await join(b, roomId, "Client B");
  assert.deepEqual(second.peers.map(p => p.id), [a.id]);
  assert.equal(second.peers[0].name, "Client A");
  assert.equal(second.chatSessionId, first.chatSessionId);
  assert.deepEqual(await noticed, { id: b.id, name: "Client B" });
  assert.equal((await roomInfo(a, roomId)).peersCount, 2);
  const transport = await reply(a, "producer_transport_created",
    () => a.emit("create_producer_transport", { roomId }));
  assert.ok(transport.iceCandidates.length > 0);
  for (const candidate of transport.iceCandidates) {
    assert.equal(candidate.ip, "127.0.0.1");
    assert.ok(candidate.port >= 10000 && candidate.port <= 10100);
  }
  const left = reply(a, "user_left", () => {});
  await reply(b, "left_room", () => b.emit("leave_room"));
  assert.equal((await left).id, b.id);
  assert.equal((await roomInfo(a, roomId)).peersCount, 1);
  await reply(a, "left_room", () => a.emit("leave_room"));
  assert.equal((await fetch(base + "/rooms/" + roomId)).status, 404);
});

test("simultaneous first joins share one room/router", async (t) => {
  const roomId = "signaling-race-" + randomUUID();
  const a = await client(t), b = await client(t);
  const [first, second] = await Promise.all([join(a, roomId, "Concurrent A"), join(b, roomId, "Concurrent B")]);
  assert.equal(first.chatSessionId, second.chatSessionId);
  const info = await roomInfo(a, roomId);
  assert.equal(info.peersCount, 2);
  assert.deepEqual(new Set(info.peers.map(p => p.id)), new Set([a.id, b.id]));
});

test("a peer that left receives no later room presence event", async (t) => {
  const roomId = "signaling-leave-" + randomUUID();
  const a = await client(t), b = await client(t);
  await join(a, roomId, "Leaving A");
  await join(b, roomId, "Remaining B");
  await reply(a, "left_room", () => a.emit("leave_room"));
  const c = await client(t);
  let leaked = 0;
  a.on("user_joined", () => leaked++);
  await join(c, roomId, "New C");
  // A's message queue must have reached the server before we assert silence.
  await roomInfo(a, roomId);
  await new Promise(resolve => setTimeout(resolve, 100));
  assert.equal(leaked, 0);
});

test("disconnect removes the participant and notifies the remaining peer", async (t) => {
  const roomId = "signaling-disconnect-" + randomUUID();
  const a = await client(t), b = await client(t);
  await join(a, roomId, "Observer A");
  await join(b, roomId, "Disconnecting B");
  const bId = b.id;
  const left = await reply(a, "user_left", () => b.disconnect());
  assert.equal(left.id, bId);
  assert.equal((await roomInfo(a, roomId)).peersCount, 1);
});

test("leaving producer removes its consumers from the remaining peer", async (t) => {
  const roomId = "media-cleanup-" + randomUUID();
  const a = await client(t), b = await client(t);
  const capabilities = await join(a, roomId, "Sender A");
  await join(b, roomId, "Receiver B");
  const send = await reply(a, "producer_transport_created",
    () => a.emit("create_producer_transport", { roomId }));
  const receive = await reply(b, "consumer_transport_created",
    () => b.emit("create_consumer_transport", { roomId }));
  const producer = await reply(a, "producer_created", () => a.emit("produce", {
    roomId, transportId: send.id, kind: "audio",
    rtpParameters: {
      codecs: [{ mimeType: "audio/opus", payloadType: 111, clockRate: 48000, channels: 2 }],
      encodings: [{ ssrc: 12345678 }], rtcp: { cname: "media-cleanup-test" },
    },
  }));
  await reply(b, "consumer_created", () => b.emit("consume", {
    roomId, transportId: receive.id, producerId: producer.id,
    rtpCapabilities: capabilities.rtpCapabilities,
  }));
  assert.equal((await roomInfo(b, roomId)).peers.find(p => p.id === b.id).consumersCount, 1);
  await reply(a, "left_room", () => a.emit("leave_room"));
  // The worker delivers producer-close asynchronously to the consumer.
  let remaining;
  for (let attempt = 0; attempt < 20; attempt++) {
    remaining = (await roomInfo(b, roomId)).peers.find(p => p.id === b.id).consumersCount;
    if (remaining === 0) break;
    await new Promise(resolve => setTimeout(resolve, 25));
  }
  assert.equal(remaining, 0);
});

test('closing a screen producer acknowledges the sender and notifies the receiver while camera stays',async t=>{
 const room=randomUUID(),a=await client(t),b=await client(t);await join(a,room,'A');await join(b,room,'B');
 const transport=await reply(a,'producer_transport_created',()=>a.emit('create_producer_transport',{roomId:room}));
 const produce=(ssrc,type)=>reply(a,'producer_created',()=>a.emit('produce',{roomId:room,transportId:transport.id,kind:'video',rtpParameters:{codecs:[{mimeType:'video/VP8',payloadType:96,clockRate:90000}],encodings:[{ssrc}],rtcp:{cname:'screen-test'}},appData:{type,peerId:a.id,peerName:'A'}}));
 const camera=await produce(1234561,'camera'),screen=await produce(1234562,'screen');
 const noticed=reply(b,'producer_closed',()=>{});const ack=await reply(a,'producer_closed_success',()=>a.emit('close_producer',{producerId:screen.id}));
 assert.equal(ack.producerId,screen.id);assert.equal((await noticed).producerId,screen.id);
 const info=await reply(a,'room_info',()=>a.emit('get_room_info',{roomId:room}));assert.equal(info.peers.find(p=>p.id===a.id).producersCount,1);
 const last=await reply(a,'producer_closed_success',()=>a.emit('close_producer',{producerId:camera.id}));assert.equal(last.producerId,camera.id);
});
