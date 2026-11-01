# Widget Studio

Application Android native (Kotlin, Jetpack Compose, Jetpack Glance) pour créer et personnaliser des widgets d'écran d'accueil, avec les codes visuels de One UI (Samsung). Interface en français.

## Fonctionnalités

- **Raccourci d'application** : un widget qui lance n'importe quelle application installée.
- **Horloge et date** : heure animée par le système (sans réveiller l'application), format 12 / 24 h, date optionnelle. Un appui peut ouvrir une application au choix.
- **Franceinfo** : les gros titres « À la une » (flux RSS public, sans compte). Un appui ouvre l'article.
- **Gmail** : vos e-mails non lus (expéditeur, objet), en lecture seule. Voir ci-dessous.
- **Notes Joplin** : liste des notes récentes de Joplin (qui n'a pas de widget), lue depuis OneDrive. Voir ci-dessous.
- **Éditeur visuel** avec aperçu en temps réel : type, application, nom, taille, forme (carré, arrondi, capsule), couleurs du fond et du texte, transparence, police.
- **Deux façons d'ajouter un widget** : depuis l'application (« Ajouter à l'écran d'accueil », épinglage système) ou depuis le menu des widgets du lanceur (« Widget Studio » → l'éditeur s'ouvre à la pose).
- **Mode sombre** : suivre le système, clair, sombre ou noir pur AMOLED ; couleurs dynamiques (Android 12+) en option.
- **One UI** : coins très arrondis, cartes flottantes, grands titres qui se replient, barre de navigation flottante, commandes principales dans la moitié basse de l'écran, retours haptiques discrets (désactivables).

## Architecture

MVVM avec séparation domaine / données / UI, injection avec Hilt.

```
app/src/main/java/fmrv/widgetstudio/
├── domain/      modèles (WidgetConfig…) et interfaces de repository
├── data/        Room (configs de widgets), DataStore (réglages), liste des apps, chargement des icônes
├── di/          modules Hilt
├── viewmodel/   un ViewModel par écran, état exposé en StateFlow
├── ui/          thème One UI, composants, écrans (home, apps, settings, editor), navigation
└── widget/      UniversalWidget (Glance), receivers, activité de configuration, épinglage, worker
```

### Fonctionnement des widgets

Android impose de déclarer chaque type de widget dans le manifeste : on ne peut pas en créer un nouveau par application. Widget Studio utilise donc **un seul `GlanceAppWidget` générique** (`UniversalWidget`), exposé en trois tailles (petit 1×1, moyen 2×2, large 4×2). Chaque widget posé reçoit un identifiant du système (`appWidgetId`) auquel est associée une `WidgetConfig` stockée dans Room.

- À la pose, le lanceur ouvre `WidgetConfigActivity` (`android:configure`). Tant que la configuration n'est pas enregistrée, le résultat reste `RESULT_CANCELED` et le lanceur retire le widget.
- Pour l'épinglage depuis l'application, la configuration est mise « en attente » (5 minutes maximum) puis attribuée au widget pose par l'activité de configuration, ou à défaut par `PinResultReceiver`.
- Les configurations des widgets supprimés sont nettoyées (`onDeleted` + réconciliation au démarrage).
- Un `WorkManager` périodique (6 h, batterie non faible) rafraîchit les icônes. Aucune alarme exacte, aucune permission spéciale.

### Compatibilité

- `minSdk 30` (Android 11), `targetSdk 35`.
- Android 12+ : coins arrondis natifs (`cornerRadius`). Android 11 : repli par forme vectorielle teintée (le rayon est alors étiré avec le widget).
- Les polices personnalisées ne sont pas possibles dans un widget (RemoteViews) : le choix se limite aux familles système (standard, serif, monospace, manuscrite). Sur Samsung, « standard » correspond à One UI Sans.
- Liste des applications via `<queries>` sur l'intent LAUNCHER : aucune permission `QUERY_ALL_PACKAGES`.
- Unique permission : `INTERNET`, utilisée uniquement pour les widgets de liste que vous créez (notes Joplin via OneDrive, actualités Franceinfo, e-mails Gmail). Sans widget de ce type, aucune requête réseau n'est émise.

## Widget de notes Joplin (OneDrive)

Android interdit à une application de lire les données d'une autre : les notes de Joplin sont dans sa base privée. Widget Studio lit donc **les fichiers que Joplin synchronise sur OneDrive** (un fichier Markdown par note dans `Apps/Joplin`), en **lecture seule** (permission Microsoft Graph `Files.Read`).

Prérequis : synchronisation Joplin vers OneDrive, **chiffrement de bout en bout désactivé** (les notes chiffrées ne sont pas lisibles).

**Configuration unique de la connexion** (gratuite, sans secret embarqué) :

1. Sur [portal.azure.com](https://portal.azure.com) : Microsoft Entra ID › Inscriptions d'applications › Nouvelle inscription.
2. Types de comptes pris en charge : « Comptes personnels Microsoft uniquement ».
3. URI de redirection : plateforme « Client mobile et de bureau », URI personnalisée `fmrv.widgetstudio://oauth`.
4. Authentification › « Autoriser les flux clients publics » : Oui.
5. Copier l'« ID d'application (client) », puis dans Widget Studio : Réglages › Joplin (OneDrive) › coller l'ID › « Se connecter à OneDrive ».
6. Créer un widget de type « Notes Joplin ».

Fonctionnement : OAuth 2.0 code d'autorisation + PKCE ; le jeton de rafraîchissement est chiffré avec une clé de l'Android Keystore. Le widget affiche un **cache local** (aucun accès réseau au moment du dessin) mis à jour toutes les ~30 minutes (WorkManager, réseau requis, batterie non faible) ou avec le bouton ⟳. Seuls les fichiers modifiés depuis la dernière actualisation sont retéléchargés. Les notes en conflit ou à la corbeille sont ignorées. Un appui sur une note tente de l'ouvrir dans Joplin (lien `joplin://x-callback-url/openNote`) et, à défaut, ouvre simplement Joplin.

Limites : 20 notes les plus récentes ; le titre du carnet n'est pas affiché ; aucune modification des notes depuis le widget.

## Widgets Franceinfo et Gmail

**Franceinfo** : lit le flux RSS public de la rédaction (`https://www.francetvinfo.fr/titres.rss`, avec une adresse de secours). Si le widget reste vide, l'adresse du flux a peut-être changé : elle se corrige dans Réglages › Franceinfo (bouton « Tester et actualiser »).

**Gmail** : connexion **IMAP en lecture seule** (`imap.gmail.com:993`, TLS avec vérification du certificat). Widget Studio ne fait que `EXAMINE INBOX`, `UID SEARCH UNSEEN` et `UID FETCH` des en-têtes (`BODY.PEEK`) : il ne modifie, ne marque ni ne supprime aucun message, et ne télécharge ni le corps ni les pièces jointes.

1. Activez la validation en 2 étapes sur votre compte Google.
2. Créez un mot de passe d'application sur [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords).
3. Réglages › Gmail : saisissez votre adresse et ce mot de passe (chiffré avec l'Android Keystore). Le mot de passe est refusé et effacé immédiatement s'il est incorrect.
4. Créez un widget de type « Gmail ».

À savoir : un mot de passe d'application donne en théorie accès à toute la boîte ; supprimez-le depuis votre compte Google pour révoquer l'accès. L'expéditeur et l'objet des e-mails non lus sont **visibles sur l'écran d'accueil** et mémorisés dans le stockage privé de l'application jusqu'à la déconnexion. Les comptes Google Workspace peuvent interdire les mots de passe d'application.

Pour les trois widgets de liste : cache local, actualisation toutes les ~30 minutes (WorkManager, réseau requis, batterie non faible) ou avec le bouton ⟳.

## Compiler

Prérequis : JDK 17+ et le SDK Android (compileSdk 35).

```bash
./gradlew testDebugUnitTest   # tests unitaires
./gradlew lintDebug           # lint
./gradlew assembleDebug       # APK de debug
```

La CI (`.github/workflows/android.yml`) exécute ces trois tâches à chaque push et publie l'APK de debug et les rapports.

## Limites connues

- Les widgets déjà posés ne peuvent pas être retirés depuis l'application (le système l'interdit) : maintenir le widget appuyé sur l'écran d'accueil.
- Les configurations ne sont pas sauvegardées dans le cloud (`allowBackup=false`) : les identifiants de widgets ne sont pas portables d'un téléphone à l'autre. Sur un nouveau téléphone, les widgets réinstallés affichent « Touchez pour configurer ».
