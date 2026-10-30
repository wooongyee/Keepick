// Synthetic display input only. No desktop/window is captured by this fixture.
async (page) => {
  await page.context().addInitScript(() => {
    if (!['localhost','127.0.0.1'].includes(location.hostname)) return;
    window.__keepickDisplays=[];
    navigator.mediaDevices.getDisplayMedia=async()=>{
      const canvas=document.createElement('canvas');canvas.width=640;canvas.height=360;
      const ctx=canvas.getContext('2d');let frame=0,raf;
      const draw=()=>{ctx.fillStyle=frame%2?'#00aaff':'#fa8000';ctx.fillRect(0,0,640,360);ctx.fillStyle='white';ctx.fillText(String(frame++),40,40);raf=requestAnimationFrame(draw);};draw();
      const stream=canvas.captureStream(10),track=stream.getVideoTracks()[0],stop=track.stop.bind(track);
      track.stop=()=>{cancelAnimationFrame(raf);stop();};
      window.__keepickDisplays.push(track);
      return stream;
    };
  });
}
