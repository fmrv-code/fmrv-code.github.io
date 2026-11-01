# Widget Studio

Application Android native (Kotlin, Jetpack Compose, Jetpack Glance) pour créer des widgets d'écran d'accueil dans le style One UI de Samsung, avec un catalogue de modèles façon « galerie ». Interface en français.

## Catalogue

| Rubrique | Widgets |
|---|---|
| **Horloges** | numérique, empilée (heures/minutes en grand), à aiguilles (4 cadrans), mondiale (3 villes) |
| **Calendriers et dates** | calendrier du mois, semaine, grande date, compte à rebours, progression (jour, semaine, mois, année) |
| **Applications** | icône, pilule, dossier (8 apps), barre de recherche |
| **Informations** | Franceinfo, notes Joplin, Gmail |
| **Appareil** | entretien de l'appareil (stockage, mémoire, batterie) |

Chaque widget se personnalise avec un **aperçu fidèle** : style prédéfini (Bleu nuit, Corail, Verre clair/sombre, Lavande, Menthe, Noir, Blanc, Bleu One UI, Sans fond), fond uni, dégradé ou verre, couleurs, transparence, forme, police et réglages propres au widget. Limite : un widget ne peut pas flouter le fond d'écran ; le « verre » est un dégradé translucide.

## Architecture

Un widget est **un type + un style + des paramètres**. Le type décrit son contenu **une seule fois** sous forme d'arbre d'éléments (`render/Node.kt`) ; deux moteurs le dessinent : Glance sur l'écran d'accueil (`widget/GlanceRenderer.kt`) et Compose dans l'application (`ui/preview/ComposeRenderer.kt`). L'aperçu est donc identique au widget réel, et un nouveau widget ne s'écrit qu'une fois, en Kotlin pur testable sur JVM.

```
app/src/main/java/fmrv/widgetstudio/
├── domain/    modèles (WidgetInstance, WidgetStyle, Params), styles prédéfinis, interfaces
├── render/    arbre d'éléments (Node), couleurs, chargement d'images
├── kinds/     un fichier par famille : clocks/, calendar/, apps/, device/, feeds/ ; Kinds.kt = le catalogue
├── data/      Room (instances en JSON), DataStore, apps, Joplin/OneDrive, Franceinfo, Gmail
├── widget/    UniversalWidget (Glance), récepteurs, configuration, épinglage, actualisation
├── ui/        thème One UI, galerie, éditeur généré, aperçu, réglages
└── viewmodel/
```

**Ajouter un type de widget** : créer un objet qui implémente `WidgetKind` (déclare ses champs de réglage, charge ses données, produit son arbre), puis l'ajouter à `Kinds.all`. La galerie, l'éditeur et l'aperçu s'adaptent seuls.

### Fonctionnement des widgets

Android impose de déclarer chaque type de fournisseur dans le manifeste : on ne peut pas en créer un nouveau par widget. Widget Studio utilise donc **un seul `GlanceAppWidget`** exposé en cinq tailles (1×1, 2×2, 4×2, 4×1, 4×4). Chaque widget posé reçoit un identifiant du système auquel est associée une configuration stockée dans Room.

- À la pose, le lanceur ouvre `WidgetConfigActivity`. Tant que la configuration n'est pas enregistrée, le résultat reste `RESULT_CANCELED` et le lanceur retire le widget.
- Pour l'épinglage depuis l'application, la configuration est « en attente » (5 minutes maximum) puis attribuée au widget posé.
- Redessin : toutes les 30 minutes et juste après minuit (le jour courant d'un calendrier change sans ouvrir l'application) ; les sources distantes (notes, actualités, e-mails) toutes les ~30 minutes si le réseau est disponible.
- Les horloges sont animées par le système (`TextClock`, `AnalogClock`) sans réveiller l'application.

### Compatibilité

- `minSdk 30` (Android 11), `targetSdk 35`. Android 12+ : coins arrondis natifs ; Android 11 : repli par forme vectorielle teintée.
- Les polices personnalisées sont impossibles dans un widget : quatre familles système (sur Samsung, « Standard » = One UI Sans).
- Aucune permission demandée, sauf `INTERNET`, utilisée uniquement par les widgets qui lisent un service (Joplin via OneDrive, Franceinfo, Gmail).

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
