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

# ── 2. Vérifier que l'android/ existe ────────────────────────────────────────
if [ ! -d "android" ]; then
  echo ""
  echo "Le dossier android/ est absent."
  echo "Lance d'abord (réponds N si Flutter propose d'écraser main.dart) :"
  echo "  flutter create --org com.example --project-name capsule ."
  echo "Puis relance : bash setup.sh"
  exit 1
fi

# ── 3. Patch compileSdk → 36 (requis par file_picker 8.x) ────────────────────
# Flutter 3.22+ génère build.gradle.kts (Kotlin DSL)
# Anciennes versions génèrent build.gradle (Groovy DSL)
GRADLE_KTS="android/app/build.gradle.kts"
GRADLE_GRV="android/app/build.gradle"

patch_gradle() {
  local file="$1"
  if grep -qE 'compileSdk\s*(=\s*)?(36|37|38|39|40)' "$file" 2>/dev/null; then
    echo "✓  compileSdk déjà ≥ 36 dans $file"
    return
  fi
  sed -i \
    -e 's/compileSdk = flutter\.compileSdkVersion/compileSdk = 36/g' \
    -e 's/compileSdk flutter\.compileSdkVersion/compileSdk 36/g' \
    -e 's/compileSdk\s*=\s*34/compileSdk = 36/g' \
    -e 's/compileSdk\s*=\s*35/compileSdk = 36/g' \
    -e 's/compileSdkVersion 34/compileSdkVersion 36/g' \
    -e 's/compileSdkVersion 35/compileSdkVersion 36/g' \
    "$file"
  echo "✓  $file — compileSdk mis à 36"
}

if [ -f "$GRADLE_KTS" ]; then
  patch_gradle "$GRADLE_KTS"
elif [ -f "$GRADLE_GRV" ]; then
  patch_gradle "$GRADLE_GRV"
else
  echo "⚠  Aucun build.gradle[.kts] trouvé dans android/app/"
  echo "   Lance : flutter create --org com.example --project-name capsule ."
  echo "   Puis relance : bash setup.sh"
  exit 1
fi

# ── 4. Dépendances Flutter ────────────────────────────────────────────────────
echo ""
flutter pub get
echo "✓  flutter pub get OK"

# ── 5. Résumé ─────────────────────────────────────────────────────────────────
echo ""
echo "✅ Tout est prêt !"
echo ""
echo "  Lance l'app :"
echo "    flutter run -d RFCX41EFHZK   # Samsung S928B"
echo "    flutter run -d linux          # Linux desktop"
echo ""
echo "  Sync avec le PC (une fois) :"
echo "    1. Installe Syncthing-Fork (F-Droid) sur le téléphone"
echo "    2. Installe Syncthing sur le PC : https://syncthing.net"
echo "    3. Syncthing Android → Réglages → Accès à tous les fichiers → Autoriser"
echo "    4. Ajoute le dossier affiché dans Réglages → Synchronisation de Capsule"
echo "    5. Partage ce dossier avec ton PC dans l'interface Syncthing"
