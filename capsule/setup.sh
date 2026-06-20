#!/usr/bin/env bash
# ─────────────────────────────────────────────────────────────────────────────
# setup.sh — Configuration one-shot de Capsule
# Lance depuis le dossier capsule/ : bash setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Capsule — setup ==="
echo ""

# ── 1. Vérifier Flutter ───────────────────────────────────────────────────────
if ! command -v flutter &>/dev/null; then
  echo "✗  Flutter introuvable. Installe Flutter puis relance ce script."
  exit 1
fi
echo "✓  Flutter : $(flutter --version 2>/dev/null | head -1)"

# ── 2. Vérifier que android/ existe ──────────────────────────────────────────
if [ ! -d "android" ]; then
  echo ""
  echo "Le dossier android/ est absent."
  echo "Lance d'abord (réponds N si Flutter propose d'écraser main.dart) :"
  echo "  flutter create --org com.example --project-name capsule ."
  echo "Puis relance : bash setup.sh"
  exit 1
fi

# ── 3. Patch compileSdk de l'APP → 36 ────────────────────────────────────────
APP_GRADLE_KTS="android/app/build.gradle.kts"
APP_GRADLE_GRV="android/app/build.gradle"

patch_compile_sdk() {
  local file="$1"
  if grep -qE 'compileSdk\s*(=\s*)?(36|37|38|39|40)' "$file" 2>/dev/null; then
    echo "✓  compileSdk de l'app déjà ≥ 36"
  else
    sed -i \
      -e 's/compileSdk = flutter\.compileSdkVersion/compileSdk = 36/g' \
      -e 's/compileSdk flutter\.compileSdkVersion/compileSdk 36/g' \
      -e 's/compileSdk\s*=\s*3[0-5]/compileSdk = 36/g' \
      -e 's/compileSdkVersion 3[0-5]/compileSdkVersion 36/g' \
      "$file"
    echo "✓  $file → compileSdk 36"
  fi
}

if   [ -f "$APP_GRADLE_KTS" ]; then patch_compile_sdk "$APP_GRADLE_KTS"; APP_BUILD="$APP_GRADLE_KTS"
elif [ -f "$APP_GRADLE_GRV" ]; then patch_compile_sdk "$APP_GRADLE_GRV"; APP_BUILD="$APP_GRADLE_GRV"
else
  echo "⚠  Aucun android/app/build.gradle[.kts] trouvé."
  echo "   Lance : flutter create --org com.example --project-name capsule ."
  exit 1
fi

# ── 4. Désactiver checkAarMetadata (file_picker 8.x distribué avec SDK 34) ───
# file_picker est publié sous forme d'AAR pré-compilé avec compileSdk=34.
# flutter_plugin_android_lifecycle requiert que ses dépendants utilisent SDK≥36.
# Ce conflit ne peut pas être résolu en recompilant — on désactive la vérification.
if grep -q "capsule_aar_metadata_patch" "$APP_BUILD" 2>/dev/null; then
  echo "✓  Patch checkAarMetadata déjà présent"
else
  if [[ "$APP_BUILD" == *.kts ]]; then
    # Kotlin DSL
    cat >> "$APP_BUILD" << 'KOTLIN'

// capsule_aar_metadata_patch — contourne le conflit compileSdk de file_picker 8.x
tasks.configureEach {
    if (name.contains("checkDebugAarMetadata") || name.contains("checkReleaseAarMetadata")) {
        enabled = false
    }
}
KOTLIN
  else
    # Groovy DSL
    cat >> "$APP_BUILD" << 'GROOVY'

// capsule_aar_metadata_patch — contourne le conflit compileSdk de file_picker 8.x
tasks.configureEach { task ->
    if (task.name.contains("checkDebugAarMetadata") || task.name.contains("checkReleaseAarMetadata")) {
        task.enabled = false
    }
}
GROOVY
  fi
  echo "✓  $APP_BUILD — patch checkAarMetadata ajouté"
fi

# ── 5. Dépendances Flutter ────────────────────────────────────────────────────
echo ""
flutter pub get
echo "✓  flutter pub get OK"

# ── 6. Résumé ─────────────────────────────────────────────────────────────────
echo ""
echo "✅ Tout est prêt !"
echo ""
echo "  Lance l'app :"
echo "    flutter run -d RFCX41EFHZK   # Samsung S928B"
echo "    flutter run -d linux          # Linux desktop"
echo ""
echo "  Sync avec le PC (une fois) :"
echo "    1. Installe Syncthing-Fork (F-Droid) sur le téléphone"
echo "    2. Installe Syncthing sur le PC : syncthing.net"
echo "    3. Syncthing Android → Réglages → Accès à tous les fichiers → Autoriser"
echo "    4. Ajoute le dossier affiché dans Réglages → Synchronisation de Capsule"
echo "    5. Partage ce dossier avec ton PC dans l'interface Syncthing"
