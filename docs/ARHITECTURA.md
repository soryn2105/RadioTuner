# RadioTuner — Arhitectura aplicației

## Scop
Aplicație Android nativă pentru ascultarea posturilor de radio online:
căutare, favorite, redare live, afișaj de metadate tip RDS, detectare
offline cu reconectare automată, și interfață multilingvă.

## Stack tehnic
| Componentă | Alegere | Motiv |
|---|---|---|
| Limbaj | Kotlin | standard modern Android |
| UI | Jetpack Compose | declarativ, direcția oficială Google, se mapează natural pe prototipul făcut în React |
| Rețea | Retrofit + OkHttp | căutare posturi via Radio Browser API |
| Streaming audio | ExoPlayer (Media3) | biblioteca standard Android pentru redare audio în flux |
| Persistență favorite | DataStore (Preferences) | înlocuitor modern pentru SharedPreferences, bazat pe Coroutines/Flow |
| Multilingv | resurse `strings.xml` per limbă + `AppCompatDelegate.setApplicationLocales` | selector manual de limbă în aplicație, independent de limba telefonului |
| Arhitectură generală | MVVM pe pachete (un singur modul Gradle) | separare clară UI / logică / date prin interfețe; ușor de depanat pe bucăți; se poate muta în module Gradle separate mai târziu fără rescriere majoră |

## Principiul de modularitate
UI-ul (Compose) **nu vorbește niciodată direct** cu rețeaua, baza de date
locală sau player-ul. Vorbește doar cu `RadioViewModel`, care la rândul
lui depinde doar de **interfețe** (`StationRepository`, `FavoritesRepository`,
`RadioPlayer`), nu de implementările concrete.

Consecința practică: dacă streaming-ul nu merge, problema e garantat în
`player/`. Dacă favoritele nu se salvează, problema e în `data/local/`.
Dacă vrem să înlocuim ExoPlayer cu altă bibliotecă, schimbăm doar
`ExoRadioPlayer.kt`, fără să atingem niciun ecran.

```
UI (Compose)  →  RadioViewModel  →  interfețe (contracte)
                                        │
                        ┌───────────────┼────────────────┐
                        ▼               ▼                ▼
              NetworkStationRepository  LocalFavoritesRepository  ExoRadioPlayer
              (implementare reală)      (implementare reală)      (implementare reală)
```

## Structura pachetelor
```
com.numele_tau.radiotuner/
├── RadioTunerApplication.kt   → punctul de start, creează AppContainer
├── MainActivity.kt            → activitatea unică, montează Compose
├── di/
│   └── AppContainer.kt        → "leagă" interfețele de implementările lor (DI manual)
├── data/
│   ├── model/                 → Station, RdsInfo — date pure, fără logică
│   ├── repository/            → interfețe: StationRepository, FavoritesRepository
│   ├── network/                → implementare rețea: Retrofit, DTO-uri, mapper
│   └── local/                  → implementare locală: DataStore pentru favorite
├── player/                     → interfața RadioPlayer + implementarea ExoRadioPlayer
├── util/
│   ├── ConnectivityObserver.kt → Flow<Boolean> cu starea conexiunii la internet
│   ├── ReconnectStrategy.kt    → backoff progresiv anti-buclă la reconectare
│   └── LanguageManager.kt      → aplică/citește limba selectată manual
└── ui/
    ├── theme/                  → culori, tipografie, tema Compose
    ├── components/             → bucăți reutilizabile (rând post, player, RDS ticker, dialog limbă)
    ├── viewmodel/               → RadioViewModel + RadioUiState (starea ecranului)
    └── RadioScreen.kt          → ecranul principal, landscape/portrait
```

## Multilingv — cum funcționează
1. Toate textele din interfață sunt în `res/values/strings.xml` (română,
   implicit), `res/values-en/strings.xml` (engleză) și
   `res/values-fr/strings.xml` (franceză, exemplu suplimentar).
2. La pornire, Android alege automat setul potrivit după limba
   telefonului — asta e comportamentul standard, gratuit.
3. Peste el, adăugăm un **buton de limbă în aplicație** (nu doar limba
   telefonului): la apăsare, un dialog listează limbile disponibile;
   alegerea cheamă `AppCompatDelegate.setApplicationLocales(...)`, care
   suprascrie limba **doar pentru aplicația noastră** și se reține automat
   între lansări (mecanism nativ AndroidX, fără cod suplimentar de
   salvare din partea noastră).

## Detectarea offline și reconectarea anti-buclă
`ConnectivityObserver` expune un `Flow<Boolean>` cu starea rețelei,
folosind `ConnectivityManager.NetworkCallback` (API nativ Android, nu
polling manual). Când conexiunea revine după o pierdere **în timp ce un
post reda**, `RadioViewModel` cere player-ului reconectare — dar nu
instant și la nesfârșit, ci prin `ReconnectStrategy`: un backoff
progresiv (2s → 4s → 8s → 16s, plafonat la 30s), resetat la 2s după orice
redare reușită. Așa evităm bucla de reîncercări rapide care ar
supraîncărca telefonul și streamul.

## Limitări cunoscute (documentate intenționat)
- ~~Radio Browser API are mai multe servere-oglindă; folosim unul fix~~ —
  **actualizat**: acum descoperim automat serverele active
  (`RadioBrowserServers.discover()`) și facem failover între ele dacă
  unul nu răspunde. Toate serverele au aceeași bază de date mondială —
  asta nu adaugă mai multe posturi, doar face căutarea rezistentă la
  căderea unui singur server.
- Metadata ICY (echivalentul RDS pe internet) nu e garantată de toate
  posturile — quand lipsește, afișăm starea „fără metadate”.
- Codul din `player/ExoRadioPlayer.kt` pentru citirea ICY e scris corect
  conform documentației Media3, dar se verifică și, dacă e nevoie, se
  ajustează la prima rulare reală — nu a fost compilat/testat de mine.

## Jurnal de decizii
| Decizie | Motiv |
|---|---|
| Jetpack Compose în loc de XML | cod declarativ, mai ușor de întreținut |
| MVVM pe pachete, nu module Gradle | separare reală fără complexitatea configurării multi-modul |
| Limbă suprascrisă manual, nu doar sistem | cerință explicită: buton de alegere limbă în aplicație |
| Nume aplicație: "Amber Tuner" | reflectă identitatea vizuală centrală (cadranul amber), se citește natural în RO/EN/FR |
| Iconiță: vector drawable, nu imagine bitmap | cod, nu fișier binar — consistent cu restul proiectului, scalează perfect la orice rezoluție |
| DataStore în loc de SharedPreferences | API modern, bazat pe Coroutines/Flow, recomandat de Google |
| Descoperire automată de servere Radio Browser + failover | rezistență la căderea unui server (chiar bug-ul care a blocat căutarea inițial); toate serverele au aceeași bază mondială, deci nu schimbă ce se găsește, doar cât de fiabil se găsește |
| Paginare ("Încarcă mai multe") în loc de doar creșterea limitei fixe | evită liste uriașe greu de randat și folosirea abuzivă a API-ului gratuit; utilizatorul controlează câte pagini încarcă |
