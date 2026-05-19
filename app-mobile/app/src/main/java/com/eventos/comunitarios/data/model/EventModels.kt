package com.eventos.comunitarios.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserPublic(
    val id: String,
    val displayName: String?,
    val email: String?
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
    val organizerId: String,
    val createdAt: String,
    val updatedAt: String,
    val organizer: UserPublic,
    @Json(name = "_count") val count: EventCounts
)

@JsonClass(generateAdapter = true)
data class EventsResponse(
    val success: Boolean,
    val events: List<EventSummary>
)
