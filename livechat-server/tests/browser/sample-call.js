async (page) => {
  return await page.evaluate(async () => {
    const sample = async () => {
    const pcs = [];
    for (const pc of window.__keepickPeerConnections || []) {
      if (pc.connectionState === 'closed') continue;
      const stats = await pc.getStats();
      const rtp = [...stats.values()].filter(s => s.type === 'inbound-rtp' || s.type === 'outbound-rtp').map(s => ({
        id:s.id, type:s.type, kind:s.kind, ssrc:s.ssrc,
        bytesSent:s.bytesSent, bytesReceived:s.bytesReceived,
        packetsSent:s.packetsSent, packetsReceived:s.packetsReceived,
        framesEncoded:s.framesEncoded, framesDecoded:s.framesDecoded,
        framesPerSecond:s.framesPerSecond, totalAudioEnergy:s.totalAudioEnergy,
        totalSamplesReceived:s.totalSamplesReceived, audioLevel:s.audioLevel,
        codec:stats.get(s.codecId)?.mimeType,
      }));
      pcs.push({state:pc.connectionState,iceState:pc.iceConnectionState,rtp,senders:pc.getSenders().map(s=>({kind:s.track?.kind,enabled:s.track?.enabled,state:s.track?.readyState,label:s.track?.label})),sources:[...stats.values()].filter(s=>s.type==="media-source").map(s=>({kind:s.kind,audioLevel:s.audioLevel,totalAudioEnergy:s.totalAudioEnergy}))});
    }
    const videos = [...document.querySelectorAll('video')].map(video => {
      const canvas = document.createElement('canvas');canvas.width=32;canvas.height=18;
      let frameHash=null,meanRGB=null;
      if (video.readyState>=2) {
        const ctx=canvas.getContext('2d');ctx.drawImage(video,0,0,32,18);
        const data=ctx.getImageData(0,0,32,18).data;let hash=2166136261;const sums=[0,0,0];
        for(let i=0;i<data.length;i++){hash=Math.imul(hash^data[i],16777619);if(i%4<3)sums[i%4]+=data[i];}
        frameHash=(hash>>>0).toString(16);meanRGB=sums.map(n=>Math.round(n/(32*18)));
      }
      return {card:video.parentElement.innerText.slice(0,120),width:video.videoWidth,height:video.videoHeight,
        readyState:video.readyState,paused:video.paused,currentTime:video.currentTime,
        totalVideoFrames:video.getVideoPlaybackQuality().totalVideoFrames,frameHash,meanRGB,
        tracks:video.srcObject?.getTracks().map(t=>({id:t.id,kind:t.kind,label:t.label,enabled:t.enabled,state:t.readyState}))};
    });
    const audio = [...document.querySelectorAll('audio')].map(a=>({paused:a.paused,muted:a.muted,currentTime:a.currentTime,
      tracks:a.srcObject?.getTracks().map(t=>({kind:t.kind,enabled:t.enabled,state:t.readyState}))}));
    return {url:location.href,timestamp:performance.now(),pcs,videos,audio};
    };
    const before = await sample();
    await new Promise(resolve => setTimeout(resolve, 3000));
    return {before, after: await sample()};
  });
}
