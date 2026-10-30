// Read-only diagnostics for setup-media.js; no application globals are added.
async (page) => {
  return await page.evaluate(() => ({
    url: location.href,
    captures: (window.__keepickCaptureStreams || []).map(stream => stream.getTracks().map(track => ({
      id:track.id,kind:track.kind,label:track.label,state:track.readyState,enabled:track.enabled,
    }))),
    senders: (window.__keepickPeerConnections || []).filter(pc=>pc.connectionState!=='closed').flatMap(pc=>pc.getSenders().map(s=>({
      id:s.track?.id,kind:s.track?.kind,label:s.track?.label,state:s.track?.readyState,enabled:s.track?.enabled,
    }))),
    videos:[...document.querySelectorAll('video')].map(v=>({width:v.videoWidth,height:v.videoHeight,frames:v.getVideoPlaybackQuality().totalVideoFrames,
      tracks:v.srcObject?.getTracks().map(t=>({id:t.id,label:t.label,state:t.readyState}))})),
    aiRequests:performance.getEntriesByType('resource').filter(r=>/\/models\/|mediapipe|\/images\/gestures\//.test(r.name)).map(r=>({url:r.name,bytes:r.transferSize})),
    alerts:[...document.querySelectorAll('[role="alert"]')].map(a=>a.textContent).filter(Boolean),
  }));
}
