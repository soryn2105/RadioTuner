package com.sorin.radiotuner.data.model

import kotlinx.serialization.Serializable

/**
 * (RO) Model de date pur — reprezintă un post de radio.
 * Serializabil prin kotlinx.serialization pentru compatibilitate multiplatformă (Android + iOS).
 */
@Serializable
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

/**
 * (RO) Metadatele afișate în ticker-ul "RDS" în timpul redării.
 */
@Serializable
data class RdsInfo(
    val text: String,
    val isAvailable: Boolean
)

/**
 * (RO) O țară din care se pot filtra posturile.
 */
@Serializable
data class CountryOption(
    val name: String,
    val stationCount: Int,
    val code: String? = null
)
