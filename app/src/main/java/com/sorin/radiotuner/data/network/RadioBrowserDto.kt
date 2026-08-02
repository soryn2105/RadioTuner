package com.sorin.radiotuner.data.network

import com.google.gson.annotations.SerializedName

/**
 * (RO)
 * Structura brută primită de la Radio Browser API (json/stations/search).
 *  * Numele câmpurilor trebuie să corespundă exact cu cele din răspunsul
 *  * API-ului — de asta folosim @SerializedName, ca să putem avea în Kotlin
 *  * nume mai clare decât cele originale.
 *  (EN)
 *  Raw structure received from the Radio Browser API (json/stations/search).
 *  The field names must match exactly those in the API response — that's why we use @SerializedName,
 *  so we can have clearer names in Kotlin than the original ones.
 */
class RadioBrowserStationDto (
    @SerializedName("stationuuid") val stationUuid: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("url_resolved") val urlResolved: String?,
    @SerializedName("tags") val tags: String?,
    @SerializedName("country") val country: String?,
    @SerializedName("favicon") val favicon: String?,
    @SerializedName("bitrate") val bitrate: Int?,
    @SerializedName("homepage") val homepage: String?
)

/**
 * (RO)
 * Structura brută primită de la Radio Browser API (json/countries).
 * Notă: endpoint-ul întoarce doar "name" și "stationcount" — NU un cod
 * ISO. Filtrarea se face după numele țării, nu după cod.
 *
 * (EN)
 * Raw structure received from the Radio Browser API (json/countries).
 * Note: the endpoint only returns "name" and "stationcount" — NOT an
 * ISO code. Filtering is done by country name, not by code.
 */
class RadioBrowserCountryDto(
    @SerializedName("name") val name: String?,
    @SerializedName("stationcount") val stationCount: Int?,
    @SerializedName("iso_3166_1") val countryCode: String?
)
