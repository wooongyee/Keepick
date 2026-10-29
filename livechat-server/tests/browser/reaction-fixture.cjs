// Native Socket.IO test peer; sends a deterministic AI result, not recognition.
const {io}=require('socket.io-client');
const socket=io(process.env.SIGNALING_URL||'http://127.0.0.1:18444',{transports:['websocket'],reconnection:false,extraHeaders:{Origin:'http://localhost:3110'}});
const roomId=process.env.REACTION_TEST_ROOM||'step4-browser';
socket.on('connect',()=>socket.emit('join_room',{roomId,userName:'Reaction Fixture'}));
socket.once('joined_room',()=>setTimeout(()=>socket.emit('ai_reaction',{roomId,reaction:{id:'ui-fixture-'+Date.now(),emoji:'👍',aiType:'gesture',confidence:.99,duration:3500}}),1200));
socket.once('ai_reaction_received',data=>process.stdout.write(JSON.stringify(data)+'\n'));
const cleanup=()=>{socket.disconnect();process.exit(0);};process.on('SIGTERM',cleanup);process.on('SIGINT',cleanup);setTimeout(cleanup,15000);
