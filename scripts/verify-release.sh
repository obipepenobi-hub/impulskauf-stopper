#!/usr/bin/env bash
# Checks that a signed release APK really carries the signatures we expect, so a wrong or missing
# secret can never ship an APK signed with the wrong key.
#
#   Android 13+  (v3.1 block)  -> the NEW release key is the signer
#   Android 8-12               -> the OLD key (key rotation exists from Android 9, but it is only
#                                 applied from Android 13 on); the APK must still verify there
#
# When a check fails, the apksigner output it was based on is printed (public certificate data
# only), so a failure can be diagnosed from the CI log.
#
# Usage: scripts/verify-release.sh <signed.apk>
set -euo pipefail

apk="${1:?usage: verify-release.sh <signed.apk>}"

# SHA-256 fingerprints of the two signing certificates (public information).
OLD_CERT_SHA256="bda870ace1b90382049581b0901b8fec0225914093d2b19b92784b3d4536d0a0"
NEW_CERT_SHA256="bb9dc73af5f956742ffbef7ebbe68ad56fb1ce9318de41cd8a382244ce6906b3"

# First Android API level that must get the new key (33 = Android 13); keep in sync with
# --rotation-min-sdk-version in sign-release.sh.
ROTATION_MIN_SDK=33
ROTATION_ANDROID="Android 13"

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$repo_root/scripts/android-build-tools.sh"

output=""

fail() {
  echo "---- apksigner output ----" >&2
  echo "$output" >&2
  echo "--------------------------" >&2
  echo "::error::release verification failed: $1" >&2
  exit 1
}

# Runs `apksigner verify` with the given options and keeps its output in $output.
run_verify() {
  local what="$1"
  shift
  output="$("$apksigner" verify "$@" "$apk" 2>&1 | tr -d '\r')" || fail "APK does not verify for $what"
}

# Prints the SHA-256 digests of the signing certificates in $output that apply on Android API
# level $1. A rotated APK is listed with one block per SDK range:
#   build-tools 34-36:  "Signer (minSdkVersion=X, maxSdkVersion=Y) certificate SHA-256 digest: ..."
#   build-tools 37:     "V3.1 Signer: (minSdkVersion=X, maxSdkVersion=Y) certificate SHA-256 digest: ..."
# A verification limited to a range with a single signer is listed as
# "Signer #1 certificate SHA-256 digest: ...".
signer_digests() {
  local sdk="$1" line ranged_seen=0
  local ranged_re='^(V[0-9.]+ )?Signer:? \(minSdkVersion=([0-9]+), maxSdkVersion=([0-9]+)[^)]*\) certificate SHA-256 digest: ([0-9a-fA-F]{64})$'
  local plain_re='^Signer #[0-9]+ certificate SHA-256 digest: ([0-9a-fA-F]{64})$'
  local -a ranged=() plain=()
  while IFS= read -r line; do
    if [[ "$line" =~ $ranged_re ]]; then
      ranged_seen=1
      if (( BASH_REMATCH[2] <= sdk && sdk <= BASH_REMATCH[3] )); then
        ranged+=("${BASH_REMATCH[4],,}")
      fi
    elif [[ "$line" =~ $plain_re ]]; then
      plain+=("${BASH_REMATCH[1],,}")
    fi
  done <<< "$output"
  if (( ranged_seen )); then
    if (( ${#ranged[@]} > 0 )); then printf '%s\n' "${ranged[@]}"; fi
  else
    if (( ${#plain[@]} > 0 )); then printf '%s\n' "${plain[@]}"; fi
  fi
}

# Fails unless apksigner lists at least one signer for API level $1 and all of them are $2.
expect_signer() {
  local sdk="$1" expected="$2" what="$3" digest
  local -a found=()
  mapfile -t found < <(signer_digests "$sdk")
  (( ${#found[@]} > 0 )) || fail "no signer for $what found in apksigner's output"
  for digest in "${found[@]}"; do
    [ "$digest" = "$expected" ] || fail "$what is signed by $digest, not by the expected key $expected"
  done
}

run_verify "$ROTATION_ANDROID+" -v --print-certs --min-sdk-version "$ROTATION_MIN_SDK"
grep -q "Verified using v3.1 scheme (APK Signature Scheme v3.1): true" <<< "$output" ||
  fail "no v3.1 (key rotation) signature"
expect_signer "$ROTATION_MIN_SDK" "$NEW_CERT_SHA256" "$ROTATION_ANDROID+ (the new release key)"

run_verify "Android 9-12" --print-certs --min-sdk-version 28 --max-sdk-version 32
expect_signer 30 "$OLD_CERT_SHA256" "Android 9-12 (the old key)"

run_verify "Android 8" --print-certs --min-sdk-version 26 --max-sdk-version 27
expect_signer 26 "$OLD_CERT_SHA256" "Android 8 (the old key)"

echo "release signature OK: new key on $ROTATION_ANDROID+ (API $ROTATION_MIN_SDK+), old key below"
