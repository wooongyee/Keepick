const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const root = path.resolve(__dirname, '..');
const deferred = () => { let resolve, reject; const promise = new Promise((a,b)=>{resolve=a;reject=b}); return {promise,resolve,reject}; };
const tick = () => new Promise(setImmediate);
const config = (extra={}) => ({gesture:{static:{enabled:true,confidence:.7},dynamic:{enabled:true,confidence:.7}},emotion:{enabled:false,confidence:.5},beauty:{enabled:false},...extra});
function fixture() {
  const state = { models:[], images:[], actions:[], frames:new Map(), draws:0, captures:0, devices:0, gates:[], playGate:null, hangInference:false, backend:[] };
  let id=0;
  class Track {
    constructor(kind='video',label='camera') { this.id=String(++id);this.kind=kind;this.label=label;this.enabled=true;this.readyState='live';this.stops=0;state.tracks.push(this); }
    clone(){ const clone=new Track(this.kind,this.label);clone.parent=this;return clone; }
    stop(){this.stops++;this.readyState='ended';}
    getSettings(){return {width:640,height:360};}
  }
  state.tracks=[];
  class Stream {constructor(tracks=[]){this.tracks=tracks;}getTracks(){return this.tracks;}getVideoTracks(){return this.tracks.filter(t=>t.kind==='video');}getAudioTracks(){return this.tracks.filter(t=>t.kind==='audio');}}
  const ctx={drawImage(){state.draws++},getImageData(){return {width:640,height:360,data:new Uint8ClampedArray(4)}},putImageData(){},save(){},restore(){},translate(){},scale(){},globalAlpha:1};
  class Video {constructor(){this.readyState=4;this.videoWidth=640;this.videoHeight=360;this.paused=false;} async play(){if(state.playGate)await state.playGate.promise;this.paused=false;}pause(){this.paused=true;}}
  class Canvas {getContext(){return ctx;}captureStream(){return new Stream([new Track('video','canvas')]);}}
  class Image {set src(v){this.url=v;if(v){state.images.push(v);queueMicrotask(()=>this.onload?.());}}}
  function processor(kind) {return class {
    constructor(){this.kind=kind;this.alive=false;this.closes=0;state.models.push(this);}
    async init(){this.loads=(this.loads||0)+1;const gate=state.gates.shift();if(gate)await gate.promise;this.alive=true;}
    cleanup(){this.alive=false;this.closes++;}
    updateConfig(){}
    async detectGestures(){if(state.hangInference)await new Promise(()=>{});return {type:'gesture',static:{label:'none',confidence:0},dynamic:{label:'none',confidence:0},timestamp:0};}
    async detectEmotion(){return null;}
    async applyFilters(x){return x;}
  };}
  const actions={setAiRuntime:payload=>({type:'ai/runtime',payload}),setAiEnabled:payload=>({type:'ai/enabled',payload})};
  const tf={async setBackend(name){state.backend.push(name);if(state.failWebGL&&name==='webgl')throw Error('WebGL unavailable');return !(state.rejectWebGL&&name==='webgl');},async ready(){}};
  const overrides={
    './emotionCaptureManager':{emotionCaptureManager:{init(){},cleanup(){}}},
    './emotionFaceProcessor':{EmotionFaceProcessor:processor('emotion')},
    './beautyFilterProcessor':{BeautyFilterProcessor:processor('beauty')},
    './gestureProcessor':{GestureProcessor:processor('gesture')},
    './faceMeshLoader':{FACE_MESH_ASSETS:'https://example.invalid/face'},
    '@tensorflow/tfjs':tf,
    '@/entities/video-conference/ai/model/aiSlice':actions,
  };
  const cache=new Map();
  const globals={console:{log(){},warn(){},error(){}},AbortController,DOMException,Error,Promise,Image,Uint8ClampedArray,MediaStream:Stream,queueMicrotask,
    setTimeout:(fn,ms)=>setTimeout(fn,Math.min(ms,40)),clearTimeout,
    performance:{now:()=>10000},window:{},
    document:{createElement:tag=>tag==='video'?new Video():new Canvas()},
    requestAnimationFrame:fn=>{const n=++id;state.frames.set(n,fn);return n;},cancelAnimationFrame:n=>state.frames.delete(n),
    navigator:{mediaDevices:{async getUserMedia(c){state.captures++;return new Stream([...(c.audio?[new Track('audio','microphone')]:[]),...(c.video?[new Track()]:[])]);}}},
  };
  function load(relative, extra={}) {
    const absolute=path.join(root,relative);
    if(cache.has(absolute))return cache.get(absolute);
    const code=ts.transpileModule(fs.readFileSync(absolute,'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
    const exports={};cache.set(absolute,exports);
    vm.runInNewContext(code,{...globals,exports,require:name=>{
      if(name in extra)return extra[name];
      if(name in overrides)return overrides[name];
      if(name.startsWith('.'))return load(path.relative(root,path.resolve(path.dirname(absolute),name+'.ts')));
      throw Error('Unexpected dependency '+name);
    }},{filename:relative});return exports;
  }
  const core=load('src/shared/api/ai/frontendAiProcessor.ts').frontendAiProcessor;
  const dispatch=a=>state.actions.push(a);
  let current;
  const media={init(){},cleanup(){current?.track.stop();current=null;},async addLocalTrack(track){if(track.kind==='video')current={trackId:track.id,track,producer:{}};},
    getLocalCameraTrackInfo:()=>current,getLocalCameraTrack:()=>current?.track,
    async replaceLocalTrack(_id,track){current.track.stop();current={...current,trackId:track.id,track};}};
  const mediaActions=new Proxy({}, {get:(_,name)=>payload=>({type:String(name),payload})});
  const manager=load('src/shared/api/mediasoupManager.ts',{
    'mediasoup-client':{Device:class {constructor(){state.devices++;}}},
    './mediaTrackManager':{mediaTrackManager:media},
    './screenShareManager':{screenShareManager:{cleanup(){},init(){}}},
    '@/entities/video-conference/media/model/mediaSlice':mediaActions,
    './socket':{webrtcHandler:{}},
    './ai':{frontendAiProcessor:core,initializeAISystem:(d,c)=>core.init(d,c),cleanupAISystem:()=>core.cleanup()},
  }).mediasoupManager;
  return {state,core,manager,dispatch,Track,media,get current(){return current;},step(){const entries=[...state.frames];state.frames.clear();entries.forEach(([,fn])=>fn());}};
}

test('core media init is idempotent and performs no AI I/O',async()=>{
 const f=fixture();await f.manager.init(f.dispatch);await f.manager.init(f.dispatch);
 assert.equal(f.state.devices,1);assert.equal(f.state.models.length,0);assert.equal(f.state.images.length,0);f.manager.cleanup();
});
test('concurrent identical AI requests load only requested modules once',async()=>{
 const f=fixture();await Promise.all([f.core.init(f.dispatch,config()),f.core.init(f.dispatch,config())]);
 assert.deepEqual(f.state.models.map(m=>m.kind),['gesture']);assert.equal(f.state.models[0].loads,1);
 assert.equal(f.state.images.some(s=>s.includes('/emotion/')),false);f.core.cleanup();
});
test('WebGL failure selects CPU explicitly',async()=>{
 const f=fixture();f.state.failWebGL=true;await f.core.init(f.dispatch,config());
 assert.deepEqual(f.state.backend,['webgl','cpu']);f.core.cleanup();
});
test('model failure rejects and exposes error instead of reporting ready',async()=>{
 const f=fixture(),gate=deferred();f.state.gates.push(gate);const task=f.core.init(f.dispatch,config());await tick();gate.reject(Error('model failed'));
 await assert.rejects(task,/model failed/);assert.equal(f.state.actions.at(-1).payload.status,'error');assert.equal(f.state.models.some(m=>m.alive),false);f.core.cleanup();
});
test('model timeout is bounded and late completion cannot resurrect resources',async()=>{
 const f=fixture(),gate=deferred();f.state.gates.push(gate);await assert.rejects(f.core.init(f.dispatch,config()),/초과/);
 gate.resolve();await tick();assert.equal(f.state.models.some(m=>m.alive),false);f.core.cleanup();
});
test('cancellation and cleanup cannot overwrite a newer initialized runtime',async()=>{
 const f=fixture(),gate=deferred();f.state.gates.push(gate);const old=f.core.init(f.dispatch,config());await tick();f.core.cleanup();
 await assert.rejects(old,e=>e.name==='AbortError');const next=f.core.init(f.dispatch,config({emotion:{enabled:true,confidence:.5}}));
 await tick();assert.equal(f.state.models.filter(m=>m.loads).length,1);
 gate.resolve();await next;assert.equal(f.state.actions.at(-1).payload.status,'ready');
 assert.equal(f.state.models[0].alive,false);assert.equal(f.state.models.filter(m=>m.alive).length,2);f.core.cleanup();
});
test('processing stop releases its clone/output/RAF and preserves the caller camera',async()=>{
 const f=fixture();await f.core.init(f.dispatch,config());const source=new f.Track(),output=await f.core.processVideoTrack(source);
 assert.equal(output.readyState,'live');assert.equal(f.state.frames.size,1);f.core.stopProcessing();
 assert.equal(source.readyState,'live');assert.equal(source.stops,0);assert.equal(output.readyState,'ended');assert.equal(f.state.frames.size,0);f.core.cleanup();
});
test('cancelled video startup cannot attach a late output',async()=>{
 const f=fixture();await f.core.init(f.dispatch,config());f.state.playGate=deferred();const source=new f.Track();const task=f.core.processVideoTrack(source);
 f.core.stopProcessing();await assert.rejects(task,e=>e.name==='AbortError');f.state.playGate.resolve();await tick();
 assert.equal(source.readyState,'live');assert.equal(f.state.frames.size,0);f.core.cleanup();
});
test('render frames continue while inference is pending',async()=>{
 const f=fixture();await f.core.init(f.dispatch,config());f.state.hangInference=true;const source=new f.Track();await f.core.processVideoTrack(source);
 for(let i=0;i<8;i++)f.step();assert.ok(f.state.draws>=9);f.core.cleanup();assert.equal(f.state.frames.size,0);
});
test('repeated conference AI toggles retain a single camera source without recapture',async()=>{
 const f=fixture();await f.manager.init(f.dispatch);await f.manager.startLocalMedia();const raw=f.current.track.parent;
 for(let i=0;i<3;i++){
  await f.manager.toggleAIDuringConference(true,config());assert.equal(f.current.track.label,'canvas');assert.equal(raw.readyState,'live');
  await f.manager.toggleAIDuringConference(false);assert.equal(f.current.track.label,'camera');assert.equal(raw.readyState,'live');
 }
 assert.equal(f.state.captures,1);f.manager.cleanup();assert.equal(raw.readyState,'ended');assert.equal(f.state.frames.size,0);
});
test('failed conference initialization leaves original media live',async()=>{
 const f=fixture();await f.manager.init(f.dispatch);await f.manager.startLocalMedia();const original=f.current.track,gate=deferred();f.state.gates.push(gate);
 const task=f.manager.toggleAIDuringConference(true,config());await tick();gate.reject(Error('unavailable'));
 await assert.rejects(task,/unavailable/);assert.equal(f.current.track,original);assert.equal(original.readyState,'live');f.manager.cleanup();
});
test('turning off during model loading cancels before any replacement',async()=>{
 const f=fixture();await f.manager.init(f.dispatch);await f.manager.startLocalMedia();const original=f.current.track,gate=deferred();f.state.gates.push(gate);
 const on=f.manager.toggleAIDuringConference(true,config());await tick();const off=f.manager.toggleAIDuringConference(false);await Promise.all([on,off]);
 gate.resolve();await tick();assert.equal(f.current.track,original);assert.equal(original.readyState,'live');assert.equal(f.state.frames.size,0);f.manager.cleanup();
});

test('inference timeout reports failure while raw rendering remains live',async()=>{
 const f=fixture();await f.core.init(f.dispatch,config());f.state.hangInference=true;const source=new f.Track(),failed=deferred();
 const output=await f.core.processVideoTrack(source,error=>failed.resolve(error));const error=await failed.promise;
 assert.match(error.message,/초과/);assert.equal(output.readyState,'live');assert.equal(source.readyState,'live');
 const before=f.state.draws;f.step();assert.ok(f.state.draws>before);f.core.cleanup();
});
test('changing the camera replaces the owned source before resuming AI',async()=>{
 const f=fixture();await f.manager.init(f.dispatch);await f.manager.startLocalMedia();const previous=f.current.track.parent;
 await f.manager.toggleAIDuringConference(true,config());await f.manager.changeDevice('video','new-camera');
 assert.equal(previous.readyState,'ended');assert.equal(f.current.track.label,'canvas');assert.equal(f.state.captures,2);
 await f.manager.toggleAIDuringConference(false);assert.equal(f.current.track.parent.readyState,'live');f.manager.cleanup();
});

test('legacy FaceMesh releases only its own global before Tasks Vision loads',async()=>{
 const code=ts.transpileModule(fs.readFileSync(path.join(root,'src/shared/api/ai/faceMeshLoader.ts'),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
 const globals={Module:{},createMediapipeSolutionsPackedAssets:{}};const exports={};
 vm.runInNewContext(code,{exports,window:globals,require:()=>({})});
 const own={};globals.createMediapipeSolutionsPackedAssets=own;
 await exports.initializeFaceMesh({async initialize(){globals.Module=own;}});assert.equal(globals.Module,undefined);
 const other={};await exports.initializeFaceMesh({async initialize(){globals.Module=other;}});assert.equal(globals.Module,other);
 await assert.rejects(exports.initializeFaceMesh({async initialize(){globals.Module=own;throw Error('failed');}}));assert.equal(globals.Module,undefined);
});

test('WebGL initialization returning false selects CPU as well',async()=>{
 const f=fixture();f.state.rejectWebGL=true;await f.core.init(f.dispatch,config());
 assert.deepEqual(f.state.backend,['webgl','cpu']);f.core.cleanup();
});
