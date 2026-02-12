package com.satwik.aimemory.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

/**
 * Retrofit interface for the OpenClaw backend API.
 * Base URL points to VPS Tailscale IP on port 8000.
 */
interface OpenClawApi {

    /**
     * Upload an audio chunk as a multipart WAV file.
     * The backend processes the audio through Sarvam AI STT.
     *
     * Field names must match the FastAPI endpoint:
     *   audio: UploadFile = File(...)
     *   timestamp: str = Form(...)
     *   device_id: str = Form(...)
     */
    @Multipart
    @POST("audio/upload")
    suspend fun uploadAudio(
        @Part audio: MultipartBody.Part,
        @Part("timestamp") timestamp: RequestBody,
        @Part("device_id") deviceId: RequestBody
    ): Response<UploadResponse>

    /**
     * Retrieve the most recent hourly summary.
     */
    @GET("summary/latest")
    suspend fun getLatestSummary(): Response<SummaryResponse>

    /**
     * Health check endpoint to verify backend connectivity.
     * Returns 200 with status "ok" when the backend is reachable.
     */
    @GET("health")
    suspend fun healthCheck(): Response<HealthResponse>

    /**
     * Submit a natural language query to search across memories.
     */
    @POST("query")
    suspend fun submitQuery(
        @retrofit2.http.Body request: QueryRequest
    ): Response<QueryResponse>
}
