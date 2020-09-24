# Dashchan Extensions

Extensions and libraries.

Old extensions are placed under their own specific branch. It's planned to move them all into master branch.

## Building Guide

1. Install JDK 8 or higher
2. Install Android SDK, define `ANDROID_HOME` environment variable or set `sdk.dir` in `local.properties`
4. Run `./gradlew :extensions:%CHAN_NAME%:assembleRelease`

The resulting APK file will appear in `extensions/%CHAN_NAME%/build/outputs/apk` directory.

### Build Signed Binary

You can create `keystore.properties` in the source code directory with the following properties:

```properties
store.file=%PATH_TO_KEYSTORE_FILE%
store.password=%KEYSTORE_PASSWORD%
key.alias=%KEY_ALIAS%
key.password=%KEY_PASSWORD%
```

## License

Extensions and libraries are available under the [GNU General Public License, version 3 or later](COPYING).
