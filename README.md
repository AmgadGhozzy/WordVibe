# WordVibe

English vocabulary trainer (`com.venom.wordvibe`, minSdk 24, SDK 37): flashcards, spelling practice, and quizzes with AR/EN support.

## Features (`app/src/main/java/com/venom/`)

- `stackcard/` — flashcard decks · `lingospell/` — spelling practice · `quiz/` — quizzes
- `data/` + `domain/` — clean-architecture layers, mappers, local + mock sources
- `analytics/` — usage analytics · `ui/` + Hilt `di/` + Compose navigation
- Push via `wordvibe/MyFirebaseMessagingService.kt` (FCM) + Crashlytics
- `localeFilters`: `en`, `ar` (see `app/build.gradle.kts`); release build minified + shrunk, baseline profiles enabled

## Stack

Kotlin · Jetpack Compose · Hilt + KSP · Kotlin Serialization · Firebase (Crashlytics, FCM). `RECORD_AUDIO` for pronunciation input.

## Build

Open in Android Studio. `local.properties` is optional (clean checkouts configure without it). `app/google-services.json` is required — fetch it from the Firebase console for `com.venom.wordvibe`; it stays local via `app/.gitignore`. Shared libraries resolve from the vendored `libs/m2repo` (no `~/.m2` or network account needed). Room schemas export to `app/schemas/` on every build.
