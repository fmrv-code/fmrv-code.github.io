// Ta clé API NewsAPI (remplace par la tienne)
const CLE_API = "27a4fdcd93ea459eb77152d0d7dafe83";

// On récupère les éléments de la page
const bouton = document.getElementById("bouton-recherche");
const champ = document.getElementById("champ-recherche");
const resultats = document.getElementById("resultats");

// Quand on clique sur Rechercher
bouton.addEventListener("click", () => {
  const sujet = champ.value.trim();
  if (sujet === "") {
    resultats.innerHTML = "<p class='message-accueil'>⚠️ Tape un sujet avant de rechercher !</p>";
    return;
  }
  rechercherActualites(sujet);
});

// Recherche aussi quand on appuie sur Entrée
champ.addEventListener("keypress", (e) => {
  if (e.key === "Enter") bouton.click();
});

// La fonction qui va chercher les actualités
async function rechercherActualites(sujet) {
  // Affiche un message de chargement
  resultats.innerHTML = "<p class='message-accueil'>⏳ Recherche en cours...</p>";

  try {
    // Appel à l'API NewsAPI
    const reponse = await fetch(
      `https://newsapi.org/v2/everything?q=${sujet}&language=fr&sortBy=publishedAt&pageSize=10&apiKey=${CLE_API}`
    );
    const donnees = await reponse.json();

    // Si aucun article trouvé
    if (donnees.articles.length === 0) {
      resultats.innerHTML = "<p class='message-accueil'>😕 Aucun article trouvé pour ce sujet.</p>";
      return;
    }

    // Affiche les articles
    resultats.innerHTML = "";
    donnees.articles.forEach((article) => {
      // Ignore les articles sans titre
      if (article.title === "[Removed]") return;

      const carte = document.createElement("div");
      carte.className = "carte-article";
      carte.innerHTML = `
        <h2>${article.title}</h2>
        <p>${article.description || "Pas de description disponible."}</p>
        <p class="source">📰 ${article.source.name} — ${new Date(article.publishedAt).toLocaleDateString("fr-FR")}</p>
        <a href="${article.url}" target="_blank">Lire l'article complet →</a>
      `;
      resultats.appendChild(carte);
    });

  } catch (erreur) {
    resultats.innerHTML = "<p class='message-accueil'>❌ Erreur de connexion. Vérifie ta clé API.</p>";
  }
}
