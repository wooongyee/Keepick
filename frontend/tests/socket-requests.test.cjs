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
 const socket=new Socket();
 const manager=load('shared/api/socket/socketManager.ts',{'socket.io-client':{io:()=>socket},'@/shared/config':{SOCKET_SERVER_URL:'local'},'@/entities/video-conference/session/model/slice':{}}).socketManager;
 manager.init(()=>{},[]);return {socket,manager};
}
test('request timeout preserves long-lived producer-close listeners',async()=>{
 const {socket,manager}=fixture();let count=0;socket.on('producer_closed',()=>count++);
 await assert.rejects(manager.request('close_producer','producer_closed',{},25),/timed out/);
 socket.receive('producer_closed',{producerId:'remote'});assert.equal(count,1);assert.equal(socket.listenerCount('producer_closed'),1);
});
test('ack errors remove only the request-owned listener',async()=>{
 const {socket,manager}=fixture();const persistent=()=>{};socket.on('reply',persistent);
 const task=manager.request('operation','reply',{});socket.sent.at(-1)[2]({error:'failed'});
 await assert.rejects(task,/failed/);assert.deepEqual(socket.listeners('reply'),[persistent]);
});
test('concurrent close replies are correlated by producer ID',async()=>{
 const {socket,manager}=fixture();
 const a=manager.request('close_producer','producer_closed_success',{},100,r=>r.producerId==='a');
 const b=manager.request('close_producer','producer_closed_success',{},100,r=>r.producerId==='b');
 socket.receive('producer_closed_success',{producerId:'b'});socket.receive('producer_closed_success',{producerId:'a'});
 assert.equal((await a).producerId,'a');assert.equal((await b).producerId,'b');assert.equal(socket.listenerCount('producer_closed_success'),0);
});
