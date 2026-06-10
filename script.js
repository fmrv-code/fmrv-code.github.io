/* =============================================================
   NEWSFLOW — script.js
   25 sources FR · Météo · Briefing · Recherche · Catégories
   ============================================================= */
 
/* ===== 1. SOURCES ===== */
const SOURCES = [
  { name:"Frandroid",         category:"tech",     icon:"📱", color:"var(--g-tech)",     rss:"https://www.frandroid.com/feed" },
  { name:"01net",             category:"tech",     icon:"💻", color:"var(--g-tech)",     rss:"https://www.01net.com/feed/" },
  { name:"Numerama",          category:"tech",     icon:"🔭", color:"var(--g-tech)",     rss:"https://www.numerama.com/feed/" },
  { name:"Clubic",            category:"tech",     icon:"📡", color:"var(--g-tech)",     rss:"https://www.clubic.com/feed/rss.xml" },
  { name:"MacG",              category:"tech",     icon:"🍎", color:"var(--g-tech)",     rss:"https://www.macg.co/rss.xml" },
  { name:"iGen",              category:"tech",     icon:"📲", color:"var(--g-tech)",     rss:"https://www.igen.fr/rss.xml" },
  { name:"ZDNet FR",          category:"tech",     icon:"🖥️", color:"var(--g-tech)",     rss:"https://www.zdnet.fr/feeds/rss/" },
  { name:"Tom's Hardware",    category:"hardware", icon:"⚙️", color:"var(--g-hardware)", rss:"https://www.tomshardware.fr/feed" },
  { name:"Les Numériques",    category:"hardware", icon:"🖨️", color:"var(--g-hardware)", rss:"https://www.lesnumeriques.com/rss.xml" },
  { name:"Cowcotland",        category:"hardware", icon:"🔧", color:"var(--g-hardware)", rss:"https://www.cowcotland.com/rss.xml" },
  { name:"Jeuxvideo.com",     category:"gaming",   icon:"🎮", color:"var(--g-gaming)",   rss:"https://www.jeuxvideo.com/rss/rss-news.xml" },
  { name:"IGN France",        category:"gaming",   icon:"🕹️", color:"var(--g-gaming)",   rss:"https://fr.ign.com/feed.xml" },
  { name:"Gamekult",          category:"gaming",   icon:"👾", color:"var(--g-gaming)",   rss:"https://www.gamekult.com/feed.xml" },
  { name:"Canard PC",         category:"gaming",   icon:"🦆", color:"var(--g-gaming)",   rss:"https://www.canardpc.com/feed" },
  { name:"Le Monde",          category:"actu",     icon:"🗞️", color:"var(--g-actu)",     rss:"https://www.lemonde.fr/rss/une.xml" },
  { name:"Le Figaro",         category:"actu",     icon:"📰", color:"var(--g-actu)",     rss:"https://www.lefigaro.fr/rss/figaro_actualites.xml" },
  { name:"Franceinfo",        category:"actu",     icon:"📺", color:"var(--g-actu)",     rss:"https://www.francetvinfo.fr/titres.rss" },
  { name:"L'Express",         category:"actu",     icon:"📄", color:"var(--g-actu)",     rss:"https://www.lexpress.fr/arc/outboundfeeds/rss/?outputType=xml" },
  { name:"20 Minutes",        category:"actu",     icon:"⏱️", color:"var(--g-actu)",     rss:"https://www.20minutes.fr/feeds/rss/actu" },
  { name:"Futura Sciences",   category:"science",  icon:"🔬", color:"var(--g-science)",  rss:"https://www.futura-sciences.com/rss/actualites.xml" },
  { name:"Science & Vie",     category:"science",  icon:"🧬", color:"var(--g-science)",  rss:"https://www.science-et-vie.com/feed" },
  { name:"Nat Geo France",    category:"science",  icon:"🌍", color:"var(--g-science)",  rss:"https://www.nationalgeographic.fr/rss.xml" },
  { name:"L'Équipe",          category:"sport",    icon:"⚽", color:"var(--g-sport)",    rss:"https://www.lequipe.fr/rss/actu_rss.xml" },
  { name:"Eurosport",         category:"sport",    icon:"🏆", color:"var(--g-sport)",    rss:"https://www.eurosport.fr/rss.xml" },
  { name:"Automobile Propre", category:"mobilite", icon:"⚡", color:"var(--g-mobilite)", rss:"https://www.automobile-propre.com/feed" },
];
 
/* Catégories "virtuelles" détectées par mots-clés dans le titre/description */
const KEYWORD_CATS = {
  ia:     ["intelligence artificielle"," ia ","chatgpt","openai","gemini","claude","llm","machine learning","copilot","midjourney","anthropic"],
  espace: ["espace","nasa","spacex","fusée","fusee","satellite","astronomie","galaxie","planète","planete","mars","lune","télescope","telescope"],
  sante:  ["santé","sante","médecine","medecine","vaccin","maladie","cancer","virus","hôpital","hopital","cerveau","adn"],
};
 
/* ===== 2. CONFIG ===== */
const RSS2JSON  = "https://api.rss2json.com/v1/api.json?api_key=omswhjx3rikyfg98pxers1xhluruu8cdqfyba0gn&rss_url=";
const MAX_ART   = 8;
const LAT = 48.1983, LON = 3.2878; // Sens (89100)
 
const CAT_LABELS = {
  all:"À la une", actu:"Monde & France", tech:"Tech", ia:"Intelligence artificielle",
  hardware:"Hardware", gaming:"Gaming", science:"Science", espace:"Espace",
  sante:"Santé", sport:"Sport", mobilite:"Mobilité",
};
 
/* ===== 3. ÉTAT ===== */
let allArticles = [];
let currentCat  = "all";
let searchQuery = "";
 
/* ===== 4. DATE ===== */
function showDate() {
  const d = new Date().toLocaleDateString("fr-FR",{weekday:"long",day:"numeric",month:"long"});
  const cap = d.charAt(0).toUpperCase()+d.slice(1);
  document.getElementById("navbar-date").textContent = cap;
  document.getElementById("briefing-time").textContent =
    new Date().toLocaleTimeString("fr-FR",{hour:"2-digit",minute:"2-digit"});
}
 
/* ===== 5. MÉTÉO (Open-Meteo, gratuit, sans clé) ===== */
const WMO = {0:["☀️","Ciel dégagé"],1:["🌤️","Plutôt dégagé"],2:["⛅","Partiellement nuageux"],3:["☁️","Couvert"],
  45:["🌫️","Brouillard"],48:["🌫️","Brouillard givrant"],51:["🌦️","Bruine"],53:["🌦️","Bruine"],55:["🌦️","Bruine"],
  61:["🌧️","Pluie légère"],63:["🌧️","Pluie"],65:["🌧️","Forte pluie"],71:["🌨️","Neige"],73:["🌨️","Neige"],75:["🌨️","Forte neige"],
  80:["🌦️","Averses"],81:["🌦️","Averses"],82:["⛈️","Fortes averses"],95:["⛈️","Orage"],96:["⛈️","Orage"],99:["⛈️","Orage violent"]};
 
async function loadWeather() {
  try {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${LAT}&longitude=${LON}`
      + `&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code`
      + `&daily=weather_code,temperature_2m_max&wind_speed_unit=kmh&timezone=Europe/Paris&forecast_days=5`;
    const data = await (await fetch(url)).json();
    const c = data.current;
    const [emoji,desc] = WMO[c.weather_code] || ["🌡️","Variable"];
 
    document.getElementById("weather-emoji").textContent = emoji;
    document.getElementById("weather-temp").textContent  = `${Math.round(c.temperature_2m)}°`;
    document.getElementById("weather-desc").textContent  = desc;
    document.getElementById("w-feels").textContent = `${Math.round(c.apparent_temperature)}°`;
    document.getElementById("w-wind").textContent  = `${Math.round(c.wind_speed_10m)} km/h`;
    document.getElementById("w-humid").textContent = `${c.relative_humidity_2m}%`;
 
    // Prévisions 4 jours suivants
    const days = ["Dim","Lun","Mar","Mer","Jeu","Ven","Sam"];
    let html = "";
    for (let i=1; i<=4; i++) {
      const dayName = days[new Date(data.daily.time[i]).getDay()];
      const [em] = WMO[data.daily.weather_code[i]] || ["🌡️"];
      html += `<div class="fc-day"><span class="fc-name">${dayName}</span>
               <span class="fc-emoji">${em}</span>
               <span class="fc-temp">${Math.round(data.daily.temperature_2m_max[i])}°</span></div>`;
    }
    document.getElementById("weather-forecast").innerHTML = html;
  } catch(e) {
    document.getElementById("weather-desc").textContent = "Météo indisponible";
  }
}
 
/* ===== 6. FLUX RSS ===== */
async function fetchFeed(s) {
  try {
    const res = await fetch(`${RSS2JSON}${encodeURIComponent(s.rss)}&count=${MAX_ART}`);
    if (!res.ok) return [];
    const data = await res.json();
    if (data.status !== "ok" || !data.items) return [];
    return data.items.map(it => ({
      title:it.title||"Sans titre", description:it.description||"",
      link:it.link||"#", thumbnail:it.thumbnail||it.enclosure?.link||"",
      pubDate:it.pubDate||"", sourceName:s.name, sourceColor:s.color,
      sourceIcon:s.icon, category:s.category,
    }));
  } catch(e) { return []; }
}
 
/* ===== 7. OUTILS ===== */
function stripHTML(h){ const d=document.createElement("div"); d.innerHTML=h; return d.textContent||""; }
 
function timeAgo(str) {
  if(!str) return "";
  const diff = (Date.now()-new Date(str))/1000;
  if(isNaN(diff)) return "";
  if(diff<3600)   return `il y a ${Math.max(1,Math.round(diff/60))} min`;
  if(diff<86400)  return `il y a ${Math.round(diff/3600)} h`;
  return `il y a ${Math.round(diff/86400)} j`;
}
 
/* Vérifie si un article appartient à une catégorie mots-clés */
function matchKeyword(article, cat) {
  const text = (article.title+" "+article.description).toLowerCase();
  return KEYWORD_CATS[cat].some(kw => text.includes(kw));
}
 
/* Retourne les articles selon catégorie + recherche */
function getFiltered() {
  let list = allArticles;
  // Catégorie
  if (currentCat !== "all") {
    if (KEYWORD_CATS[currentCat]) list = list.filter(a => matchKeyword(a,currentCat));
    else list = list.filter(a => a.category === currentCat);
  }
  // Recherche
  if (searchQuery) {
    const q = searchQuery.toLowerCase();
    list = list.filter(a => (a.title+" "+a.description+" "+a.sourceName).toLowerCase().includes(q));
  }
  return list;
}
 
/* ===== 8. BRIEFING ===== */
function showBriefing() {
  const ul = document.getElementById("briefing-list");
  const tops = allArticles.filter(a=>a.category==="actu").slice(0,5);
  if(!tops.length){ ul.innerHTML='<li style="color:var(--text-dim)">Indisponible</li>'; return; }
  ul.innerHTML = tops.map((a,i)=>`
    <a class="briefing-item" href="${a.link}" target="_blank" rel="noopener">
      <span class="briefing-rank">${i+1}</span>
      <span>${a.title}</span>
    </a>`).join("");
}
 
/* ===== 9. CARTE ===== */
function makeCard(a, index) {
  const card = document.createElement("a");
  card.className = "news-card";
  card.href = a.link; card.target="_blank"; card.rel="noopener noreferrer";
  card.style.animationDelay = `${Math.min(index*0.03,0.4)}s`;
  const desc = stripHTML(a.description).trim().substring(0,110);
  card.innerHTML = `
    ${a.thumbnail ? `<img class="card-img" src="${a.thumbnail}" alt="" loading="lazy" onerror="this.style.display='none'">` : ""}
    <div class="card-body">
      <span class="card-source" style="background:${a.sourceColor}">${a.sourceIcon} ${a.sourceName}</span>
      <h3 class="card-title">${a.title}</h3>
      ${desc ? `<p class="card-desc">${desc}…</p>` : ""}
      <span class="card-time">${timeAgo(a.pubDate)}</span>
    </div>`;
  return card;
}
 
/* ===== 10. RENDU ===== */
function render() {
  const grid = document.getElementById("grid");
  const list = getFiltered();
 
  document.getElementById("section-title").textContent =
    searchQuery ? `Résultats pour « ${searchQuery} »` : CAT_LABELS[currentCat];
  document.getElementById("article-count").textContent = list.length;
 
  grid.innerHTML = "";
  if(!list.length){
    grid.innerHTML = `<div class="empty"><span class="empty-emoji">🔍</span>
      Aucun article trouvé. Essayez une autre catégorie ou recherche.</div>`;
    return;
  }
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
 

