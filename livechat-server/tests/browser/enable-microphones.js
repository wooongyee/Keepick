async(page)=>{
 for(const p of page.context().pages()) await p.getByRole('button',{name:'마이크 켜기',exact:true}).click();
 return {microphonesEnabled:true};
}
