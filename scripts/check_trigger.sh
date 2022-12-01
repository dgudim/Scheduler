#!/bin/bash

set -e
set -x

COMMIT_MSG=$(git log --no-merges -1 --oneline)

# The commit marker "[fastlane]" or "[metadata]" will trigger the build when required
if [[ "$GITHUB_EVENT_NAME" == schedule ||
      "$GITHUB_EVENT_NAME" == workflow_dispatch ||
      "$COMMIT_MSG" =~ \[fastlane\] ||
      "$COMMIT_MSG" =~ \[metadata\] ]]; then
    echo "::set-output name=validate::true"
fi
