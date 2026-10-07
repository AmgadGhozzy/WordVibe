# ADR-0007 — google-services.json is committed; Compose smoke tests live in androidTest

Date: 2026-10-04
Status: Accepted
Related: ADR-0006 (JVM testing for the Compose shell), Phase 1 closure review.

## Decision

1. **`app/google-services.json` is committed.** The Google Services Gradle plugin
   refuses to configure without it, which broke every clean clone (the Phase 1
   closure gate caught this). The file contains public Android identifiers — the
   API key, package name, and project IDs are not secrets on Android; protection
   comes from API-key restrictions, not from hiding the file. Required follow-up:
   restrict the key in Google Cloud Console (Android app, package
   `com.venom.wordvibe`, release SHA-1) before the repository goes public.
2. **Compose smoke tests (`ShellTabSmokeTest`) live in `app/src/androidTest`** and
   run via `connectedDebugAndroidTest` on a device/emulator, together with the
   light/dark screenshots that gate Phase 1 closure. Robolectric was attempted on
   the JVM and withdrawn (ADR-0006): its test JVM hung and then crashed the Gradle
   daemon in this environment.

## Consequences

- A clean clone builds and tests green on any machine with a JDK 21 toolchain
  (pinned via `gradle/gradle-daemon-jvm.properties`, see the Phase 1 closure
  commit) — no manual Firebase setup step.
- The API key must be restricted before publication; rotation is otherwise
  trivial from the Firebase console.
- ADRs remain disk-only per the constitution (docs/ is never committed). If
  versioned ADR history is wanted, that is a constitution amendment.
