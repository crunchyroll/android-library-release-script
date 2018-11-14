# Android Library Release Script

Release any Android library with this one simple trick DevOps won't tell you about.

## Prerequisites

```bash
brew install maven
brew install holgerbrandl/tap/kscript
brew install hub
```

## Running the script

```bash
kscript https://github.com/crunchyroll/android-library-release-script/blob/master/src/main/kotlin/Release.kt
```

## Releasing

The script itself doesn't have to be compiled to be released. Anyone can reference it directly from the `master` tree.

## Development

Run `./gradlew build` to compile the script and run all checks.
