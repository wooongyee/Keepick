import {test} from 'node:test';import assert from 'node:assert/strict';import {validateRelease,components,Releases} from '../release.mjs';
import fs from 'node:fs/promises';
import os from 'node:os';
import path from 'node:path';
const good=()=>({revision:'a'.repeat(40),images:Object.fromEntries(components.map(name=>[name,`ghcr.io/wooongyee/keepick-${name}@sha256:${'b'.repeat(64)}`]))});
test('accepts only immutable images for the exact Keepick components',()=>assert.deepEqual(validateRelease(good()),good()));
test('rejects branch names, tags, other namespaces and shell text',()=>{
 for(const revision of ['master','abc','a'.repeat(40)+';id'])assert.throws(()=>validateRelease({...good(),revision}));
 for(const image of ['ghcr.io/wooongyee/keepick-web:latest',`ghcr.io/other/image@sha256:${'b'.repeat(64)}`,'x; id']){const d=good();d.images.web=image;assert.throws(()=>validateRelease(d));}
});

async function fixture(t, {head='a'.repeat(40), imageRevision='a'.repeat(40), failHealth=false}={}) {
 const root=await fs.mkdtemp(path.join(os.tmpdir(),'keepick-release-test-'));
 t.after(()=>fs.rm(root,{recursive:true,force:true}));
 await fs.mkdir(path.join(root,'shared'));
 await fs.writeFile(path.join(root,'shared/runtime.env'),'TEST_ONLY=true\n');
 const calls=[];
 const releases=new Releases(root,{
  request:async url=>new Response(JSON.stringify(String(url).includes('api.github.com')?{object:{sha:head}}:{services:{}})),
  runCommand:async(args,options={})=>{
   calls.push({args,cwd:options.cwd});
   if(args[0]==='image')return JSON.stringify([{Config:{Labels:{'org.opencontainers.image.revision':imageRevision}}}]);
   if(failHealth&&args.includes('up')&&options.cwd.endsWith('a'.repeat(40)))throw new Error('new release unhealthy');
   return '';
  },
 });
 await releases.initialize();
 const finish=async()=>{
  const deadline=Date.now()+3000;
  while(releases.busy&&Date.now()<deadline)await new Promise(resolve=>setTimeout(resolve,5));
  assert.equal(releases.busy,false,'deployment completes');
  return releases.job;
 };
 return {root,releases,calls,finish,input:{...good(),registry_token:'ephemeral-test-token'}};
}

test('rejects stale master revisions before Docker or current release changes',async t=>{
 const f=await fixture(t,{head:'c'.repeat(40)});
 await f.releases.apply(f.input);
 assert.equal((await f.finish()).status,'failed');
 assert.equal(f.calls.length,0);
 assert.equal(await f.releases.current(),null);
});

test('image source mismatch never activates a release',async t=>{
 const f=await fixture(t,{imageRevision:'c'.repeat(40)});
 await f.releases.apply(f.input);
 assert.match((await f.finish()).error,/Image revision mismatch/);
 assert.equal(f.calls.some(call=>call.args.includes('up')),false);
 assert.equal(await f.releases.current(),null);
});

test('failed health restores previous images without deleting persistent volumes',async t=>{
 const f=await fixture(t,{failHealth:true});
 const previous=path.join(f.root,'releases','previous');
 await fs.mkdir(previous);
 await f.releases.setCurrent(previous);
 await f.releases.apply(f.input);
 assert.match((await f.finish()).error,/new release unhealthy/);
 assert.equal(await f.releases.current(),previous);
 assert.ok(f.calls.some(call=>call.cwd===previous&&call.args.includes('up')));
 assert.equal(f.calls.some(call=>call.args.includes('down')||call.args.includes('-v')),false);
});

test('successful deployment records exact images and rejects overlapping operations',async t=>{
 const f=await fixture(t);
 await f.releases.apply(f.input);
 await assert.rejects(f.releases.apply(f.input),/already running/);
 const job=await f.finish();
 assert.equal(job.status,'succeeded');
 assert.deepEqual(job.images,f.input.images);
 assert.equal(await f.releases.current(),path.join(f.root,'releases',f.input.revision));
});
