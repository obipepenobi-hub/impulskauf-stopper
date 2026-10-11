#!/usr/bin/env bash
# Sourced by sign-release.sh and verify-release.sh: selects the Android build-tools to use and sets
# $build_tools, $apksigner and $zipalign.
#
# Pinned on purpose. apksigner's text output (which verify-release.sh reads) differs between
# versions, and a changed default for key rotation would silently change which Android versions
# get the new key. The pinned version is the one this signing set-up was tested with; another one
# can be tried on purpose with BUILD_TOOLS_VERSION (the Release workflow's dry run has an input
# for it). If the pinned version is no longer on the runner image, the newest installed one is
# used instead and a warning is printed.
PINNED_BUILD_TOOLS_VERSION="36.0.0"

tools_root="${ANDROID_HOME:?ANDROID_HOME is not set}/build-tools"

if [ -n "${BUILD_TOOLS_VERSION:-}" ]; then
  if ! [[ "$BUILD_TOOLS_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
    echo "::error::BUILD_TOOLS_VERSION must look like 36.0.0, got: $BUILD_TOOLS_VERSION" >&2
    exit 1
  fi
  build_tools="$tools_root/$BUILD_TOOLS_VERSION"
  if [ ! -d "$build_tools" ]; then
    echo "::error::build-tools $BUILD_TOOLS_VERSION are not installed (found: $(ls "$tools_root" | tr '\n' ' '))" >&2
    exit 1
  fi
else
  build_tools="$tools_root/$PINNED_BUILD_TOOLS_VERSION"
  if [ ! -d "$build_tools" ]; then
    build_tools="$(ls -d "$tools_root/"* | sort -V | tail -1)"
    echo "::warning::build-tools $PINNED_BUILD_TOOLS_VERSION not installed, using $build_tools - run the release dry run and check the signature output" >&2
  fi
fi

apksigner="${APKSIGNER:-$build_tools/apksigner}"
zipalign="${ZIPALIGN:-$build_tools/zipalign}"
echo "using Android build-tools: $(basename "$build_tools")" >&2
