# RandomClip

Android-App zum zufälligen Abspielen kurzer Video-Clips aus einem lokalen Ordner.

## Features

- **SAF-Ordnerauswahl** — Videos per Storage Access Framework auswählen
- **Zufällige Clips** — zufälliges Video + zufälliger Startpunkt
- **Einstellungen** — Clip-Länge (2–15 s), Ton, Auto-Advance, Wiedergabegeschwindigkeit
- **Catppuccin Mocha** — dunkles, minimalistisches UI
- **Performance** — wiederverwendeter ExoPlayer, Hardware-Decoding, Room-Cache für Metadaten

## Tech Stack

- Kotlin + Jetpack Compose
- Media3 ExoPlayer
- DataStore (Preferences)
- Room (Video-Metadaten-Cache)

## Projektstruktur

```
app/src/main/java/com/randomclip/app/
├── MainActivity.kt
├── RandomClipApplication.kt
├── data/
│   ├── SettingsRepository.kt      # DataStore
│   ├── VideoRepository.kt         # Scan + Cache + Zufallsauswahl
│   ├── VideoScanner.kt            # SAF DocumentFile + MediaMetadataRetriever
│   └── local/                     # Room DB
├── model/Models.kt
├── player/VideoPlayerManager.kt   # ExoPlayer (Reuse, CLOSEST_SYNC)
└── ui/
    ├── RandomClipViewModel.kt
    ├── screens/
    │   ├── VideoPlayerScreen.kt
    │   └── SettingsSheet.kt
    └── theme/
```

## Build

1. Projekt in **Android Studio** oder **Rider** öffnen: `/home/abdul/RiderProjects/RandomClip`
2. Gradle Sync ausführen
3. Auf Gerät/Emulator deployen (minSdk 26)

```bash
cd /home/abdul/RiderProjects/RandomClip
./gradlew assembleDebug
```

## Nutzung

1. App starten → Einstellungen (Zahnrad)
2. **Video-Ordner wählen** → Ordner mit Videodateien auswählen
3. App spielt automatisch zufällige Clips ab
4. Bei deaktiviertem Auto-Advance: Tippen oder nach links wischen für nächsten Clip

## Hinweise

- Erste Ordner-Scan kann bei vielen Videos dauern; danach werden Metadaten aus Room geladen
- Videos werden **nicht** transkodiert — nur Seek + direkte Wiedergabe in Originalqualität
- Bei App-Pause werden Player-Ressourcen pausiert; beim Verlassen freigegeben
