package com.eventos.comunitarios.data.network

import com.eventos.comunitarios.data.model.*
import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface EventsApi {
    @GET("api/events")
    suspend fun getEvents(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): EventsResponse

    @GET("api/events/{id}")
    suspend fun getEventById(@Path("id") id: String): EventDetailResponse

    @POST("api/events")
    suspend fun createEvent(@Body body: CreateEventRequest): EventResponse

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: String,
        @Body body: UpdateEventRequest
    ): EventResponse

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): SuccessResponse

    @POST("api/events/{id}/attend")
    suspend fun attendEvent(@Path("id") id: String): AttendanceResponse

    @POST("api/events/{id}/cancel")
    suspend fun cancelAttendance(@Path("id") id: String): AttendanceResponse

    @GET("api/events/my-events")
    suspend fun getMyEvents(): MyEventsResponse

    @GET("api/users/profile")
    suspend fun getProfile(): ProfileResponse

    @PUT("api/users/profile")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ProfileResponse
}

@JsonClass(generateAdapter = true)
data class MyEventsResponse(
    val success: Boolean,
    val organized: List<Event>,
    val attended: List<AttendedEvent>
)

@JsonClass(generateAdapter = true)
data class AttendedEvent(
    val id: String,
    val userId: String,
    val eventId: String,
    val status: String,
    val createdAt: String,
    val event: Event
)
