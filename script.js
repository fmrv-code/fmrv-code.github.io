/* =============================================
   NEWSFLOW DASHBOARD — script.js
   25 sources françaises + Météo + Briefing
   ============================================= */
 
/* ===== 1. LES 25 SOURCES RSS ===== */
const SOURCES = [
  // === TECH ===
  { name:"Frandroid",        category:"tech",     icon:"📱", color:"linear-gradient(135deg,#7c3aed,#2563eb)", rss:"https://www.frandroid.com/feed" },
  { name:"01net",            category:"tech",     icon:"💻", color:"linear-gradient(135deg,#2563eb,#06b6d4)", rss:"https://www.01net.com/feed/" },
  { name:"Numerama",         category:"tech",     icon:"🔭", color:"linear-gradient(135deg,#06b6d4,#3b82f6)", rss:"https://www.numerama.com/feed/" },
  { name:"Clubic",           category:"tech",     icon:"📡", color:"linear-gradient(135deg,#3b82f6,#7c3aed)", rss:"https://www.clubic.com/feed/rss.xml" },
  { name:"MacG",             category:"tech",     icon:"🍎", color:"linear-gradient(135deg,#6366f1,#8b5cf6)", rss:"https://www.macg.co/rss.xml" },
  { name:"iGen",             category:"tech",     icon:"📲", color:"linear-gradient(135deg,#8b5cf6,#a78bfa)", rss:"https://www.igen.fr/rss.xml" },
  { name:"ZDNet FR",         category:"tech",     icon:"🖥️", color:"linear-gradient(135deg,#0ea5e9,#6366f1)", rss:"https://www.zdnet.fr/feeds/rss/" },
 
  // === HARDWARE ===
  { name:"Tom's Hardware FR",category:"hardware", icon:"⚙️", color:"linear-gradient(135deg,#7c3aed,#4f46e5)", rss:"https://www.tomshardware.fr/feed" },
  { name:"Les Numériques",   category:"hardware", icon:"🖨️", color:"linear-gradient(135deg,#4f46e5,#2563eb)", rss:"https://www.lesnumeriques.com/rss.xml" },
  { name:"Cowcotland",       category:"hardware", icon:"🔧", color:"linear-gradient(135deg,#2563eb,#3b82f6)", rss:"https://www.cowcotland.com/rss.xml" },
 
  // === GAMING ===
  { name:"Jeuxvideo.com",    category:"gaming",   icon:"🎮", color:"linear-gradient(135deg,#ec4899,#f97316)", rss:"https://www.jeuxvideo.com/rss/rss-news.xml" },
  { name:"IGN France",       category:"gaming",   icon:"🕹️", color:"linear-gradient(135deg,#f97316,#ef4444)", rss:"https://fr.ign.com/feed.xml" },
  { name:"Gamekult",         category:"gaming",   icon:"👾", color:"linear-gradient(135deg,#ef4444,#ec4899)", rss:"https://www.gamekult.com/feed.xml" },
  { name:"Canard PC",        category:"gaming",   icon:"🦆", color:"linear-gradient(135deg,#ec4899,#a855f7)", rss:"https://www.canardpc.com/feed" },
 
  // === ACTU GÉNÉRALE ===
  { name:"Le Monde",         category:"actu",     icon:"🗞️", color:"linear-gradient(135deg,#ef4444,#dc2626)", rss:"https://www.lemonde.fr/rss/une.xml" },
  { name:"Le Figaro",        category:"actu",     icon:"📰", color:"linear-gradient(135deg,#dc2626,#b91c1c)", rss:"https://www.lefigaro.fr/rss/figaro_actualites.xml" },
  { name:"Franceinfo",       category:"actu",     icon:"📺", color:"linear-gradient(135deg,#b91c1c,#ef4444)", rss:"https://www.francetvinfo.fr/titres.rss" },
  { name:"L'Express",        category:"actu",     icon:"📄", color:"linear-gradient(135deg,#f97316,#ef4444)", rss:"https://www.lexpress.fr/arc/outboundfeeds/rss/?outputType=xml" },
  { name:"20 Minutes",       category:"actu",     icon:"⏱️", color:"linear-gradient(135deg,#ef4444,#f97316)", rss:"https://www.20minutes.fr/feeds/rss/actu" },
 
  // === SCIENCE ===
  { name:"Futura Sciences",  category:"science",  icon:"🔬", color:"linear-gradient(135deg,#06b6d4,#10b981)", rss:"https://www.futura-sciences.com/rss/actualites.xml" },
  { name:"Science & Vie",    category:"science",  icon:"🧬", color:"linear-gradient(135deg,#10b981,#06b6d4)", rss:"https://www.science-et-vie.com/feed" },
  { name:"Nat Geo France",   category:"science",  icon:"🌍", color:"linear-gradient(135deg,#0ea5e9,#06b6d4)", rss:"https://www.nationalgeographic.fr/rss.xml" },
 
  // === SPORT ===
  { name:"L'Équipe",         category:"sport",    icon:"⚽", color:"linear-gradient(135deg,#f97316,#fbbf24)", rss:"https://www.lequipe.fr/rss/actu_rss.xml" },
  { name:"Eurosport",        category:"sport",    icon:"🏆", color:"linear-gradient(135deg,#fbbf24,#f97316)", rss:"https://www.eurosport.fr/rss.xml" },
 
  // === MOBILITÉ ===
  { name:"Automobile Propre",category:"mobilite", icon:"⚡", color:"linear-gradient(135deg,#10b981,#3b82f6)", rss:"https://www.automobile-propre.com/feed" },
];
 
/* ===== 2. CONFIG ===== */
const RSS2JSON   = "https://api.rss2json.com/v1/api.json?rss_url=";
const MAX_ART    = 8;   // articles max par source
const METEO_LAT  = 48.1983;  // Sens (89100)
const METEO_LON  = 3.2878;
 
/* ===== 3. VARIABLES GLOBALES ===== */
let allArticles  = [];
let currentFilter = "all";
 
/* ===== 4. DATE EN FRANÇAIS ===== */
function afficherDate() {
  const d = new Date();
  const options = { weekday:"long", day:"numeric", month:"long", year:"numeric" };
  const str = d.toLocaleDateString("fr-FR", options);
  // Met la première lettre en majuscule
  document.getElementById("navbar-date").textContent =
    str.charAt(0).toUpperCase() + str.slice(1);
}
 
/* ===== 5. MÉTÉO (Open-Meteo — 100% gratuit, sans clé API) ===== */
async function chargerMeteo() {
  try {
    const url = `https://api.open-meteo.com/v1/forecast?latitude=${METEO_LAT}&longitude=${METEO_LON}&current=temperature_2m,relative_humidity_2m,apparent_temperature,wind_speed_10m,weather_code&wind_speed_unit=kmh&timezone=Europe/Paris`;
    const res  = await fetch(url);
    const data = await res.json();
    const c    = data.current;
 
    // Interprète le code météo WMO
    const desc = interpreterCodeMeteo(c.weather_code);
 
    document.getElementById("meteo-temp").textContent     = `${Math.round(c.temperature_2m)}°`;
    document.getElementById("meteo-desc").textContent     = desc;
    document.getElementById("meteo-humidity").textContent = `💧 ${c.relative_humidity_2m}%`;
    document.getElementById("meteo-wind").textContent     = `💨 ${Math.round(c.wind_speed_10m)} km/h`;
    document.getElementById("meteo-feels").textContent    = `🌡️ Ressenti ${Math.round(c.apparent_temperature)}°`;
  } catch(e) {
    document.getElementById("meteo-desc").textContent = "Météo indisponible";
  }
}
 
/* Convertit les codes WMO en description française */
function interpreterCodeMeteo(code) {
  const codes = {
    0:"Ciel dégagé", 1:"Principalement dégagé", 2:"Partiellement nuageux", 3:"Couvert",
    45:"Brouillard", 48:"Brouillard givrant",
    51:"Bruine légère", 53:"Bruine modérée", 55:"Bruine dense",
    61:"Pluie légère", 63:"Pluie modérée", 65:"Pluie forte",
    71:"Neige légère", 73:"Neige modérée", 75:"Neige forte",
    80:"Averses légères", 81:"Averses modérées", 82:"Averses violentes",
    95:"Orageux", 96:"Orage avec grêle", 99:"Orage violent"
  };
  return codes[code] || "Variable";
}
 
/* ===== 6. RÉCUPÉRER UN FLUX RSS ===== */
async function fetchFeed(source) {
  const url = `${RSS2JSON}${encodeURIComponent(source.rss)}&count=${MAX_ART}`;
  try {
    const res  = await fetch(url);
    if (!res.ok) return [];
    const data = await res.json();
    if (data.status !== "ok" || !data.items) return [];
    return data.items.map(item => ({
      title:      item.title      || "Sans titre",
      description:item.description|| "",
      link:       item.link       || "#",
      thumbnail:  item.thumbnail  || item.enclosure?.link || "",
      pubDate:    item.pubDate    || "",
      sourceName: source.name,
      sourceColor:source.color,
      sourceIcon: source.icon,
      category:   source.category
    }));
  } catch(e) {
    return [];
  }
}
 
/* ===== 7. NETTOIE LE HTML ===== */
function stripHTML(html) {
  const d = document.createElement("div");
  d.innerHTML = html;
  return d.textContent || d.innerText || "";
}
 
/* ===== 8. FORMATE LA DATE ===== */
function formatDate(str) {
  if (!str) return "";
  const d = new Date(str);
  if (isNaN(d)) return "";
  return d.toLocaleDateString("fr-FR", { day:"numeric", month:"short", year:"numeric" });
}
 
/* ===== 9. BRIEFING DU JOUR ===== */
function afficherBriefing(articles) {
  const liste = document.getElementById("briefing-list");
  // Prend les 5 articles les plus récents de l'actu générale
  const tops = articles
    .filter(a => a.category === "actu")
    .slice(0, 5);
 
  if (tops.length === 0) {
    liste.innerHTML = '<li class="briefing-loading">Aucune actu disponible</li>';
    return;
  }
 
  liste.innerHTML = tops.map((a, i) => `
    <a class="briefing-item" href="${a.link}" target="_blank" rel="noopener">
      <span class="briefing-num">${String(i+1).padStart(2,"0")}</span>
      <span>${a.title}</span>
    </a>
  `).join("");
}
 
/* ===== 10. CRÉER UNE CARTE ===== */
function creerCarte(article) {
  const card = document.createElement("a");
  card.className = "news-card";
  card.href      = article.link;
  card.target    = "_blank";
  card.rel       = "noopener noreferrer";
 
  const desc = stripHTML(article.description).trim().substring(0, 110);
 
  card.innerHTML = `
    ${article.thumbnail
      ? `<img class="card-image" src="${article.thumbnail}" alt="" loading="lazy" onerror="this.style.display='none'" />`
      : ""}
    <div class="card-body">
      <span class="card-source" style="background:${article.sourceColor}">
        ${article.sourceIcon} ${article.sourceName}
      </span>
      <h2 class="card-title">${article.title}</h2>
      ${desc ? `<p class="card-desc">${desc}…</p>` : ""}
      <span class="card-date">${formatDate(article.pubDate)}</span>
    </div>
  `;
  return card;
}
 
/* ===== 11. AFFICHER LES ARTICLES ===== */
function afficherArticles() {
  const grid = document.getElementById("news-grid");
  grid.innerHTML = "";
 
  const filtered = currentFilter === "all"
    ? allArticles
    : allArticles.filter(a => a.category === currentFilter);
 
  if (filtered.length === 0) {
    grid.innerHTML = `<p style="color:var(--text-secondary);grid-column:1/-1;text-align:center;padding:40px 0">
      Aucun article dans cette catégorie.</p>`;
    return;
  }
 
  filtered.forEach(a => grid.appendChild(creerCarte(a)));
}
 
/* ===== 12. FILTRES ===== */
function setupFiltres() {
  document.querySelectorAll(".filter-btn").forEach(btn => {
    btn.addEventListener("click", () => {
      currentFilter = btn.dataset.filter;
      document.querySelectorAll(".filter-btn").forEach(b => b.classList.remove("active"));
      btn.classList.add("active");
      afficherArticles();
    });
  });
}
 
/* ===== 13. TRIER PAR DATE ===== */
function trierParDate(articles) {
  return articles.sort((a,b) => new Date(b.pubDate) - new Date(a.pubDate));
}
 
/* ===== 14. MISE À JOUR COMPTEURS ===== */
function majCompteurs(articles, sourcesReussies) {
  document.getElementById("article-count").textContent = articles.length;
  document.getElementById("stat-sources").textContent  = sourcesReussies;
  document.getElementById("stat-articles").textContent = articles.length;
}
 
/* ===== 15. FONCTION PRINCIPALE ===== */
async function init() {
  const loader        = document.getElementById("loader");
  const loaderTotal   = document.getElementById("loader-total");
  const loaderProgress= document.getElementById("loader-progress");
 
  loaderTotal.textContent = SOURCES.length;
  loader.classList.remove("hidden");
 
  let done = 0;
  let sourcesReussies = 0;
 
  // Charge toutes les sources en parallèle
  // mais met à jour le compteur au fur et à mesure
  const promises = SOURCES.map(source =>
    fetchFeed(source).then(articles => {
      done++;
      loaderProgress.textContent = done;
      if (articles.length > 0) sourcesReussies++;
      return articles;
    })
  );
 
  const results = await Promise.all(promises);
  const articles = results.flat();
 
  loader.classList.add("hidden");
 
  allArticles = trierParDate(articles);
 
  majCompteurs(allArticles, sourcesReussies);
  afficherBriefing(allArticles);
  afficherArticles();
}
 
/* ===== 16. DÉMARRAGE ===== */
document.addEventListener("DOMContentLoaded", () => {
  afficherDate();
  chargerMeteo();
  setupFiltres();
  init();
});
 

