const {test}=require('node:test');
const assert=require('node:assert/strict');
const {EventEmitter}=require('node:events');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),ts=require('typescript');
class Socket extends EventEmitter {
 constructor(){super();this.id='socket-a';this.connected=true;this.sent=[];}
 emit(...args){this.sent.push(args);return true;}
 receive(...args){return super.emit(...args);}
}
function load(file,modules){
 const code=ts.transpileModule(fs.readFileSync(path.join(__dirname,'../src',file),'utf8'),{compilerOptions:{module:ts.ModuleKind.CommonJS,target:ts.ScriptTarget.ES2022}}).outputText;
 const exports={};vm.runInNewContext(code,{exports,require:name=>{if(!(name in modules))throw Error(name);return modules[name];},setTimeout,clearTimeout,console},{filename:file});return exports;
}
function fixture(){
 const socket=new Socket(),actions=[];
 const manager=load('shared/api/socket/socketManager.ts',{'socket.io-client':{io:()=>socket},'@/shared/config':{SOCKET_SERVER_URL:'local'},'@/entities/video-conference/session/model/slice':{}}).socketManager;
 manager.init(a=>actions.push(a),[]);
 const handler=load('shared/api/socket/aiEventHandler.ts',{'@/entities/emoji-reaction/model/slice':{addReaction:payload=>({type:'add',payload}),clearAllReactions:()=>({type:'clear'})}}).aiEventHandler;
 const dispatch=a=>actions.push(a);
 return {socket,manager,handler,actions,dispatch};
}
const event=(userId='socket-b',id='unique')=>({roomId:'room',userId,userName:'B',reaction:{id,emoji:'👍',userId,userName:'B',timestamp:100,duration:2000,source:'ai',aiType:'gesture',confidence:.9}});
test('AI initialization is idempotent and sends one identity-free contract',()=>{
 const {socket,handler,actions,dispatch}=fixture();handler.init(socket,dispatch);handler.init(socket,dispatch);handler.setRoomId('room');
 assert.equal(socket.listenerCount('ai_reaction_received'),1);handler.sendAiReaction(event().reaction);
 const [name,data]=socket.sent.at(-1);assert.equal(name,'ai_reaction');assert.equal(data.roomId,'room');assert.equal('userId' in data.reaction,false);assert.equal('timestamp' in data.reaction,false);assert.equal(actions.filter(a=>a.type==='add').length,0);
});
test('AI receives local echo and remote identity once, ignoring other rooms',()=>{
 const {socket,handler,actions,dispatch}=fixture();handler.init(socket,dispatch);handler.setRoomId('room');
 socket.receive('ai_reaction_received',event());socket.receive('ai_reaction_received',event());socket.receive('ai_reaction_received',event('socket-a','local-id'));socket.receive('ai_reaction_received',{...event('other','foreign'),roomId:'other'});
 const reactions=actions.filter(a=>a.type==='add');assert.equal(reactions.length,2);assert.equal(reactions[0].payload.userId,'socket-b');assert.equal(reactions[1].payload.userId,'local');
});
test('AI cleanup and reentry preserve other handlers without duplicate dispatch',()=>{
 const {socket,handler,actions,dispatch}=fixture();const core=()=>{};socket.on('connect',core);socket.on('gesture_detected',core);
 handler.init(socket,dispatch);handler.setRoomId('room');handler.cleanup();assert.equal(socket.listenerCount('ai_reaction_received'),0);assert.equal(socket.listenerCount('connect'),2);assert.equal(socket.listenerCount('gesture_detected'),1);
 handler.init(socket,dispatch);handler.setRoomId('room');socket.receive('ai_reaction_received',event());assert.equal(actions.filter(a=>a.type==='add').length,1);handler.cleanup();
});

test('an expired old reaction cannot erase a newer reaction',()=>{
 const slice=load('entities/emoji-reaction/model/slice.ts',{'@reduxjs/toolkit':require('@reduxjs/toolkit')});
 let state=slice.emojiReactionReducer(undefined,{type:'init'});
 state=slice.emojiReactionReducer(state,slice.addReaction(event('socket-b','new').reaction));
 state=slice.emojiReactionReducer(state,slice.expireReaction({userId:'socket-b',id:'old'}));assert.equal(state.activeReactions['socket-b'].id,'new');
 state=slice.emojiReactionReducer(state,slice.expireReaction({userId:'socket-b',id:'new'}));assert.equal(state.activeReactions['socket-b'],undefined);
});
