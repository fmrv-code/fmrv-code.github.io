
// =============================================
// auth.js — Protection par mot de passe
// Version corrigée pour Deno (Netlify Edge)
// =============================================
 
export default async function auth(request, context) {
 
  // ===== TON MOT DE PASSE ICI =====
  const USERNAME = "FM";
  const PASSWORD = "azerty"; // ← remplace par ton vrai mot de passe
 
  // Récupère l'en-tête Authorization
  const authHeader = request.headers.get("Authorization");
 
  // Fonction pour décoder le Base64 (compatible Deno)
  function decodeBase64(str) {
    const binary = atob(str);
    return binary;
  }
 
  // Si pas d'en-tête → demande la connexion
  if (!authHeader || !authHeader.startsWith("Basic ")) {
    return new Response("Accès refusé.", {
      status: 401,
      headers: {
        "WWW-Authenticate": 'Basic realm="NewsFlow"',
      },
    });
  }
 
  try {
    // Décode les identifiants
    const base64 = authHeader.replace("Basic ", "");
    const decoded = decodeBase64(base64);
    const colonIndex = decoded.indexOf(":");
    const user = decoded.substring(0, colonIndex);
    const pass = decoded.substring(colonIndex + 1);
 
    // Vérifie les identifiants
    if (user === USERNAME && pass === PASSWORD) {
      return context.next(); // ✅ Accès autorisé
    }
  } catch (e) {
    // Erreur de décodage
  }
 
  // ❌ Mauvais identifiants
  return new Response("Identifiants incorrects.", {
    status: 401,
    headers: {
      "WWW-Authenticate": 'Basic realm="NewsFlow"',
    },
  });
}
 
export const config = {
  path: "/*",
};

