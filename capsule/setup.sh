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
patch_gradle() {
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

if   [ -f "android/app/build.gradle.kts" ]; then patch_gradle "android/app/build.gradle.kts"
elif [ -f "android/app/build.gradle" ];     then patch_gradle "android/app/build.gradle"
else
  echo "⚠  Aucun android/app/build.gradle[.kts] trouvé."
  echo "   Lance : flutter create --org com.example --project-name capsule ."
  echo "   Puis relance : bash setup.sh"
  exit 1
fi

# ── 4. Patch ROOT build.gradle → force compileSdk 36 pour TOUS les plugins ───
# file_picker 8.x est lui-même compilé avec SDK 34 mais dépend d'une lib
# qui exige SDK 36. Ce bloc applique SDK 36 à tous les sous-projets (plugins).
ROOT_GRADLE="android/build.gradle"
if [ -f "$ROOT_GRADLE" ]; then
  if grep -q "capsule_compileSdk_patch" "$ROOT_GRADLE"; then
    echo "✓  Patch plugins compileSdk déjà présent"
  else
    cat >> "$ROOT_GRADLE" << 'GROOVY'

// capsule_compileSdk_patch — force SDK 36 pour tous les plugins Flutter
subprojects {
    afterEvaluate { project ->
        if (project.plugins.hasPlugin("com.android.library") ||
            project.plugins.hasPlugin("com.android.application")) {
            project.android {
                compileSdkVersion 36
            }
        }
    }
}
GROOVY
    echo "✓  android/build.gradle — patch plugins compileSdk 36 ajouté"
  fi
else
  echo "⚠  android/build.gradle introuvable, patch plugins ignoré"
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
