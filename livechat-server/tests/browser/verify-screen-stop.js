async (page) => {
  for (const old of page.context().pages()) if (old !== page) await old.close();
  const other=await page.context().newPage();
  const room='step4-browser';
  async function join(client,name,tone){
    await client.goto(`http://localhost:3110/groupchat/${room}?testTone=${tone}`);
    await client.getByRole('textbox',{name:'이름을 입력하세요'}).fill(name);
    await client.getByRole('button',{name:'지금 참여하기'}).click();
    await client.getByRole('button',{name:'설정',exact:true}).waitFor();
    await client.waitForFunction(()=>document.querySelectorAll('video').length===2);
  }
  // Sequential join avoids waiting for the second participant before it exists.
  await page.goto(`http://localhost:3110/groupchat/${room}?testTone=440`);
  await page.getByRole('textbox',{name:'이름을 입력하세요'}).fill('Screen A');
  await page.getByRole('button',{name:'지금 참여하기'}).click();
  await page.getByRole('button',{name:'설정',exact:true}).waitFor();
  await join(other,'Screen B',660);
  await page.waitForFunction(()=>document.querySelectorAll('video').length===2);
  const results=[];
  async function share(sender,receiver,stop){
    await sender.getByRole('button',{name:'화면 공유 시작',exact:true}).click();
    await receiver.waitForFunction(()=>document.querySelectorAll('video').length===3);
    await receiver.waitForFunction(()=>[...document.querySelectorAll('video')].every(v=>v.videoWidth>0 && v.getVideoPlaybackQuality().totalVideoFrames>=2));
    const before=await receiver.evaluate(()=>[...document.querySelectorAll('video')].map(v=>({frames:v.getVideoPlaybackQuality().totalVideoFrames,width:v.videoWidth,tracks:v.srcObject?.getTracks().map(t=>({id:t.id,state:t.readyState}))})));
    if(stop==='browser-ended'){
      await sender.evaluate(()=>{const track=window.__keepickDisplays.at(-1);track.stop();track.dispatchEvent(new Event('ended'));});
    }else await sender.getByRole('button',{name:'화면 공유 중지',exact:true}).click();
    await receiver.waitForFunction(()=>document.querySelectorAll('video').length===2);
    await sender.getByRole('button',{name:'화면 공유 시작',exact:true}).waitFor();
    results.push({stop,remoteVideosBefore:before,after:await receiver.evaluate(()=>({videos:document.querySelectorAll('video').length,states:(window.__keepickPeerConnections||[]).filter(pc=>pc.connectionState!=='closed').map(pc=>pc.connectionState),camera:(window.__keepickCaptureStreams||[]).flatMap(s=>s.getVideoTracks()).filter(t=>t.readyState==='live').map(t=>t.id)}))});
  }
  await share(page,other,'app-button');
  // Receiver A has already closed its own share: its producer_closed listener
  // must still receive B's closure after the old timeout interval.
  await other.waitForTimeout(3200);
  await share(other,page,'app-button-after-own-stop');
  await share(page,other,'browser-ended');
  return results;
}
