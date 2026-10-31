package com.sorin.radiotuner.data.network

import com.sorin.radiotuner.data.model.CountryOption
import com.sorin.radiotuner.data.model.Station
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * (RO) DTO-uri pentru Radio Browser API compatibile multiplatform (Android + iOS).
 */
@Serializable
data class KtorRadioBrowserStationDto(
    @SerialName("stationuuid") val stationUuid: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("url_resolved") val urlResolved: String? = null,
    @SerialName("tags") val tags: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("favicon") val favicon: String? = null,
    @SerialName("bitrate") val bitrate: Int? = null,
    @SerialName("homepage") val homepage: String? = null
)

@Serializable
data class KtorRadioBrowserCountryDto(
    @SerialName("name") val name: String? = null,
    @SerialName("stationcount") val stationCount: Int? = null,
    @SerialName("iso_3166_1") val countryCode: String? = null
)

/**
 * (RO) Client HTTP Ktor Multiplatform pentru Radio Browser API.
 * Funcționează identic pe Android și pe iOS.
 */
class KtorRadioBrowserApi(
    private val client: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }
) {
    suspend fun search(
        host: String,
        name: String,
        country: String? = null,
        countryCode: String? = null,
        language: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<Station> {
        val dtos: List<KtorRadioBrowserStationDto> = client.get("https://$host/json/stations/search") {
            header("User-Agent", "AmberTuner/1.0 (ComposeMultiplatform; educational project)")
            parameter("name", name)
            if (!country.isNullOrBlank()) parameter("country", country)
            if (!countryCode.isNullOrBlank()) parameter("countrycode", countryCode)
            if (!language.isNullOrBlank()) parameter("language", language)
            parameter("limit", limit)
            parameter("offset", offset)
            parameter("hidebroken", true)
            parameter("order", "clickcount")
            parameter("reverse", true)
        }.body()

        return dtos.mapNotNull { dto ->
            val id = dto.stationUuid ?: return@mapNotNull null
            val url = dto.urlResolved ?: return@mapNotNull null
            val stationName = dto.name ?: "Necunoscut"
            Station(
                id = id,
                name = stationName,
                genre = dto.tags?.replace(",", " • ")?.take(50) ?: "Radio",
                country = dto.country ?: "Global",
                streamUrl = url,
                imageUrl = dto.favicon?.takeIf { it.isNotBlank() },
                bitrate = dto.bitrate,
                homepage = dto.homepage?.takeIf { it.isNotBlank() }
            )
        }
    }

    suspend fun listCountries(host: String): List<CountryOption> {
        val dtos: List<KtorRadioBrowserCountryDto> = client.get("https://$host/json/countries") {
            header("User-Agent", "AmberTuner/1.0 (ComposeMultiplatform; educational project)")
        }.body()

        return dtos.mapNotNull { dto ->
            val name = dto.name ?: return@mapNotNull null
            CountryOption(
                name = name,
                stationCount = dto.stationCount ?: 0,
                code = dto.countryCode
            )
        }
    }
}
