package com.sorin.radiotuner.data.network

import retrofit2.http.GET

interface GiteaUpdateApi {
    /**
     * (RO) Obține ultimul release public din Gitea.
     * Schimbă 'tiky/RadioTuner' dacă repo-ul tău are alt nume/proprietar.
     */
    @GET("api/v1/repos/tiky/RadioTuner/releases/latest")
    suspend fun getLatestRelease(): GiteaReleaseDto
}
