import { spawn } from 'node:child_process';
import fs from 'node:fs/promises';
import path from 'node:path';
import os from 'node:os';

export const components = ['web','api','realtime','gateway'];
const repository = 'wooongyee/Keepick';
export function validateRelease(input) {
  if (!/^[a-f0-9]{40}$/.test(input.revision)) throw new Error('A full commit SHA is required');
  for (const name of components) {
    if (!new RegExp(`^ghcr\\.io/wooongyee/keepick-${name}@sha256:[a-f0-9]{64}$`).test(input.images?.[name] || '')) throw new Error(`Invalid ${name} image digest`);
  }
  return { revision: input.revision, images: Object.fromEntries(components.map(name=>[name,input.images[name]])) };
}
async function command(args, {cwd,env,stdin,timeout=600000}={}) {
  return await new Promise((resolve,reject)=>{
    const child=spawn('docker',args,{cwd,env:{...process.env,...env},stdio:['pipe','pipe','pipe']});let output='';
    const timer=setTimeout(()=>child.kill('SIGTERM'),timeout);
    const collect=data=>{output=(output+data.toString()).slice(-16000);};child.stdout.on('data',collect);child.stderr.on('data',collect);
    child.on('error',error=>{clearTimeout(timer);reject(error);});
    child.on('close',code=>{clearTimeout(timer);code===0?resolve(output):reject(new Error(`Docker command failed (${code}): ${output.slice(-3000)}`));});
    if(stdin)child.stdin.write(stdin);child.stdin.end();
  });
}
export class Releases {
  constructor(root, {runCommand=command, request=fetch}={}) {this.root=root;this.busy=false;this.job=null;this.command=runCommand;this.fetch=request;}
  async initialize() {
    await fs.mkdir(path.join(this.root,'releases'),{recursive:true,mode:0o700});
    try {this.job=JSON.parse(await fs.readFile(path.join(this.root,'operation.json'),'utf8'));if(this.job.status==='running'){this.job.status='interrupted';this.job.error='Deployer restarted during the operation';await this.save();}} catch(error){if(error.code!=='ENOENT')throw error;}
  }
  async save() {await fs.writeFile(path.join(this.root,'operation.json'),JSON.stringify(this.job,null,2),{mode:0o600});}
  async current() {try{return await fs.readlink(path.join(this.root,'current'));}catch(error){if(error.code==='ENOENT')return null;throw error;}}
  async status() {
    const containers=await this.command(['ps','-a','--filter','label=com.docker.compose.project=keepick-homeserver','--format','{{json .}}']);
    return {operation:this.job,current:await this.current(),containers:containers.trim().split('\n').filter(Boolean).map(line=>JSON.parse(line))};
  }
  async plan(input) {const release=validateRelease(input);return {...release,previous:await this.current(),services:['mysql','redis','api','realtime','web','gateway'],data:'Keepick named volumes are retained',rollback:'Restore previous release and restart its images if health fails'};}
  async apply(input) {
    if(this.busy)throw new Error('A Keepick deployment is already running');
    validateRelease(input);
    if(typeof input.registry_token!=='string'||!input.registry_token)throw new Error('An ephemeral registry token is required');
    this.busy=true;this.job={id:input.revision,status:'running',phase:'verify',startedAt:new Date().toISOString(),revision:input.revision};
    try {await this.save();} catch(error){this.busy=false;throw error;}
    void this.run(input).catch(async error=>{this.job={...this.job,status:'failed',error:error.message.replaceAll(input.registry_token,'[REDACTED]'),finishedAt:new Date().toISOString()};await this.save();}).finally(()=>{this.busy=false;});
    return {operationId:this.job.id,status:this.job.status};
  }
  async phase(phase){this.job.phase=phase;await this.save();}
  async setCurrent(target) {
    const temp=path.join(this.root,`.current-${process.pid}`);await fs.rm(temp,{force:true});await fs.symlink(target,temp);await fs.rename(temp,path.join(this.root,'current'));
  }
  async run(input) {
    const release=validateRelease(input),previous=await this.current();
    const directory=path.join(this.root,'releases',release.revision);
    const credentials=await fs.mkdtemp(path.join(os.tmpdir(),'keepick-registry-'));
    const env={DOCKER_CONFIG:credentials};let changed=false;
    const githubHeaders={Accept:'application/vnd.github+json',Authorization:`Bearer ${input.registry_token}`,'X-GitHub-Api-Version':'2022-11-28'};
    try {
      const head=await this.fetch(`https://api.github.com/repos/${repository}/git/ref/heads/master`,{headers:githubHeaders,signal:AbortSignal.timeout(15000)});
      if(!head.ok||(await head.json()).object.sha!==release.revision)throw new Error('Deployment must use the current master commit');
      await this.phase('prepare');await fs.mkdir(directory,{recursive:true,mode:0o700});
      for(const file of ['compose.homeserver.yml','exec/sql/ddl.sql']) {
        const response=await this.fetch(`https://raw.githubusercontent.com/${repository}/${release.revision}/${file}`,{signal:AbortSignal.timeout(30000)});
        if(!response.ok)throw new Error(`Cannot fetch release file: ${file}`);
        const text=await response.text();if(text.length>1000000)throw new Error('Release configuration is too large');
        const destination=path.join(directory,file);await fs.mkdir(path.dirname(destination),{recursive:true});await fs.writeFile(destination,text);
      }
      const shared=await fs.readFile(path.join(this.root,'shared/runtime.env'),'utf8');
      const images=components.map(name=>`IMAGE_${name.toUpperCase()}=${release.images[name]}`).join('\n');
      await fs.writeFile(path.join(directory,'.env'),shared+'\n'+images+'\n',{mode:0o600});
      await this.command(['login','ghcr.io','-u','wooongyee','--password-stdin'],{env,stdin:input.registry_token});
      await this.phase('pull');
      for(const name of components) {
        await this.command(['pull',release.images[name]],{env});
        const metadata=JSON.parse(await this.command(['image','inspect',release.images[name]],{env}))[0];
        if(metadata.Config.Labels?.['org.opencontainers.image.revision']!==release.revision)throw new Error(`Image revision mismatch: ${name}`);
      }
      await this.command(['compose','--env-file','.env','-f','compose.homeserver.yml','config','--quiet'],{cwd:directory,env});
      await this.phase('activate');await this.setCurrent(directory);changed=true;
      await this.command(['compose','--env-file','.env','-f','compose.homeserver.yml','up','-d','--no-build','--wait','--wait-timeout','240'],{cwd:directory,env,timeout:360000});
      this.job={...this.job,status:'succeeded',phase:'healthy',finishedAt:new Date().toISOString(),images:release.images};await this.save();
    } catch(error) {
      if(changed){
        await this.phase('rollback');
        try {
          if(previous){await this.setCurrent(previous);await this.command(['compose','--env-file','.env','-f','compose.homeserver.yml','up','-d','--no-build','--wait','--wait-timeout','180'],{cwd:previous,env,timeout:240000});}
          else {await this.command(['compose','--env-file','.env','-f','compose.homeserver.yml','down'],{cwd:directory,env});await fs.rm(path.join(this.root,'current'),{force:true});}
        } catch(rollback){throw new Error(`${error.message}; rollback failed: ${rollback.message}`);}
      }
      throw error;
    } finally {await fs.rm(credentials,{recursive:true,force:true});}
  }
}
