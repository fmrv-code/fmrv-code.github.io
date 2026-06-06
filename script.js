// Ta clé API GNews (remplace par la tienne)
const CLE_API = "2717812ee3e186f9d83085a61daef964";

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
  resultats.innerHTML = "<p class='message-accueil'>⏳ Recherche en cours...</p>";

  try {
    const reponse = await fetch(
      `https://gnews.io/api/v4/search?q=${encodeURIComponent(sujet)}&lang=fr&max=10&apikey=${CLE_API}`
    );
    const donnees = await reponse.json();

    if (!donnees.articles || donnees.articles.length === 0) {
      resultats.innerHTML = "<p class='message-accueil'>😕 Aucun article trouvé pour ce sujet.</p>";
      return;
    }

    resultats.innerHTML = "";
    donnees.articles.forEach((article) => {
      const carte = document.createElement("div");
      carte.className = "carte-article";
      carte.innerHTML = `
        ${article.image ? `<img src="${article.image}" alt="image article" style="width:100%;border-radius:8px;margin-bottom:10px;">` : ""}
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
