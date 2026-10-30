// Fixture: config-a camera, AI off, conference Settings panel open.
async (page) => {
  const inspect = () => page.evaluate(() => ({
    captures:(window.__keepickCaptureStreams||[]).map(s=>s.getTracks().map(t=>({id:t.id,kind:t.kind,state:t.readyState}))),
    sender:(window.__keepickPeerConnections||[]).flatMap(pc=>pc.getSenders()).find(s=>s.track?.kind==='video')?.track && (()=>{
      const t=(window.__keepickPeerConnections||[]).flatMap(pc=>pc.getSenders()).find(s=>s.track?.kind==='video').track;
      return {id:t.id,label:t.label,state:t.readyState};
    })(),
    aiRequests:performance.getEntriesByType('resource').filter(r=>/\/models\/|mediapipe|\/images\/gestures\//.test(r.name)).length,
  }));
  const results=[];
  for(let i=0;i<2;i++){
    await page.getByRole('switch',{name:'AI 기능 전체'}).click();
    await page.waitForFunction(()=> (window.__keepickPeerConnections||[]).flatMap(pc=>pc.getSenders()).some(s=>s.track?.kind==='video'&&!s.track.label.includes('camera-a.y4m')),null,{timeout:25000});
    results.push({phase:'on',...await inspect()});
    await page.getByRole('switch',{name:'AI 기능 전체'}).click();
    await page.waitForFunction(()=> (window.__keepickPeerConnections||[]).flatMap(pc=>pc.getSenders()).some(s=>s.track?.kind==='video'&&s.track.label.includes('camera-a.y4m')),null,{timeout:5000});
    results.push({phase:'off',...await inspect()});
  }
  return results;
}
