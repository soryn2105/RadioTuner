package com.sorin.radiotuner.data.model

/** (RO)
 *  Model de date pur — reprezintă un post de radio, indiferent de unde
 *  vine (rețea, cache local etc.). Nu conține nicio logică, doar date.
 *  Fiind "pur", poate fi folosit de UI, de rețea și de persistență fără
 *  ca vreunul dintre ele să depindă de celelalte.
 *
 **  (EN)
 *  Pure data model — represents a radio station, regardless of where it comes from
 *  (network, local cache, etc.). It contains no logic, only data.
 *  Being "pure", it can be used by UI, network,
 *  and persistence without any of them depending on the others.
 */

data class Station(
    val id: String,
    val name: String,
    val genre: String,
    val country: String,
    val streamUrl: String,
    val isFavorite: Boolean = false,
    val imageUrl: String? = null,
    val bitrate: Int? = null,
    val homepage: String? = null
)

/** (RO)
 * Metadatele afișate în ticker-ul "RDS" în timpul redării.
 * Pe internet nu există RDS real (acela e un protocol al cipurilor FM
 * din mașini) — folosim aici metadata ICY/Shoutcast (ex. "StreamTitle"),
 * care e echivalentul cel mai apropiat disponibil pentru radio online.
 ** (EN)
 * Metadata displayed in the "RDS" ticker during playback.
 * On the internet, there is no real RDS (that is a protocol of FM chips
 * in cars) — we use ICY/Shoutcast metadata here (e.g., "StreamTitle"),
 * which is the closest available equivalent for online radio.
 */

data class RdsInfo(
    val text: String,
    val isAvailable: Boolean
)
/**
 * (RO)
 * O țară din care se pot filtra posturile, cu numărul de posturi
 * disponibile acolo (informativ, afișat lângă nume în selector).
 *
 * (EN)
 * A country stations can be filtered by, with the number of stations
 * available there (informational, shown next to the name in the picker).
 */
data class CountryOption(
    val name: String,
    val stationCount: Int,
    val code: String? = null
)
