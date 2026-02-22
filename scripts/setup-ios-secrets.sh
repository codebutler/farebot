#!/usr/bin/env bash
set -euo pipefail

# Setup GitHub secrets for iOS TestFlight CI
# Uses fastlane to create the distribution cert and provisioning profile,
# then uploads everything as GitHub repo secrets via gh.
#
# Usage: ./scripts/setup-ios-secrets.sh

APP_IDENTIFIER="com.codebutler.farebot"
TEAM_ID="ZJ9GEQ36AH"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

info()  { echo -e "${GREEN}==>${NC} ${BOLD}$*${NC}"; }
warn()  { echo -e "${YELLOW}warning:${NC} $*"; }
error() { echo -e "${RED}error:${NC} $*" >&2; }
die()   { error "$@"; exit 1; }

WORK_DIR=""
cleanup() {
  if [[ -n "$WORK_DIR" && -d "$WORK_DIR" ]]; then
    rm -rf "$WORK_DIR"
  fi
}
trap cleanup EXIT

# --- Pre-flight checks ---

command -v gh >/dev/null 2>&1 || die "gh CLI not found. Install from https://cli.github.com"
gh auth status >/dev/null 2>&1 || die "Not authenticated with gh. Run: gh auth login"

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null) \
  || die "Not in a GitHub repository or unable to determine repo"

if ! command -v fastlane >/dev/null 2>&1; then
  info "Installing fastlane..."
  brew install fastlane || die "Failed to install fastlane. Install manually: brew install fastlane"
fi

echo ""
echo -e "${BOLD}iOS TestFlight Secret Setup${NC}"
echo -e "Repository: ${GREEN}${REPO}${NC}"
echo -e "Bundle ID:  ${GREEN}${APP_IDENTIFIER}${NC}"
echo -e "Team ID:    ${GREEN}${TEAM_ID}${NC}"
echo ""

WORK_DIR=$(mktemp -d)

prompt_file() {
  local description="$1"
  local var_name="$2"
  local extensions="$3"

  while true; do
    echo -ne "${BOLD}${description}${NC} (${extensions}): "
    read -r filepath
    filepath="${filepath/#\~/$HOME}"

    if [[ -z "$filepath" ]]; then
      error "Path cannot be empty"
      continue
    fi
    if [[ ! -f "$filepath" ]]; then
      error "File not found: $filepath"
      continue
    fi

    eval "$var_name=\"$filepath\""
    return 0
  done
}

prompt_text() {
  local description="$1"
  local var_name="$2"

  while true; do
    echo -ne "${BOLD}${description}${NC}: "
    read -r value

    if [[ -z "$value" ]]; then
      error "Value cannot be empty"
      continue
    fi

    eval "$var_name=\"$value\""
    return 0
  done
}

set_secret() {
  local name="$1"
  local value="$2"

  echo -n "$value" | gh secret set "$name" --repo "$REPO" 2>/dev/null \
    || die "Failed to set secret $name"
  echo -e "  ${GREEN}✓${NC} $name"
}

# --- Collect Apple ID ---

prompt_text "Apple ID (email)" APPLE_ID
echo ""

# --- Step 1: Distribution Certificate ---

echo -e "${BOLD}Step 1: Distribution Certificate${NC}"
echo "   fastlane will create (or fetch existing) Apple Distribution certificate."
echo "   You'll be prompted to sign in with your Apple ID."
echo ""

KEYCHAIN_PATH=$(security default-keychain -d user | tr -d '"' | xargs)

info "Running fastlane cert..."
fastlane cert \
  --username "$APPLE_ID" \
  --development false \
  --team_id "$TEAM_ID" \
  --output_path "$WORK_DIR" \
  --keychain_path "$KEYCHAIN_PATH" \
  || die "fastlane cert failed. Check your Apple Developer account."

# fastlane only writes a .p12 when creating a NEW cert. When reusing an
# existing one it just verifies it's in the keychain. Either way, export
# the distribution identity from the keychain ourselves.
P12_PATH="$WORK_DIR/distribution.p12"
P12_PASSWORD=$(openssl rand -base64 24)

FASTLANE_P12=$(find "$WORK_DIR" -name '*.p12' -print -quit)
if [[ -n "$FASTLANE_P12" && -f "$FASTLANE_P12" ]]; then
  # New cert — fastlane wrote the .p12 (empty password). Re-export with our password.
  mv "$FASTLANE_P12" "$P12_PATH"
  P12_PASSWORD=""
else
  # Existing cert — export from keychain
  info "Exporting certificate from keychain..."
  security export \
    -k "$KEYCHAIN_PATH" \
    -t identities \
    -f pkcs12 \
    -P "$P12_PASSWORD" \
    -o "$P12_PATH" \
    || die "Failed to export certificate from keychain. You may be prompted for your keychain password."

  [[ -s "$P12_PATH" ]] || die "Exported .p12 is empty"
fi

echo -e "  ${GREEN}✓${NC} Distribution certificate ready"
echo ""

# --- Step 2: Provisioning Profile ---

echo -e "${BOLD}Step 2: App Store Provisioning Profile${NC}"
echo "   fastlane will create (or fetch existing) App Store provisioning profile."
echo ""

info "Running fastlane sigh..."
PROFILE_PATH=$(fastlane sigh \
  --username "$APPLE_ID" \
  --app_identifier "$APP_IDENTIFIER" \
  --team_id "$TEAM_ID" \
  --output_path "$WORK_DIR" \
  --filename "AppStore.mobileprovision" \
  2>&1 | tee /dev/stderr | grep -o '/.*\.mobileprovision' | tail -1) \
  || true

# Fallback: find it in output dir
if [[ -z "$PROFILE_PATH" || ! -f "$PROFILE_PATH" ]]; then
  PROFILE_PATH=$(find "$WORK_DIR" -name '*.mobileprovision' -print -quit)
fi

[[ -n "$PROFILE_PATH" && -f "$PROFILE_PATH" ]] \
  || die "fastlane sigh failed. Check your Apple Developer account and bundle ID ($APP_IDENTIFIER)."

echo -e "  ${GREEN}✓${NC} Provisioning profile ready"
echo ""

# --- Step 3: App Store Connect API Key ---

echo -e "${BOLD}Step 3: App Store Connect API Key${NC}"
echo "   This must be created manually (no API for it)."
echo "   Go to: https://appstoreconnect.apple.com/access/integrations/api"
echo "   Create a key with App Manager (or Admin) role, then download the .p8 file."
echo ""
prompt_text "API Key ID (e.g. ABC123DEFG)" API_KEY_ID
prompt_text "Issuer ID (e.g. 12345678-1234-...)" ISSUER_ID
prompt_file "Path to .p8 key file" P8_PATH "AuthKey_*.p8"
echo ""

# --- Encode files ---

info "Encoding files..."
CERT_B64=$(base64 -i "$P12_PATH")
PROFILE_B64=$(base64 -i "$PROFILE_PATH")
KEY_B64=$(base64 -i "$P8_PATH")

# --- Confirm ---

echo ""
echo -e "${BOLD}Ready to set 6 secrets on ${GREEN}${REPO}${NC}${BOLD}:${NC}"
echo "  APPLE_CERTIFICATE_BASE64          ($(wc -c < "$P12_PATH" | tr -d ' ') bytes)"
echo "  APPLE_CERTIFICATE_PASSWORD        (****)"
echo "  APPLE_PROVISIONING_PROFILE_BASE64 ($(wc -c < "$PROFILE_PATH" | tr -d ' ') bytes)"
echo "  APP_STORE_CONNECT_API_KEY_ID      ($API_KEY_ID)"
echo "  APP_STORE_CONNECT_ISSUER_ID       ($ISSUER_ID)"
echo "  APP_STORE_CONNECT_API_KEY_BASE64  ($(wc -c < "$P8_PATH" | tr -d ' ') bytes)"
echo ""
echo -ne "${BOLD}Proceed? [y/N]${NC} "
read -r confirm
[[ "$confirm" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

# --- Set secrets ---

echo ""
info "Setting secrets..."
set_secret "APPLE_CERTIFICATE_BASE64"          "$CERT_B64"
set_secret "APPLE_CERTIFICATE_PASSWORD"        "$P12_PASSWORD"
set_secret "APPLE_PROVISIONING_PROFILE_BASE64" "$PROFILE_B64"
set_secret "APP_STORE_CONNECT_API_KEY_ID"      "$API_KEY_ID"
set_secret "APP_STORE_CONNECT_ISSUER_ID"       "$ISSUER_ID"
set_secret "APP_STORE_CONNECT_API_KEY_BASE64"  "$KEY_B64"

echo ""
echo -e "${GREEN}Done!${NC} All 6 secrets set on ${BOLD}${REPO}${NC}."
echo ""
echo "To trigger a release, push a version tag:"
echo -e "  ${BOLD}git tag v1.0.0 && git push origin v1.0.0${NC}"
