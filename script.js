const SOURCES = [
  { nom: "Franceinfo", url: "https://www.francetvinfo.fr/titres.rss" },
  { nom: "Le Monde",   url: "https://www.lemonde.fr/rss/une.xml" },
  { nom: "Le Figaro",  url: "https://www.lefigaro.fr/rss/figaro_actualites.xml" },
  { nom: "L'Équipe",   url: "https://www.lequipe.fr/rss/actu_rss.xml" },
  { nom: "Frandroid",  url: "https://www.frandroid.com/feed" },
  { nom: "01net",      url: "https://www.01net.com/feed/" },
  { nom: "Libération", url: "https://www.liberation.fr/arc/outboundfeeds/rss/" },
];

const PROXY = "https://api.rss2json.com/v1/api.json?rss_url=";

const bouton    = document.getElementById("bouton-recherche");
const champ     = document.getElementById("champ-recherche");
const resultats = document.getElementById("resultats");
const filtres   = document.querySelectorAll(".filtre-btn");

let tousLesArticles = [];
let sourceActive = "toutes";

// Gestion des filtres
filtres.forEach((btn) => {
  btn.addEventListener("click", () => {
    filtres.forEach((b) => b.classList.remove("actif"));
    btn.classList.add("actif");
    sourceActive = btn.dataset.source;
    afficherArticles();
  });
});

bouton.addEventListener("click", () => {
  const sujet = champ.value.trim().toLowerCase();
  if (sujet === "") {
    resultats.innerHTML = "<p class='message-accueil'>⚠️ Tape un sujet avant de rechercher !</p>";
    return;
  }
  rechercherActualites(sujet);
});

champ.addEventListener("keypress", (e) => {
  if (e.key === "Enter") bouton.click();
});

async function rechercherActualites(sujet) {
  resultats.innerHTML = "<p class='message-accueil'>⏳ Recherche en cours sur toutes les sources...</p>";
  tousLesArticles = [];

  for (const source of SOURCES) {
    try {
      const reponse = await fetch(PROXY + encodeURIComponent(source.url));
      const donnees = await reponse.json();

      if (donnees.items) {
        const filtres = donnees.items.filter((article) => {
          const titre = (article.title || "").toLowerCase();
          const desc  = (article.description || "").toLowerCase();
          return titre.includes(sujet) || desc.includes(sujet);
        });
        filtres.forEach((a) => (a.sourcenom = source.nom));
        tousLesArticles = tousLesArticles.concat(filtres);
      }
    } catch (e) {
      console.log("Erreur : " + source.nom);
    }
  }

  tousLesArticles.sort((a, b) => new Date(b.pubDate) - new Date(a.pubDate));
  afficherArticles();
}

function afficherArticles() {
  const articles = sourceActive === "toutes"
    ? tousLesArticles
    : tousLesArticles.filter((a) => a.sourcenom === sourceActive);

  if (articles.length === 0) {
    resultats.innerHTML = "<p class='message-accueil'>😕 Aucun article trouvé.</p>";
    return;
  }

  resultats.innerHTML = `<p class="compteur">✅ ${articles.length} article(s) trouvé(s)</p>`;

  articles.forEach((article) => {
    const carte = document.createElement("div");
    carte.className = "carte-article";
    carte.innerHTML = `
      ${article.thumbnail ? `<img src="${article.thumbnail}" alt="image">` : ""}
      <h2>${article.title}</h2>
      <p>${article.description ? article.description.replace(/<[^>]+>/g, "").substring(0, 200) + "..." : "Pas de description."}</p>
      <p class="source">📰 ${article.sourcenom} — ${new Date(article.pubDate).toLocaleDateString("fr-FR")}</p>
      <a href="${article.link}" target="_blank">Lire l'article complet →</a>
    `;
    resultats.appendChild(carte);
  });
}
