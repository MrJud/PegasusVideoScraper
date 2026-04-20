# Pegasus Video Player

APK Android per la riproduzione di video trailer da **Pegasus Frontend** (ReStory theme).

## Funzionamento

Pegasus chiama `Qt.openUrlExternally("restory-video://play?url=...&title=...&gameKey=...")`.
L'APK intercetta lo schema, avvia ExoPlayer in fullscreen landscape e torna a Pegasus al termine.

## Requisiti

- Android Studio Meerkat (o superiore)
- Android SDK 35
- ADB nel PATH
- Odin2 connesso via USB (device `69bdb979`)

## Primo avvio

```bash
# 1. Copia local.properties.template → local.properties e compila i campi
# 2. Genera keystore (solo per release)
keytool -genkey -v -keystore keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias pegasus

# 3. Copia gradle wrapper dal progetto ROM Hasher
cp ../RetroAchievements-ROM-Hasher/gradlew .
cp ../RetroAchievements-ROM-Hasher/gradlew.bat .
cp -r ../RetroAchievements-ROM-Hasher/gradle/wrapper/gradle-wrapper.jar gradle/wrapper/

# 4. Build debug + deploy
deploy-android.bat            # debug (default)
deploy-android.bat release    # release firmato
```

## URI scheme

```
restory-video://play?url=HTTPS_URL&title=TITOLO&gameKey=CHIAVE
```

- `url`: URL mp4/webm HTTPS (solo HTTPS, HTTP rifiutato)
- `title`: titolo visualizzato in overlay
- `gameKey`: chiave cache (es. `supermarioworld|snes`) — usata per download offline (Fase 2)
- `download=true`: (opzionale) scarica prima di riprodurre

## Struttura

```
app/src/main/
  java/com/pegasus/videoplayer/
    VideoPlayerActivity.kt   — Activity principale
    UriParams.kt             — Parsing + validazione URI
    PlayerErrorMapper.kt     — Messaggi errore ExoPlayer
  res/
    layout/activity_video_player.xml
    values/strings.xml
    values/themes.xml
    drawable/bg_overlay.xml
    drawable/ic_close.xml
  AndroidManifest.xml
```

## Fase 2 — Download offline

Aggiungere `TrailerDownloader.kt` e `TrailerCallbackWriter.kt`.
Vedi `APK_VIDEO_PLAYER_IMPLEMENTATION.md` in `Pegasus Frontend/ReStory/` per dettagli.
