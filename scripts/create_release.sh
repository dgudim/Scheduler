cd ..

set -e
set -x

printf "\nBuilding debug apk\n"
./gradlew assembleDebug

printf "\nBuilding release apk\n"
./gradlew assembleRelease

VERSION_NAME=$(./gradlew -q :app:getVersionName --no-configuration-cache | tail -n 1)
VERSION_CODE=$(./gradlew -q :app:getVersionCode --no-configuration-cache | tail -n 1)

CHANGELOG=./fastlane/metadata/android/en-US/changelogs/$VERSION_CODE.txt

TAG=v"$VERSION_NAME"

echo "Built $VERSION_NAME ($VERSION_CODE)"

if test -f "$CHANGELOG"; then
    echo "$CHANGELOG exists, choose another version"
else
  touch "$CHANGELOG"
  echo "Fill the changelog and press enter"

  read -r

  git commit --all -m "[fastlane] Version $VERSION_NAME ($VERSION_CODE)"

  git tag "$TAG"
  git push --tags
  gh release create "$TAG" -F "$CHANGELOG" ./app/build/outputs/apk/*/*.apk

  git push
fi





