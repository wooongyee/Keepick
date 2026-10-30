const {test}=require('node:test');
const assert=require('node:assert/strict');
const {randomUUID}=require('node:crypto');
const {io}=require('socket.io-client');
const base=process.env.SIGNALING_URL||'http://127.0.0.1:18443';
const delay=ms=>new Promise(r=>setTimeout(r,ms));
function reply(socket,event,send){return new Promise((resolve,reject)=>{
 const timeout=setTimeout(()=>{socket.off(event,on);reject(Error('Timeout '+event));},5000);
 const on=data=>{clearTimeout(timeout);socket.off(event,on);resolve(data);};socket.on(event,on);send();
});}
async function client(t,room,name){const socket=io(base,{autoConnect:false,transports:['websocket'],reconnection:false,extraHeaders:{Origin:'http://localhost:3107'}});t.after(()=>socket.disconnect());await reply(socket,'connect',()=>socket.connect());if(room)await reply(socket,'joined_room',()=>socket.emit('join_room',{roomId:room,userName:name}));return socket;}
const reaction=(id=randomUUID())=>({id,emoji:'👍',aiType:'gesture',confidence:.9,duration:1000});
test('AI reactions are server-authored, echoed once and isolated to the joined room',async t=>{
 const room=randomUUID(),a=await client(t,room,'A'),b=await client(t,room,'B'),c=await client(t,randomUUID(),'C');let leaked=0;c.on('ai_reaction_received',()=>leaked++);
 const incoming=reply(b,'ai_reaction_received',()=>{});const own=await reply(a,'ai_reaction_received',()=>a.emit('ai_reaction',{roomId:room,userId:'forged',userName:'fake',reaction:{...reaction(),userId:'forged',timestamp:0,duration:999999}}));
 const remote=await incoming;assert.deepEqual(remote,own);assert.equal(own.userId,a.id);assert.equal(own.userName,'A');assert.equal(own.reaction.userId,a.id);assert.equal(own.reaction.duration,5000);assert.ok(Math.abs(Date.now()-own.reaction.timestamp)<1000);await delay(60);assert.equal(leaked,0);
});
test('AI rejects foreign rooms, nonmembers and malformed payloads',async t=>{
 const room=randomUUID(),a=await client(t,room,'A'),b=await client(t,room,'B'),outside=await client(t);let received=0;b.on('ai_reaction_received',()=>received++);
 outside.emit('ai_reaction',{roomId:room,reaction:reaction()});a.emit('ai_reaction',{roomId:'other',reaction:reaction()});
 for(const bad of [null,{}, {...reaction(),emoji:'not-an-emoji'}, {...reaction(),confidence:2}, {...reaction(),aiType:'manual'}])a.emit('ai_reaction',{roomId:room,reaction:bad});
 await delay(120);assert.equal(received,0);
});
test('AI deduplicates and bounds reaction frequency',async t=>{
 const room=randomUUID(),a=await client(t,room,'A'),b=await client(t,room,'B');const events=[];b.on('ai_reaction_received',e=>events.push(e));const first=reaction();
 await reply(a,'ai_reaction_received',()=>a.emit('ai_reaction',{roomId:room,reaction:first}));a.emit('ai_reaction',{roomId:room,reaction:first});a.emit('ai_reaction',{roomId:room,reaction:reaction()});await delay(240);
 a.emit('ai_reaction',{roomId:room,reaction:first});await reply(a,'ai_reaction_received',()=>a.emit('ai_reaction',{roomId:room,reaction:reaction()}));await delay(30);assert.equal(events.length,2);
});
test('legacy gesture compatibility also binds room and sender identity',async t=>{
 const room=randomUUID(),a=await client(t,room,'Legacy A'),b=await client(t,room,'Legacy B');let count=0;b.on('gesture_detected',()=>count++);
 a.emit('gesture_detect',null);a.emit('gesture_detect',{roomId:'other',gestureType:'static'});
 const result=await reply(b,'gesture_detected',()=>a.emit('gesture_detect',{roomId:room,userId:'forged',userName:'fake',gestureType:'static',timestamp:0}));
 assert.equal(result.userId,a.id);assert.equal(result.userName,'Legacy A');assert.ok(result.timestamp>0);await delay(50);assert.equal(count,1);
});
