// =====================
// SOURCES RSS FRANÇAISES
// =====================
const SOURCES = [
  { nom: "Frandroid",  url: "https://www.frandroid.com/feed" },
  { nom: "Le Monde",   url: "https://www.lemonde.fr/rss/une.xml" },
  { nom: "L'Équipe",   url: "https://www.lequipe.fr/rss/actu_rss.xml" },
  { nom: "01net",      url: "https://www.01net.com/feed/" },
  { nom: "Le Figaro",  url: "https://www.lefigaro.fr/rss/figaro_actualites.xml" },
];

// Proxy gratuit qui contourne le blocage CORS
const PROXY = "https://api.rss2json.com/v1/api.json?rss_url=";

// Éléments de la page
const bouton   = document.getElementById("bouton-recherche");
const champ    = document.getElementById("champ-recherche");
const resultats = document.getElementById("resultats");

// Clic sur Rechercher
bouton.addEventListener("click", () => {
  const sujet = champ.value.trim().toLowerCase();
  if (sujet === "") {
    resultats.innerHTML = "<p class='message-accueil'>⚠️ Tape un sujet avant de rechercher !</p>";
    return;
  }
  rechercherActualites(sujet);
});

// Touche Entrée
champ.addEventListener("keypress", (e) => {
  if (e.key === "Enter") bouton.click();
});

// Fonction principale
async function rechercherActualites(sujet) {
  resultats.innerHTML = "<p class='message-accueil'>⏳ Recherche en cours...</p>";

  let tousLesArticles = [];

  // On récupère les articles de chaque source
  for (const source of SOURCES) {
    try {
      const reponse = await fetch(PROXY + encodeURIComponent(source.url));
      const donnees = await reponse.json();

      if (donnees.items) {
        // On filtre les articles qui contiennent le sujet recherché
        const articlesFiltres = donnees.items.filter((article) => {
          const titre = (article.title || "").toLowerCase();
          const description = (article.description || "").toLowerCase();
          return titre.includes(sujet) || description.includes(sujet);
        });

        // On ajoute le nom de la source à chaque article
        articlesFiltres.forEach((a) => (a.sourcenom = source.nom));
        tousLesArticles = tousLesArticles.concat(articlesFiltres);
      }
    } catch (e) {
      console.log("Erreur avec " + source.nom);
    }
  }

  // Trie par date (plus récent en premier)
  tousLesArticles.sort((a, b) => new Date(b.pubDate) - new Date(a.pubDate));

  // Affichage
  if (tousLesArticles.length === 0) {
    resultats.innerHTML = "<p class='message-accueil'>😕 Aucun article trouvé pour « " + sujet + " ».</p>";
    return;
  }

  resultats.innerHTML = "";
  tousLesArticles.forEach((article) => {
    const carte = document.createElement("div");
    carte.className = "carte-article";
    carte.innerHTML = `
      ${article.thumbnail ? `<img src="${article.thumbnail}" alt="image" style="width:100%;border-radius:8px;margin-bottom:10px;object-fit:cover;max-height:200px;">` : ""}
      <h2>${article.title}</h2>
      <p>${article.description ? article.description.replace(/<[^>]+>/g, "").substring(0, 200) + "..." : "Pas de description."}</p>
      <p class="source">📰 ${article.sourcenom} — ${new Date(article.pubDate).toLocaleDateString("fr-FR")}</p>
      <a href="${article.link}" target="_blank">Lire l'article complet →</a>
    `;
    resultats.appendChild(carte);
  });
}
