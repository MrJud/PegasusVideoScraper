# PegasusVideoPlayer — Manutenzione

> Promemoria di cosa toccare, quando, e come accorgersi che serve.
> Vedi anche: [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) per autori di temi terzi.

---

## Componenti con manutenzione programmata

| Componente | File | Cadenza | Azione |
| --- | --- | --- | --- |
| **NewPipeExtractor** | [app/build.gradle.kts](app/build.gradle.kts) riga 59 | Ogni 1-2 mesi **o** alla prima segnalazione di "search/resolve fails" | Bump a ultima release: https://github.com/TeamNewPipe/NewPipeExtractor/releases |
| **Media3 / ExoPlayer** | [app/build.gradle.kts](app/build.gradle.kts) riga 43 (`val media3`) | 1× anno | Bump `val media3 = "X.Y.Z"` + smoke test playback HLS/DASH/progressive |
| **Chrome User-Agent** | [VideoPlayerActivity.kt:37-39](app/src/main/java/com/pegasus/videoplayer/VideoPlayerActivity.kt#L37-L39) | 1× anno | Aggiorna major version (Chrome/131 → Chrome/140 ecc.) |
| **compileSdk / targetSdk** | [app/build.gradle.kts](app/build.gradle.kts) righe 10, 15 | 1× anno (agosto, quando Google pubblica nuovo Android) | Bump a nuova major + test su device con quella versione |
| **Kotlin / AGP** | [app/build.gradle.kts](app/build.gradle.kts), root `build.gradle.kts` | 1× anno | Bump solo quando obbligato da Android Studio |

---

## Segnali di rottura (monitorare reattivamente)

| Sintomo | Causa più probabile | Fix |
| --- | --- | --- |
| "YouTube Search unavailable" in QML dopo mesi di funzionamento | NewPipeExtractor obsoleto, Google ha cambiato YouTube internals | Bump NewPipeExtractor → rebuild APK → redistribuisci |
| "Video non disponibile o rimosso" su video che funzionavano | Idem | Idem |
| Steam CDN restituisce 403 su trailer | Chrome UA troppo vecchio, CDN rifiuta come bot | Bump UA a Chrome major corrente |
| App crash al primo lancio su Android nuovo (15/16/17) | MANAGE_EXTERNAL_STORAGE revoked / scoped storage breaking change | Migra callback path da `/sdcard/ReStory/` a `/Android/data/com.pegasus.videoplayer/files/` (scoped storage, permesso automatico) |
| "File not found" sistematico sui callback JSON dopo Android major update | Policy FileSystem cambiata | Come sopra — scoped storage |
| ANR / freeze al launch SearchActivity | `finish()` non chiamato sincronicamente in onCreate (regressione code review) | Verifica che `handle(intent?.data)` non contenga mai `await` o `.join()` |

---

## Protocollo d'emergenza "YouTube è rotto"

Quando >50% delle ricerche/risoluzioni fallisce su più device:

1. Leggi `adb logcat -s PegasusVideoPlayer PegasusSearch` su un device affetto.
2. Se trovi `ExtractionException` / `ContentNotAvailable` / `ReCaptchaException` → NewPipeExtractor è rotto.
3. Controlla https://github.com/TeamNewPipe/NewPipeExtractor/releases — nel 90% dei casi c'è già una release con fix (di solito entro 72h dal breakage YouTube).
4. Aggiorna la riga 59 di [app/build.gradle.kts](app/build.gradle.kts) alla nuova release.
5. `./gradlew :app:assembleRelease` → firma → pubblica release GitHub.
6. Gli utenti scaricano il nuovo APK. Niente da toccare nei temi.

Se NewPipeExtractor non ha ancora pubblicato il fix: **attendere**. Patch locali sono tempo sprecato — il fix arriva sempre.

---

## Cosa **non** richiede manutenzione

Tutto quanto segue è stabile e non va toccato salvo refactor intenzionale:

- URI scheme `restory-video://` (contratto pubblico — modificare solo con deprecation period di 2-3 release)
- Schema JSON callback (`schema: 1` — bumpare solo con breaking change reale)
- Pattern videoId regex `[?&]v=([A-Za-z0-9_-]{11})` — invariato dal 2005
- Thumbnail URL pattern `i.ytimg.com/vi/<id>/hqdefault.jpg` — idem
- Kotlin coroutine scope in `VideoPlayerApp` — pattern standard
- Atomic write `.tmp` → rename in `SearchCallback.kt` — POSIX invariante

---

## Checklist annuale (1× l'anno, tipicamente Q3)

- [ ] Bump `media3` alla ultima release stabile.
- [ ] Bump Chrome UA di ~1 major version.
- [ ] Bump `compileSdk`/`targetSdk` alla nuova Android release.
- [ ] Smoke test: apri APK, concedi permessi, play YouTube trailer, play Steam trailer, search YouTube da QML, apply trailer.
- [ ] Verifica su Android nuova major (emulatore o device) che MANAGE_EXTERNAL_STORAGE non sia deprecato ulteriormente.
- [ ] Publish release con changelog.

---

## Chi può toccare cosa

| Chi | Cosa | Note |
| --- | --- | --- |
| Manutentore APK | Tutto il codice Kotlin, manifest, gradle | Non cambiare URI scheme senza notifica ai temi |
| Autore tema | **Niente del codice APK** | Interagisce solo via URI contract — vedi INTEGRATION_GUIDE.md |
| Utente finale | Installa APK, concede permessi | Mai modificare file JSON in `/sdcard/...` manualmente |

---

## Versioning del contratto pubblico

- Attuale: URI scheme `restory-video://` + JSON callback schema v1.
- Breaking changes futuri: bumpare **entrambi** (`pegasus-video://` v2 scheme + `schema: 2`) mantenendo il vecchio scheme come deprecated per almeno 2 release prima della rimozione.
- Questo dà ai temi terzi una window per aggiornarsi senza rotture silenziose.
