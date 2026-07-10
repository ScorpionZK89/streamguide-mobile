# StreamGuide Mobile

StreamGuide Mobile is een originele Android IPTV-speler voor telefoons. De app levert geen zenders, streams, playlists of piraterijcontent mee. Gebruikers voegen zelf een legale IPTV-bron toe.

## Functies

- Kotlin, Jetpack Compose en Material 3
- Media3 / ExoPlayer voor afspelen
- Room database voor playlists, zenders en EPG-programma's
- DataStore voor simpele instellingen
- WorkManager voorbereiding voor periodieke playlist- en EPG-sync
- Coil voor zenderlogo's
- Xtream Codes import via server URL, gebruikersnaam en wachtwoord
- M3U URL import en lokaal M3U-bestand kiezen
- XMLTV EPG URL import en koppeling via tvg-id
- Automatische ingebouwde Xtream EPG via xmltv.php wanneer geen losse EPG-link is ingevuld
- Favorieten, laatst bekeken, zoeken en categorieen
- Geen meegeleverde streams of illegale bronnen

## Testen

Open `C:\Users\Mark\Documents\Codex\streamguide-mobile` in Android Studio en klik op Run.

1. Kies Xtream Codes of M3U.
2. Vul je eigen legale bron in.
3. Voeg optioneel een XMLTV EPG URL toe. Bij Xtream mag je dit meestal leeg laten; de app probeert dan de ingebouwde gids.
4. Controleer de tabs Zenders, Gids en Instellingen.