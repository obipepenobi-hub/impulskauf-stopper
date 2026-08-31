# Impulskauf-Stopper

Android-App (Kotlin, Jetpack Compose, Room, WorkManager) gegen Impulskäufe: ein Wunsch
muss erst "einreifen" (preisabhängige Wartezeit), bevor du entscheidest, ob du ihn wirklich
kaufst. Sagst du "doch nicht gekauft", wandert der Betrag ins virtuelle Sparschwein.

Rein lokal (Room/SQLite), kein Backend, keine Accounts.

## Update-Struktur über GitHub

Die App hat keinen Play-Store-Vertrieb. Stattdessen:

1. Ein Git-Tag im Format `vX.Y.Z` pushen → `.github/workflows/release.yml` baut automatisch
   eine signierte Release-APK und veröffentlicht sie als **GitHub Release** mit der APK als
   Anhang.
2. Die App selbst prüft beim Start (und manuell über Einstellungen → "Nach Updates suchen")
   via `GET /repos/<owner>/<repo>/releases/latest`, ob eine neuere Version vorliegt
   ([`UpdateChecker.kt`](app/src/main/java/com/liam/kaptalismusaufhalter/update/UpdateChecker.kt)).
3. Ist eine neuere Version da, zeigt sie einen Dialog. Bei "Herunterladen" lädt
   [`UpdateInstaller.kt`](app/src/main/java/com/liam/kaptalismusaufhalter/update/UpdateInstaller.kt)
   die APK über den system-eigenen `DownloadManager` herunter und öffnet danach den
   Android-Installer (`ACTION_VIEW` + `FileProvider`). Android fragt dabei automatisch nach
   der Berechtigung "Apps aus dieser Quelle installieren", falls noch nicht erteilt.

Welches Repo dafür abgefragt wird, steht in [`gradle.properties`](gradle.properties)
(`UPDATE_REPO_OWNER`, `UPDATE_REPO_NAME`) — anpassen, falls du das Repo forkst/umbenennst.

### Warum ein Update überhaupt installiert werden kann

Damit eine neue APK eine bereits installierte ersetzen darf, muss sie mit demselben
Signing-Key signiert sein. Für das MVP liegt dafür ein Keystore im Repo
(`keystore/shared-debug.keystore`, Passwort `android`, Alias `androiddebugkey` — Standard-
Debug-Defaults), den sowohl lokale als auch CI-Builds verwenden
(`app/build.gradle.kts` → `signingConfigs["shared"]`).

**Wichtig:** Dieser Key liegt im Klartext im Repo und ist nur für den privaten MVP-Gebrauch
gedacht. Bevor die App an andere Personen verteilt wird, einen echten Release-Keystore
erzeugen, ihn **nicht** committen, sondern als GitHub Secret (`RELEASE_KEYSTORE_BASE64`,
`RELEASE_STORE_PASSWORD`, `RELEASE_KEY_PASSWORD`) hinterlegen und `release.yml` +
`signingConfigs["shared"]` entsprechend umstellen.

## Release bauen

```bash
git tag v1.0.1
git push origin v1.0.1
```

GitHub Actions übernimmt den Rest. Der Release erscheint unter
`https://github.com/<owner>/<repo>/releases`.

## Lokal bauen

Voraussetzungen: JDK 17, Android SDK (mindestens Platform 34), `local.properties` mit
`sdk.dir=<Pfad zum SDK>` (liegt bereits vor, ggf. anpassen).

```bash
./gradlew assembleDebug     # Debug-APK unter app/build/outputs/apk/debug/
./gradlew assembleRelease   # Release-APK unter app/build/outputs/apk/release/
```

## Projektstruktur

```
app/src/main/java/com/liam/kaptalismusaufhalter/
  data/       Room-Entities, DAOs, Datenbank, Wartezeit-Staffel (JSON)
  domain/     Reine Berechnungslogik (Arbeitsstunden, Wartezeit, Sparschwein-Stufen)
  work/       WorkManager-Worker + Notification
  update/     GitHub-Release-Check, Download, Installation
  ui/         Compose-Screens, Navigation, Theme, gemeinsame Components
```

## MVP-Umfang

Enthalten: Start-Dashboard, Reift-Liste (mit Schnelleingabe), Neuer-Wunsch-Formular,
Entscheidungs-Screen, Sparschwein-Verlauf, Einstellungen (Stundenlohn, Wartezeit-Staffel
lesend, Update-Check).

Bewusst zurückgestellt (siehe `impulskauf-app-tech-spec.txt`, Abschnitt "Feature (v2)"):
der Freunde-Gruppenvergleich braucht ein Backend (Supabase/Firebase) und einen
Beitritts-Flow, der im Design noch nicht ausgearbeitet ist — der Freunde-Tab zeigt aktuell
nur einen Platzhalter.
