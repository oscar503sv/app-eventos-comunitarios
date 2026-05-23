package com.eventos.comunitarios.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User(
    val id: String,
    val firebaseUid: String,
    val email: String,
    val displayName: String?,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class UserPublic(
    val id: String,
    val firebaseUid: String? = null,
    val displayName: String?,
    val email: String? = null
)

@JsonClass(generateAdapter = true)
data class EventCounts(
    val attendances: Int,
    val reviews: Int
)

@JsonClass(generateAdapter = true)
data class EventSummary(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String,
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String,
    val organizer: UserPublic,
    @Json(name = "_count") val count: EventCounts
)

@JsonClass(generateAdapter = true)
data class ProfileCounts(
    val organized: Int,
    val attended: Int,
    val reviews: Int
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    val user: User,
    val counts: ProfileCounts
)

@JsonClass(generateAdapter = true)
data class ProfileResponse(
    val success: Boolean,
    val profile: UserProfile
)

@JsonClass(generateAdapter = true)
data class Pagination(
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

@JsonClass(generateAdapter = true)
data class EventsResponse(
    val success: Boolean,
    val events: List<EventSummary>,
    val pagination: Pagination
)

@JsonClass(generateAdapter = true)
data class EventAttendance(
    val id: String,
    val userId: String,
    val eventId: String,
    val status: String,       // "confirmed" | "cancelled"
    val createdAt: String
)

@JsonClass(generateAdapter = true)
data class EventAttendanceWithUser(
    val id: String,
    val userId: String,
    val eventId: String,
    val status: String,
    val createdAt: String,
    val user: UserPublic
)

@JsonClass(generateAdapter = true)
data class ReviewWithUser(
    val id: String,
    val userId: String,
    val eventId: String,
    val rating: Int,
    val comment: String?,
    val createdAt: String,
    val user: UserPublic
)

@JsonClass(generateAdapter = true)
data class EventDetail(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String,
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String,
    val organizer: UserPublic,
    val attendances: List<EventAttendanceWithUser>,
    val reviews: List<ReviewWithUser>
)

@JsonClass(generateAdapter = true)
data class EventDetailResponse(
    val success: Boolean,
    val event: EventDetail
)

@JsonClass(generateAdapter = true)
data class Event(
    val id: String,
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String,
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String
)

@JsonClass(generateAdapter = true)
data class EventResponse(
    val success: Boolean,
    val event: Event
)

@JsonClass(generateAdapter = true)
data class AttendanceResponse(
    val success: Boolean,
    val attendance: EventAttendance
)

@JsonClass(generateAdapter = true)
data class CreateEventRequest(
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateEventRequest(
    val title: String,
    val description: String?,
    val date: String,
    val location: String,
    val category: String? = null
)

@JsonClass(generateAdapter = true)
data class SuccessResponse(
    val success: Boolean,
    val message: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val displayName: String
)

object EventCategory {
    const val CULTURA = "CULTURA"
    const val MUSICA = "MUSICA"
    const val DEPORTE = "DEPORTE"
    const val EDUCACION = "EDUCACION"
    const val GASTRONOMIA = "GASTRONOMIA"
    const val SALUD = "SALUD"
    const val OTRO = "OTRO"

    val all = listOf(CULTURA, MUSICA, DEPORTE, EDUCACION, GASTRONOMIA, SALUD, OTRO)
}

@JsonClass(generateAdapter = true)
data class ApiError(val error: String)
