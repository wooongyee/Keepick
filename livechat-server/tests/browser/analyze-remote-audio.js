async (page) => await page.evaluate(async () => {
  const audio=[...document.querySelectorAll('audio')].find(a=>a.srcObject?.getAudioTracks().length);
  if(!audio) throw new Error('No rendered remote audio stream');
  const context=new AudioContext();await context.resume();
  const source=context.createMediaStreamSource(audio.srcObject),analyser=context.createAnalyser(),silence=context.createGain();
  analyser.fftSize=8192;silence.gain.value=0;source.connect(analyser).connect(silence).connect(context.destination);
  await new Promise(resolve=>setTimeout(resolve,400));
  const frequencies=new Float32Array(analyser.frequencyBinCount);analyser.getFloatFrequencyData(frequencies);
  let peak=0;for(let i=1;i<frequencies.length;i++)if(frequencies[i]>frequencies[peak])peak=i;
  const samples=new Float32Array(analyser.fftSize);analyser.getFloatTimeDomainData(samples);
  const result={peakHz:peak*context.sampleRate/analyser.fftSize,peakDb:frequencies[peak],rms:Math.sqrt(samples.reduce((s,v)=>s+v*v,0)/samples.length)};
  await context.close();return result;
})
