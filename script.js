
  list.forEach((a,i)=>grid.appendChild(makeCard(a,i)));
}
 
/* ===== 11. CATÉGORIES ===== */
function setupCats() {
  document.querySelectorAll(".cat").forEach(btn=>{
    btn.addEventListener("click",()=>{
      currentCat = btn.dataset.cat;
      document.querySelectorAll(".cat").forEach(b=>b.classList.remove("active"));
      btn.classList.add("active");
      render();
      window.scrollTo({top:document.querySelector(".cats").offsetTop-80,behavior:"smooth"});
    });
  });
}
 
/* ===== 12. RECHERCHE ===== */
function setupSearch() {
  const input = document.getElementById("search-input");
  const clear = document.getElementById("search-clear");
  let timer;
  input.addEventListener("input",()=>{
    clearTimeout(timer);
    clear.classList.toggle("show", input.value.length>0);
    timer = setTimeout(()=>{ searchQuery = input.value.trim(); render(); }, 180);
  });
  clear.addEventListener("click",()=>{
    input.value=""; searchQuery=""; clear.classList.remove("show"); render(); input.focus();
  });
}
 
/* ===== 13. INIT ===== */
async function init() {
  const loader = document.getElementById("loader");
  const total  = document.getElementById("loader-total");
  const prog   = document.getElementById("loader-progress");
  total.textContent = SOURCES.length;
  loader.classList.remove("hidden");
 
  let done=0;
  const results = await Promise.all(SOURCES.map(s =>
    fetchFeed(s).then(arr=>{ prog.textContent = ++done; return arr; })
  ));
  loader.classList.add("hidden");
 
  allArticles = results.flat().sort((a,b)=>new Date(b.pubDate)-new Date(a.pubDate));
  showBriefing();
  render();
}
 
/* ===== 14. DÉMARRAGE ===== */
document.addEventListener("DOMContentLoaded",()=>{
  showDate();
  loadWeather();
  setupCats();
  setupSearch();
  init();
});

