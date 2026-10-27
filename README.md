<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/b3745068-d552-4c2d-ad4b-98a714f07e6a

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device
7. If you have already published your app in AI Studio, please [request upload key reset](https://support.google.com/googleplay/android-developer/answer/9842756#zippy=%2Crequest-an-upload-key-reset) in Google Play Console.

## Google Sign-In Setup

The "Continue with Google" button uses the Credential Manager API backed by the Firebase
project in `app/google-services.json` (project `coursehub-6edbc`, package
`com.agsdeveloper.coursehub`). There are only two admin accounts in the app:
`wahaballied6@gmail.com` and `junaidallied6@gmail.com`.

This repository is already configured:

- `app/google-services.json` is in place, so Firebase Auth initializes at runtime.
- The app package is `com.agsdeveloper.coursehub`, matching the Firebase / Google Cloud registration.
- The Web client ID used for sign-in defaults to the one from `google-services.json`
  (`419536212917-q64aubqdpcqk2ip8t9qp402t2fcigm8s.apps.googleusercontent.com`).

Still required, once, on the Firebase / Google Cloud side:

1. In the [Firebase console](https://console.firebase.google.com/project/coursehub-6edbc/authentication),
   enable **Google** as a sign-in provider (Authentication → Sign-in method → Google → Enable).
   If it is disabled, the app still signs you in locally, but the Firebase-backed session won't be created.
2. Make sure the app is signed with the keystore whose SHA-1 is registered for the Android OAuth
   client (currently `b8e5a444a728fe420f7b6373964d21ea0a79a9e2`). Check yours with:
   `keytool -list -v -keystore <your.keystore> -alias <alias> -storepass <password>`.
   This is the most common reason Google Sign-In fails with a "16: ..." error on a device.
3. Because the package name changed to `com.agsdeveloper.coursehub`, uninstall any old build
   installed under the previous package before reinstalling.

If you ever switch Firebase projects, override the Web client ID by uncommenting
`GOOGLE_WEB_CLIENT_ID` in `gradle.properties`.
