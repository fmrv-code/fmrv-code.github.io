
// =============================================
// auth.js — Protection par mot de passe
// Netlify Edge Function : s'exécute sur le
// serveur AVANT que la page soit envoyée.
// =============================================
 
export default async function auth(request, context) {
 
  // ===== TON MOT DE PASSE ICI =====
  // Change ces deux valeurs par les tiennes
  const USERNAME = "FM";
  const PASSWORD = "azerty"; // ← remplace par ton vrai mot de passe
 
  // Récupère l'en-tête Authorization envoyé par le navigateur
  const authHeader = request.headers.get("Authorization");
 
  // Si pas d'en-tête → demande au navigateur d'afficher la fenêtre de connexion
  if (!authHeader || !authHeader.startsWith("Basic ")) {
    return new Response("Accès refusé — Identifiez-vous.", {
      status: 401,
      headers: {
        // Cette ligne déclenche la fenêtre de connexion dans le navigateur
        "WWW-Authenticate": 'Basic realm="NewsFlow — Accès privé"',
        "Content-Type": "text/plain; charset=utf-8",
      },
    });
  }
 
  // Décode le nom d'utilisateur et mot de passe envoyés par le navigateur
  // Le navigateur les envoie en Base64 : "FM:MotDePasse" → encodé
  const base64 = authHeader.slice("Basic ".length);
  const decoded = atob(base64);                    // décode le Base64
  const [user, pass] = decoded.split(":");         // sépare user et password
 
  // Vérifie que user ET password correspondent
  if (user === USERNAME && pass === PASSWORD) {
    // ✅ Correct → laisse passer, affiche la page normalement
    return context.next();
  }
 
  // ❌ Mauvais mot de passe → re-demande les identifiants
  return new Response("Identifiants incorrects.", {
    status: 401,
    headers: {
      "WWW-Authenticate": 'Basic realm="NewsFlow — Accès privé"',
      "Content-Type": "text/plain; charset=utf-8",
    },
  });
}
 
// Dit à Netlify d'appliquer cette fonction sur TOUTES les pages du site
export const config = {
  path: "/*",
};

