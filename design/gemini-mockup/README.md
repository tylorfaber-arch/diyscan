# Gemini UI mock-up

A design exploration for DIY Scan generated with Google Gemini (AI Studio), copied here from the
`gemini-mock-up` repository at commit `b55f3b6`.

It is a **separate, standalone Android project** (package `com.example`) that runs on mock data from
`MockVehicleCatalog.kt` / `ScanMockViewModel.kt`. It is not part of the DIY Scan app: the root Gradle
build only includes `:app`, so CI and the released APK are unaffected.

To look at it, open this folder (`design/gemini-mockup`) on its own in Android Studio and run it.

Fonts in `app/src/main/res/font/` (Plus Jakarta Sans, Space Grotesk, JetBrains Mono) are licensed under
the SIL Open Font License 1.1.
