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
echo "✓  Flutter : $(flutter --version --machine 2>/dev/null | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d.get("frameworkVersion","?"))' 2>/dev/null || flutter --version | head -1)"

# ── 2. Vérifier que l'android/ existe ────────────────────────────────────────
if [ ! -d "android" ]; then
  echo ""
  echo "Le dossier android/ est absent."
  echo "Lance d'abord : flutter create --org com.example --project-name capsule ."
  echo "Réponds N si Flutter propose d'écraser main.dart."
  echo "Puis relance : bash setup.sh"
  exit 1
fi

# ── 3. Patch compileSdk → 36 (requis par file_picker 8.x) ────────────────────
GRADLE="android/app/build.gradle"
if grep -qE 'compileSdk\s*(=\s*)?(36|37|38|39|40)' "$GRADLE" 2>/dev/null; then
  echo "✓  compileSdk déjà ≥ 36"
else
  sed -i \
    -e 's/compileSdk flutter\.compileSdkVersion/compileSdk 36/g' \
    -e 's/compileSdk = flutter\.compileSdkVersion/compileSdk = 36/g' \
    -e 's/compileSdk\s*=\s*34/compileSdk = 36/g' \
    -e 's/compileSdk\s*=\s*35/compileSdk = 36/g' \
    -e 's/compileSdkVersion 34/compileSdkVersion 36/g' \
    -e 's/compileSdkVersion 35/compileSdkVersion 36/g' \
    "$GRADLE"
  echo "✓  android/app/build.gradle — compileSdk mis à 36"
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
echo "    1. Installe Syncthing sur téléphone (F-Droid : Syncthing-Fork)"
echo "    2. Installe Syncthing sur PC : https://syncthing.net"
echo "    3. Dans Syncthing Android → Réglages → Accès à tous les fichiers → Autoriser"
echo "    4. Ajoute le dossier affiché dans Réglages → Synchronisation de Capsule"
echo "    5. Partage ce dossier avec ton PC dans l'interface Syncthing"
