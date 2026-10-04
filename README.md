# WordVibe

English vocabulary trainer (`com.venom.wordvibe`, minSdk 24, SDK 37): flashcards, spelling practice, and quizzes with AR/EN support.

## Features (`app/src/main/java/com/venom/`)

- `stackcard/` — flashcard decks · `lingospell/` — spelling practice · `quiz/` — quizzes
- `data/` + `domain/` — clean-architecture layers, mappers, local + mock sources
- `analytics/` — usage analytics · `ui/` + Hilt `di/` + Compose navigation
- Push via `wordvibe/MyFirebaseMessagingService.kt` (FCM) + Crashlytics
- `localeFilters`: `en`, `ar` (see `app/build.gradle.kts`); release build minified + shrunk, baseline profiles enabled

## Stack

Kotlin · Jetpack Compose · Hilt + KSP · Kotlin Serialization · Firebase (Crashlytics, FCM).

## Build

**JDK 21 is pinned** via `gradle/gradle-daemon-jvm.properties` — Gradle selects it automatically for every clone, overriding any machine-level `org.gradle.java.home`. (Rationale: Kotlin 2.3.10 crashes probing jrt-fs on JDK 25 — the Android Studio JBR — during cold compiles.)

- Open in Android Studio or run `./gradlew :app:assembleDebug`.
- Android SDK resolves from `local.properties` (`sdk.dir`) or `ANDROID_HOME`. `local.properties` is machine-local and gitignored.
- `app/google-services.json` is **committed**: it contains public Android identifiers only (no secrets) and the Google Services plugin cannot configure without it. Restrict the API key (package + SHA-1) in Google Cloud Console before making the repo public.
- Shared libraries resolve from the vendored `libs/m2repo` (no `~/.m2` or network account needed). Room schemas export to `app/schemas/` on every build.
