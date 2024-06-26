#!/bin/bash
set -ev

getVersion() {
  ./gradlew properties -q | grep -E "^version:" | awk '{print $2}' | tr -d '[:space:]'
}

removeSnapshots() {
  sed -i 's/-SNAPSHOT//' gradle.properties
}

commitRelease() {
  local APP_VERSION=$(getVersion)
  git commit -a -m "Update version for release"
  git tag -a "v${APP_VERSION}" -m "Tag release version"
}

bumpVersion() {
  echo "Bump version number"
  local APP_VERSION=$(getVersion | xargs)
  local SEMANTIC_REGEX='^([0-9]+)\.([0-9]+)(\.([0-9]+))?$'
  if [[ ${APP_VERSION} =~ ${SEMANTIC_REGEX} ]]; then
    if [[ ${BASH_REMATCH[4]} ]]; then
      currentVersion="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${BASH_REMATCH[4]}"
      nextVersion=$((BASH_REMATCH[4] + 1))
      nextVersion="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}.${nextVersion}"
    else
      currentVersion="${BASH_REMATCH[1]}.${BASH_REMATCH[1]}"
      nextVersion=$((BASH_REMATCH[2] + 1))
      nextVersion="${BASH_REMATCH[1]}.${nextVersion}"
    fi

    echo "Bumping version: ${currentVersion} -+-> ${nextVersion}"
    sed -i -E "s/^previousVersion(\s)?=.*/previousVersion=${currentVersion}/" gradle.properties
    nextVersion="${nextVersion}-SNAPSHOT"
    sed -i -E "s/^version(\s)?=.*/version=${nextVersion}/" gradle.properties
  else
    echo "No semantic version and therefore cannot publish to maven repository: '${APP_VERSION}'"
  fi
}

commitNextVersion() {
  git commit -a -m "Update version for release"
}

git config --global user.email "actions@github.com"
git config --global user.name "GitHub Actions"

echo "Deploying plugin to JetBrains Marketplace"
removeSnapshots

./gradlew publishPlugin

commitRelease
bumpVersion
commitNextVersion
git push --follow-tags