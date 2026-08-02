Dependințe și configurare necesare
Acest fișier NU se copiază peste `build.gradle.kts` — se adaugă manual în
fișierul deja generat de Android Studio, ca să nu pierzi configurările
existente (versiune SDK, application ID etc.).
1. `app/build.gradle.kts` → în blocul `dependencies { }`
```kotlin
// Rețea
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Streaming audio
implementation("androidx.media3:media3-exoplayer:1.4.1")
implementation("androidx.media3:media3-common:1.4.1")

// Persistență favorite
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Limbă per-aplicație (AppCompatDelegate)
implementation("androidx.appcompat:appcompat:1.7.0")

// Coroutines + ViewModel în Compose
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")

// Iconițe extinse — Radio, Language, WifiOff, SkipNext/Previous, Stop
// NU sunt în setul de bază livrat automat cu Material3, e nevoie de
// acest pachet separat. Fără număr de versiune — proiectul generat de
// Android Studio are deja un Compose BOM ("platform(...)") care îi
// stabilește automat versiunea corectă.
implementation("androidx.compose.material:material-icons-extended")
```
Dacă Android Studio subliniază vreo linie, apasă Alt+Enter → de obicei
sugerează el varianta corectă de versiune pentru configurația ta (nu e
o eroare gravă, doar sincronizare de versiuni).
2. `AndroidManifest.xml`
   Adaugă, înainte de tag-ul `<application ...>`, permisiunile de rețea:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```
Pe tag-ul `<application ...>`, adaugă atributul `android:name` ca să
folosească clasa noastră de Application (unde inițializăm dependințele),
și `android:localeConfig` ca să apară limbile și în Settings > App Info
> Language pe Android 13+ (complementează, nu înlocuiește butonul din
aplicație):
```xml
<application
    android:name=".RadioTunerApplication"
    android:localeConfig="@xml/locales_config"
    ... restul atributelor deja existente ...
>
```
3. Sincronizare
   După ce adaugi toate liniile de mai sus, apasă „Sync Now” în bara
   galbenă care apare sus în Android Studio. Dacă apar erori la sincronizare,
   copiază-mi mesajul exact și le rezolvăm împreună — sunt aproape mereu
   diferențe minore de versiune, nu probleme de arhitectură.