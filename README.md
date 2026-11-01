# Widget Studio

Application Android native (Kotlin, Jetpack Compose, Jetpack Glance) pour créer et personnaliser des widgets d'écran d'accueil, avec les codes visuels de One UI (Samsung). Interface en français.

## Fonctionnalités

- **Raccourci d'application** : un widget qui lance n'importe quelle application installée.
- **Horloge et date** : heure animée par le système (sans réveiller l'application), format 12 / 24 h, date optionnelle. Un appui peut ouvrir une application au choix.
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
- Aucune permission demandée ; l'application n'utilise pas Internet.

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
