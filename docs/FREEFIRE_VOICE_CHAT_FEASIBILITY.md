# OSEN Voice R — Free Fire Voice Chat Feasibility

## Current status
- This experiment is isolated on branch `experiment/freefire-live-voice`.
- The existing `app/` source, manifest, Gradle configuration, and UI have not been modified.
- No APK has been built and no Free Fire microphone test has been performed.

## What the current app does
The current implementation records microphone PCM to a file, processes the completed recording with the selected effect, then plays the processed audio through an `AudioTrack` using media playback attributes. This is record-process-playback, not a live microphone effect pipeline.

## Key limitation
An ordinary Android app can record audio only with the applicable permission and play its own output. Android does not provide a normal public API for one app to replace the microphone input consumed by another app (such as Free Fire). Adding microphone permission, an accessibility service, or an `AudioTrack` does not create that cross-app microphone route.

## Safe next checks
1. Keep the existing app untouched.
2. Investigate whether a supported external audio input route can present processed audio as a microphone to Android/Free Fire on the user's specific phone and accessories.
3. If no supported route exists, report that software-only in-app injection is not feasible on the unmodified device; do not promise that a prototype APK will make teammates hear the effect.
4. Do not root the phone, modify Free Fire, bypass anti-cheat, or use undocumented system audio hacks.

## Test needed from the device
A real test requires the Moto G85 5G, Android 16, Free Fire, and any proposed external audio accessory. Verify the input route in Android first, then use Free Fire's own microphone test/voice chat with a trusted teammate. Until this is done, teammate-side altered voice is unverified.


## Additional Android / Moto G85 research
- Android's official USB audio documentation says compatible USB digital audio peripherals can be routed for capture and voice-chat use. This means a supported USB audio device may provide an external microphone input, but it does not mean an app can inject its processed playback into Free Fire's microphone.
- Android's audio-input sharing rules explain that microphone capture is controlled by system priority and routing. A second ordinary app cannot assume it will receive the same microphone while a game is capturing.
- Motorola's Moto G85 hardware guide identifies a USB-C port and wired-headphone support. The published hardware information reviewed here does not confirm a specific USB audio interface or processed-audio loopback configuration for Free Fire.
- No claim of compatibility is made until it is tested on the actual Moto G85 5G / Android 16 device.

### References
- Android USB digital audio: https://source.android.com/docs/core/audio/usb
- Android sharing audio input: https://developer.android.com/media/platform/sharing-audio-input
- Moto G85 hardware guide: https://help.motorola.com/hc/3585/14/global/en-us/CGT2002173049.html

## Current decision
Do not add a fake "Free Fire live voice" switch to the app. The next meaningful validation is a real-device check of whether the phone exposes a compatible external USB audio input and whether Free Fire actually selects it. A separate loopback-capable audio path would still be needed to feed OSEN's processed output into that input.
