async (page) => {
  const pages=page.context().pages();
  const label='Reaction Fixture의 AI 리액션 👍';
  await Promise.all(pages.map(p=>p.getByRole('status',{name:label,exact:true}).waitFor({timeout:10000})));
  const shown=await Promise.all(pages.map(async p=>({url:p.url(),count:await p.getByRole('status',{name:label,exact:true}).count(),text:await p.getByRole('status',{name:label,exact:true}).innerText()})));
  await page.screenshot({path:'output/playwright/keepick-step4/reaction-shown.png'});
  await Promise.all(pages.map(p=>p.getByRole('status',{name:label,exact:true}).waitFor({state:'hidden',timeout:8000})));
  return {shown,expiredWhilePeerStillJoined:await Promise.all(pages.map(p=>p.getByText('Reaction Fixture',{exact:true}).count()))};
}
