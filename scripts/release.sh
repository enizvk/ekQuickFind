#!/usr/bin/env bash
# Builds a signed direct-download release and publishes it as a GitHub release. Installed direct copies
# pick it up on their next daily update check (releases/latest/download/update.json).
#
# Usage: scripts/release.sh <version> [options]
#   <version>          version name, for example 0.2.0
#   -n, --notes TEXT   release notes (shown in the app's Updates panel and on GitHub)
#   --code N           build number to use (default: the one in gradle.properties + 1)
#   --draft            create a draft release (phones don't see it until you publish it)
#   --dry-run          build into dist/ but don't publish or change gradle.properties
#   -y, --yes          don't ask before publishing
set -euo pipefail

cd "$(dirname "$0")/.."

REPO="$(sed -n 's/^ekquickfind.githubRepo=//p' gradle.properties)"

usage() { sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//'; }
fail() { echo "error: $*" >&2; exit 1; }

VERSION="" NOTES="" CODE="" DRAFT=false DRY=false YES=false
while [[ $# -gt 0 ]]; do
  case "$1" in
    -n|--notes) NOTES="${2:?--notes needs text}"; shift 2 ;;
    --code) CODE="${2:?--code needs a number}"; shift 2 ;;
    --draft) DRAFT=true; shift ;;
    --dry-run) DRY=true; shift ;;
    -y|--yes) YES=true; shift ;;
    -h|--help) usage; exit 0 ;;
    -*) usage; fail "unknown option $1" ;;
    *) VERSION="$1"; shift ;;
  esac
done

[[ -n "$VERSION" ]] || { usage; exit 1; }
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || fail "version must look like 1.2.3"
# Without the release key Gradle falls back to the debug key, and installed copies would refuse the update.
[[ -f keystore.properties ]] || fail "keystore.properties is missing; releases must be signed with the release key"

current_code=$(grep '^ekquickfind.versionCode=' gradle.properties | cut -d= -f2)
CODE="${CODE:-$((current_code + 1))}"
[[ "$CODE" =~ ^[0-9]+$ ]] || fail "--code must be a number"
TAG="v$VERSION"
NOTES="${NOTES:-ekQuickFind $VERSION}"
OUT="dist/$TAG"

if ! $DRY; then
  command -v gh >/dev/null || fail "GitHub CLI (gh) isn't installed"
  gh auth status >/dev/null 2>&1 || fail "gh isn't logged in (run: gh auth login)"
  gh repo view "$REPO" >/dev/null 2>&1 || fail "can't see $REPO with the current gh account ($(gh api user -q .login 2>/dev/null || echo unknown))"
  if gh release view "$TAG" --repo "$REPO" >/dev/null 2>&1; then fail "release $TAG already exists"; fi
fi

# The project builds with JDK 21.
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
"$java_bin" -version 2>&1 | grep -q 'version "21' || fail "JDK 21 is needed; point JAVA_HOME at it"

echo "Building ekQuickFind $VERSION (build $CODE)..."
./gradlew :app:updateFeedDirectRelease \
  -Pekquickfind.versionCode="$CODE" \
  -Pekquickfind.versionName="$VERSION" \
  -Pekquickfind.releaseNotes="$NOTES" \
  --console=plain -q

mkdir -p "$OUT"
APK="ekQuickFind-$CODE.apk"
cp "app/build/outputs/update/directRelease/$APK" app/build/outputs/update/directRelease/update.json "$OUT/"

sdk="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$(sed -n 's/^sdk.dir=//p' local.properties 2>/dev/null)}}"
signer=$(ls -d "$sdk"/build-tools/*/apksigner 2>/dev/null | tail -1 || true)
if [[ -n "$signer" ]]; then
  echo "Signed by: $("$signer" verify --print-certs "$OUT/$APK" 2>/dev/null | grep -m1 'certificate DN' | sed 's/.*DN: //')"
fi
echo "Built into $OUT:"
ls -l "$OUT" | tail -n +2

if $DRY; then
  echo "Dry run: nothing published, gradle.properties unchanged."
  exit 0
fi

if ! $YES; then
  read -r -p "Publish $TAG to github.com/$REPO$($DRAFT && echo ' as a draft')? [y/N] " answer
  [[ "$answer" =~ ^[Yy]$ ]] || { echo "Not published. The build stays in $OUT."; exit 0; }
fi

draft_flag=()
$DRAFT && draft_flag=(--draft)
gh release create "$TAG" "$OUT/$APK" "$OUT/update.json" \
  --repo "$REPO" --title "ekQuickFind $VERSION" --notes "$NOTES" ${draft_flag[@]+"${draft_flag[@]}"}

# Only now does this become the current version, so the next release counts up from it.
sed -e "s/^ekquickfind.versionCode=.*/ekquickfind.versionCode=$CODE/" \
  -e "s/^ekquickfind.versionName=.*/ekquickfind.versionName=$VERSION/" gradle.properties > gradle.properties.tmp
mv gradle.properties.tmp gradle.properties
echo "Published $TAG. gradle.properties is now at $VERSION (build $CODE)."
$DRAFT && echo "It's a draft: publish it on GitHub so phones can see it."
exit 0
