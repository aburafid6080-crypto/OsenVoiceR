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


## Cross-device compatibility requirement
The product goal is broad Android-device support, not only the Moto G85 5G. "Works on every phone" cannot be guaranteed: Android versions, audio HAL implementations, microphone routing, vendor restrictions, and game voice-chat behavior differ by device.
A realistic release target must define a tested compatibility matrix (Android version, manufacturer/model, and Free Fire version) and clearly label untested devices. Do not advertise universal compatibility without evidence.
The no-accessory requirement is retained: any proposed method must use only the phone's built-in microphone and internal software. Under standard Android app sandbox rules, this still does not grant one app the ability to replace another app's microphone input. Therefore, a phone-only app cannot honestly promise that OSEN's processed voice will be heard by Free Fire teammates across all Android phones.


## Official Android API verification (2026-11-11)
- Android's input-sharing documentation says two ordinary apps cannot capture the microphone concurrently; when a privacy-sensitive source such as `VOICE_COMMUNICATION` is active, the ordinary competing app is silenced.
- Android's Playback Capture API captures eligible *playback output* from another app after MediaProjection consent and only when the source app's usage and capture policy permit it. It does not provide a microphone-input replacement path into a game.
- Therefore, a phone-only, unrooted, ordinary third-party app cannot implement the requested universal Free Fire voice-chat injection using public Android APIs. A live DSP engine inside OSEN could process the phone microphone while OSEN owns the input, but Free Fire would not thereby receive that processed stream.
- Decision: do not modify the production app or add a misleading "Free Fire" toggle. Do not create an APK claiming this feature. The requested all-phone / built-in-mic-only outcome is not achievable through supported public Android APIs as currently specified.

### Official references
- Android audio input sharing: https://developer.android.com/media/platform/sharing-audio-input
- Android playback capture: https://developer.android.com/media/platform/av-capture
- Android concurrent capture policy: https://source.android.com/docs/core/audio/concurrent
