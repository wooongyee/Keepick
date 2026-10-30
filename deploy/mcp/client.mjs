import fs from 'node:fs/promises';
import path from 'node:path';
import { Client } from '@modelcontextprotocol/sdk/client/index.js';
import { StreamableHTTPClientTransport } from '@modelcontextprotocol/sdk/client/streamableHttp.js';
const client=new Client({name:'keepick-cd',version:'1.0.0'});
function parse(result){if(result.isError)throw new Error(result.content?.find(c=>c.type==='text')?.text||'MCP tool failed');return JSON.parse(result.content.find(c=>c.type==='text').text);}
try {
  await client.connect(new StreamableHTTPClientTransport(new URL(process.env.KEEPICK_DEPLOY_MCP_URL),{requestInit:{headers:{Authorization:`Bearer ${process.env.KEEPICK_DEPLOY_TOKEN}`}}}));
  if(process.argv[2]==='status'){console.log(JSON.stringify(parse(await client.callTool({name:'deploy_status',arguments:{}})),null,2));}
  else {
    const revision=process.env.GITHUB_SHA,images={};
    for(const name of ['web','api','realtime','gateway']){
      const data=JSON.parse(await fs.readFile(path.join(process.env.KEEPICK_IMAGE_METADATA||'.',`${name}.json`),'utf8'));
      if(data.revision!==revision)throw new Error(`Wrong image metadata: ${name}`);
      images[name]=`ghcr.io/wooongyee/keepick-${name}@${data.digest}`;
    }
    console.log(JSON.stringify(parse(await client.callTool({name:'deploy_plan',arguments:{revision,images}})),null,2));
    const operation=parse(await client.callTool({name:'deploy_apply',arguments:{revision,images,registry_token:process.env.GHCR_TOKEN}}));
    if(operation.operationId!==revision)throw new Error('Wrong deployment operation');
    const deadline=Date.now()+20*60*1000;
    let last='';
    while(Date.now()<deadline){
      const state=parse(await client.callTool({name:'deploy_status',arguments:{}}));
      if(state.operation?.id!==revision)throw new Error('Deployment operation changed');
      if(state.operation.phase!==last){last=state.operation.phase;console.log(`Keepick deploy: ${last}`);}
      if(state.operation.status==='succeeded'){
        if(Object.entries(images).some(([name,image])=>state.operation.images?.[name]!==image))throw new Error('Deployed images differ from the requested release');
        console.log(JSON.stringify(state,null,2));break;
      }
      if(state.operation.status!=='running')throw new Error(state.operation.error||'Deployment failed');
      await new Promise(resolve=>setTimeout(resolve,5000));
    }
    const state=parse(await client.callTool({name:'deploy_status',arguments:{}}));if(state.operation?.status!=='succeeded')throw new Error('Deployment timed out');
  }
} finally {await client.close();}
