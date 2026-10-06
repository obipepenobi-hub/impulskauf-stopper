#!/usr/bin/env bash
# Checks that a signed release APK really carries the signatures we expect, so a wrong or missing
# secret can never ship an APK signed with the wrong key.
#
#   Android 13+ (v3.1)  -> the NEW release key is the current signer
#   Android 8-12        -> the OLD key (key rotation only exists from Android 9, and apksigner
#                          applies it from Android 13 by default); the APK must still verify there
#
# Usage: scripts/verify-release.sh <signed.apk>
set -euo pipefail

apk="${1:?usage: verify-release.sh <signed.apk>}"

# SHA-256 fingerprints of the two signing certificates (public information).
OLD_CERT_SHA256="bda870ace1b90382049581b0901b8fec0225914093d2b19b92784b3d4536d0a0"
NEW_CERT_SHA256="bb9dc73af5f956742ffbef7ebbe68ad56fb1ce9318de41cd8a382244ce6906b3"

build_tools="$(ls -d "${ANDROID_HOME:?ANDROID_HOME is not set}/build-tools/"* | sort -V | tail -1)"
apksigner="${APKSIGNER:-$build_tools/apksigner}"

fail() {
  echo "::error::release verification failed: $1" >&2
  exit 1
}

modern="$("$apksigner" verify -v --print-certs --min-sdk-version 33 "$apk" 2>&1)" || fail "APK does not verify for Android 13+"
echo "$modern" | grep -q "Verified using v3.1 scheme (APK Signature Scheme v3.1): true" ||
  fail "no v3.1 (key rotation) signature"
# apksigner lists the signer blocks of every Android range; only the one that starts at 33 matters here.
modern_signer="Signer (minSdkVersion=33, maxSdkVersion=2147483647) certificate SHA-256 digest:"
echo "$modern" | grep -qi "$modern_signer $NEW_CERT_SHA256" ||
  fail "the new release key is not the signer on Android 13+"
if echo "$modern" | grep -qi "$modern_signer $OLD_CERT_SHA256"; then
  fail "the old (public) key is still the signer on Android 13+"
fi

middle="$("$apksigner" verify --print-certs --min-sdk-version 28 --max-sdk-version 32 "$apk" 2>&1)" ||
  fail "APK does not verify for Android 9-12"
echo "$middle" | grep -qi "certificate SHA-256 digest: $OLD_CERT_SHA256" ||
  fail "unexpected signer on Android 9-12"

legacy="$("$apksigner" verify --print-certs --min-sdk-version 26 --max-sdk-version 27 "$apk" 2>&1)" ||
  fail "APK does not verify for Android 8"
echo "$legacy" | grep -qi "certificate SHA-256 digest: $OLD_CERT_SHA256" ||
  fail "unexpected signer on Android 8"

echo "release signature OK: new key on Android 13+, old key (no rotation) below"
