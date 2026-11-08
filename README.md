# Osen Voice R — Cloud Build starter

This is a starter Android project with a dark purple/blue Compose UI, microphone recording, stop, and playback, plus a GitHub Actions workflow that builds a debug APK.

## Build without a PC
1. Create/sign in to a GitHub account in your phone browser.
2. Create a new empty repository.
3. Upload the contents of this folder (keep the folder structure).
4. Open the repository's **Actions** tab.
5. Select **Build Osen Voice R APK** and tap **Run workflow**.
6. When it completes, open the run, scroll to **Artifacts**, and download `OsenVoiceR-debug-apk`.
7. Extract the ZIP artifact and install `app-debug.apk`. Android may ask you to allow installation from your browser/files app.

## Important status
This is a starter prototype, not a finished voice changer. Recording and playback are implemented. The effect tiles are UI placeholders: actual pitch shifting, echo, robot processing, audio export, and routing processed audio into Free Fire are not implemented. Do not describe this build as having working voice effects. Test on your phone before relying on it. A debug APK is unsigned for Play Store release; a properly signed release build needs a signing key.
