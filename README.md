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
- Instelbare playlist- en EPG-synchronisatie bij het starten van de app
- Zendergroepen verbergen met een blijvend groepsfilter
- Tv-gids met dagkeuze en programmadetails
- Eigen donkere media-interface met compacte zendertegels en moderne playerbediening
- Geen meegeleverde streams of illegale bronnen
- Automatische updatecontrole via openbare GitHub Releases
- Update downloaden en door Android laten installeren vanuit de app

## Updates publiceren

De app controleert `ScorpionZK89/streamguide-mobile` bij het starten en via **Instellingen > App-updates**. Alleen een nieuwere, gepubliceerde GitHub Release met een ondertekende `.apk` wordt aangeboden.

De workflow `.github/workflows/release-apk.yml` test en publiceert automatisch een APK bij een tag zoals `v0.2.1`. Voor de eerste release moeten eenmalig deze GitHub Actions-secrets worden ingesteld:

- `STREAMGUIDE_KEYSTORE_BASE64`
- `STREAMGUIDE_KEYSTORE_PASSWORD`
- `STREAMGUIDE_KEY_ALIAS`
- `STREAMGUIDE_KEY_PASSWORD`

Bewaar de originele keystore veilig. Iedere volgende APK moet met exact dezelfde sleutel worden ondertekend, anders weigert Android de update. De huidige test-APK gebruikt nog de debughandtekening; voor de eerste officiele release moet die eenmalig worden verwijderd voordat de release-APK wordt geinstalleerd.

## Testen

Open het project `streamguide-mobile` in Android Studio en klik op Run.

1. Kies Xtream Codes of M3U.
2. Vul je eigen legale bron in.
3. Voeg optioneel een XMLTV EPG URL toe. Bij Xtream mag je dit meestal leeg laten; de app probeert dan de ingebouwde gids.
4. Controleer de tabs Zenders, Gids en Instellingen.

## Verdere uitbreiding

De code is voorbereid op verdere functies zoals Picture-in-Picture, catch-up wanneer de provider dit legaal ondersteunt, profielen, ouderlijk toezicht, externe spelers en tabletweergave. StreamGuide blijft een eigen product en kopieert geen merk, ontwerp of assets van andere IPTV-apps.
