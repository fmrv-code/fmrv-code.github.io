
// Protection par mot de passe — Netlify Edge Function
export default async function auth(request, context) {
 
  // ===== CHANGE CES DEUX VALEURS =====
  const USERNAME = "FM";
  const PASSWORD = "azerty"; // ← ton vrai mot de passe ici
  // ====================================
 
  const authHeader = request.headers.get("Authorization");
 
  if (!authHeader || !authHeader.startsWith("Basic ")) {
    return new Response("Accès refusé.", {
      status: 401,
      headers: { "WWW-Authenticate": 'Basic realm="NewsFlow"' },
    });
  }
 
  try {
    const base64   = authHeader.replace("Basic ", "");
    const decoded  = atob(base64);
    const colon    = decoded.indexOf(":");
    const user     = decoded.substring(0, colon);
    const pass     = decoded.substring(colon + 1);
 
    if (user === USERNAME && pass === PASSWORD) {
      return context.next(); // ✅ Accès autorisé
    }
  } catch(e) {}
 
  return new Response("Identifiants incorrects.", {
    status: 401,
    headers: { "WWW-Authenticate": 'Basic realm="NewsFlow"' },
  });
}
 
export const config = { path: "/*" };

