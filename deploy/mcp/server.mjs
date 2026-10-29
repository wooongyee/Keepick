import express from 'express';
import { randomUUID,timingSafeEqual } from 'node:crypto';
import { McpServer } from '@modelcontextprotocol/sdk/server/mcp.js';
import { StreamableHTTPServerTransport } from '@modelcontextprotocol/sdk/server/streamableHttp.js';
import { isInitializeRequest } from '@modelcontextprotocol/sdk/types.js';
import { z } from 'zod';
import { Releases,components } from './release.mjs';

const token=process.env.KEEPICK_DEPLOY_TOKEN;
if(!token||token.length<32)throw new Error('KEEPICK_DEPLOY_TOKEN must be configured');
const releases=new Releases(process.env.KEEPICK_RELEASE_ROOT||'/home/ys050/workspace/keepick');await releases.initialize();
const app=express();app.disable('x-powered-by');app.use(express.json({limit:'64kb'}));
app.use((req,res,next)=>{
  if(req.path==='/healthz')return next();
  const actual=Buffer.from(req.get('authorization')||''),expected=Buffer.from(`Bearer ${token}`);
  if(actual.length!==expected.length||!timingSafeEqual(actual,expected))return res.status(401).json({error:'unauthorized'});
  next();
});
app.get('/healthz',(_req,res)=>res.json({ok:true}));
const sessions=new Map();
const schema={revision:z.string(),images:z.object(Object.fromEntries(components.map(name=>[name,z.string()])))};
const result=value=>({content:[{type:'text',text:JSON.stringify(value)}]});
function server(){
  const mcp=new McpServer({name:'keepick-deployer',version:'1.0.0'});
  mcp.tool('deploy_plan','Plan a Keepick master release without changing it',schema,async input=>result(await releases.plan(input)));
  mcp.tool('deploy_apply','Deploy approved Keepick master images and roll back on failed health', {...schema,registry_token:z.string()},async input=>result(await releases.apply(input)));
  mcp.tool('deploy_status','Read Keepick deployment state and container health',{},async()=>result(await releases.status()));
  return mcp;
}
app.post('/mcp',async(req,res)=>{
  try {
    const sid=req.get('mcp-session-id');let transport=sessions.get(sid);
    if(!transport&&!sid&&isInitializeRequest(req.body)){
      transport=new StreamableHTTPServerTransport({sessionIdGenerator:randomUUID,onsessioninitialized:id=>sessions.set(id,transport)});
      transport.onclose=()=>sessions.delete(transport.sessionId);await server().connect(transport);
    }
    if(!transport)return res.status(400).json({error:'invalid session'});
    await transport.handleRequest(req,res,req.body);
  } catch {if(!res.headersSent)res.status(500).json({error:'MCP request failed'});}
});
for(const method of ['get','delete'])app[method]('/mcp',async(req,res)=>{
  const transport=sessions.get(req.get('mcp-session-id'));if(!transport)return res.status(404).end();await transport.handleRequest(req,res);
});
app.listen(Number(process.env.PORT||4200),'0.0.0.0',()=>console.log('Keepick deployment MCP ready'));
