// Run with playwright-cli run-code --filename after opening about:blank.
// Install before navigating to the real app; use ?testTone=440 or ?testTone=660.
async (page) => {
  await page.addInitScript(() => {
    if (!["localhost", "127.0.0.1"].includes(location.hostname)) return;
    const connections = [];
    window.__keepickCaptureStreams = [];
    window.__keepickPeerConnections = connections;
    window.RTCPeerConnection = new Proxy(window.RTCPeerConnection, {
      construct(Target, args) {
        const pc = new Target(...args);
        connections.push(pc);
        return pc;
      },
    });

    // Substitute only the capture input. App signaling, tracks, codecs and SFU
    // are real. Fake video capture must be configured in Chromium launch args.
    const requested = Number(new URLSearchParams(location.search).get("testTone"));
    const frequency = [440, 660].includes(requested) ? requested : 440;
    const capture = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
    navigator.mediaDevices.getUserMedia = async (constraints) => {
      const stream = constraints.video
        ? await capture({ ...constraints, audio: false })
        : new MediaStream();
      if (constraints.audio) {
        const context = new AudioContext();
        await context.resume();
        const oscillator = context.createOscillator();
        const gain = context.createGain();
        const output = context.createMediaStreamDestination();
        oscillator.frequency.value = frequency;
        gain.gain.value = 0.15;
        oscillator.connect(gain).connect(output);
        oscillator.start();
        const track = output.stream.getAudioTracks()[0];
        const stop = track.stop.bind(track);
        let stopped = false;
        track.stop = () => {
          if (stopped) return;
          stopped = true;
          stop();
          oscillator.stop();
          void context.close();
        };
        stream.addTrack(track);
      }
      window.__keepickCaptureStreams.push(stream);
      return stream;
    };
  });
}
