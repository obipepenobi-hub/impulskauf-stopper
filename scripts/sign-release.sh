#!/usr/bin/env bash
# Signs the UNSIGNED release APK with the new private release key plus an Android key-rotation
# lineage (proof that the old key hands over to the new one), then verifies the result.
#
# Why two keys: devices that have this app installed with the OLD key (whose private part is public
# - it has been committed to this repo since the first release) can only update to an APK that
# proves it is the old key's legitimate successor. Android 9-12 and 13+ read that proof from the
# v3 / v3.1 signature; Android 8 does not know key rotation at all and keeps using the old key
# for the v2 signature. Once a device runs an APK signed by the new key, an APK signed only with
# the old key can no longer replace it (the lineage does not grant the rollback capability).
#
# Usage: RELEASE_KEYSTORE_FILE=... RELEASE_KEYSTORE_PASSWORD=... \
#          scripts/sign-release.sh <unsigned.apk> <signed-output.apk>
set -euo pipefail

in_apk="${1:?usage: sign-release.sh <unsigned.apk> <signed-output.apk>}"
out_apk="${2:?usage: sign-release.sh <unsigned.apk> <signed-output.apk>}"
: "${RELEASE_KEYSTORE_FILE:?path to the release keystore (.p12) is required}"
: "${RELEASE_KEYSTORE_PASSWORD:?password of the release keystore is required}"
export RELEASE_KEYSTORE_PASSWORD

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/android-build-tools.sh"

aligned="$(mktemp -u)-aligned.apk"
trap 'rm -f "$aligned" "$out_apk.idsig"' EXIT

"$zipalign" -p -f 4 "$in_apk" "$aligned"

# The first signer is the OLD key (needed for the v2 signature on Android 8 and for the v3 block of
# Android 9-12), the second one the NEW key. --rotation-min-sdk-version 33 is apksigner's default
# (the v3.1 block, Android 13+); it is passed explicitly so a different default in some future
# apksigner cannot silently change which Android versions get the new key.
"$apksigner" sign \
  --ks "$repo_root/keystore/shared-debug.keystore" --ks-key-alias androiddebugkey --ks-pass pass:android \
  --next-signer \
  --ks "$RELEASE_KEYSTORE_FILE" --ks-key-alias "${RELEASE_KEY_ALIAS:-impulskauf}" --ks-pass env:RELEASE_KEYSTORE_PASSWORD \
  --lineage "$repo_root/keystore/lineage.bin" \
  --min-sdk-version 26 \
  --rotation-min-sdk-version 33 \
  --v4-signing-enabled false \
  --out "$out_apk" "$aligned"

bash "$repo_root/scripts/verify-release.sh" "$out_apk"
