# ULEV1+

An Android app to read and write MIFARE™ Ultralight EV1 tags/cards, eventually with authentication given the correct password.

Features:
- Read the (non-protected) memory blocks/pages of Ultralight EV1 tags, either MF0UL11 (80B) or MF0UL21 (164B).
- Write the user pages (12 for MF0UL11 and 32 for MF0UL21) after authenticating with a provided password.
- Load text/binary dump file containing the tag data. Supports proxmark3 MFU dumps.
- Save the tag data as binary dump file.
- Management (Add/Edit/Delete) of UIDs/Password/PACK with the ability to import/export the list.
- Colored bytes based on the different memory sections (UID, BCC, CFG, etc.).


## Build

- Using Android Studio: select "Build APK" from the Build menu.
- Using Gradle: `JAVA_HOME=/opt/android-studio/jre ./gradlew build`
The APK file will be generated into `./app/build/outputs/apk/`.

## Install

You can install the APK using ADB like that: `adb install <path-to-app.apk>`.

## Disclaimer

Suggestions for improvement are always welcome.

## License

GPLv3+
