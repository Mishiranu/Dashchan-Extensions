# Dashchan Extensions

This repository is used to store Dashchan extensions source code.

Old extensions are placed under their own specific branch. It's planned to move them all into master branch.

General dependencies: [Public API](https://github.com/Mishiranu/Dashchan-Library),
[Static Library](https://github.com/Mishiranu/Dashchan-Static).

## Building Guide

1. Install JDK 8 or higher
2. Install Android SDK, define `ANDROID_HOME` environment variable or set `sdk.dir` in `local.properties`
4. Run `./gradlew :extensions:%CHAN_NAME%:assembleRelease`

The resulting APK file will appear in `extensions/%CHAN_NAME%/build/outputs/apk` directory.

The API library may be updated. In this case, run `gradle --refresh-dependencies assembleRelease`.

### Build Signed Binary

You can create `keystore.properties` in the source code directory with the following properties:

```properties
store.file=%PATH_TO_KEYSTORE_FILE%
store.password=%KEYSTORE_PASSWORD%
key.alias=%KEY_ALIAS%
key.password=%KEY_PASSWORD%
```

## License

Extensions are licensed under the [MIT License](LICENSE).
