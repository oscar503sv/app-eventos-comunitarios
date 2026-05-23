package com.eventos.comunitarios.data.repository

import com.eventos.comunitarios.data.model.*
import com.eventos.comunitarios.data.network.EventsApi
import com.eventos.comunitarios.data.network.RetrofitClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.HttpException
import java.io.IOException

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val code: Int, val message: String) : ApiResult<Nothing>()
}

class EventsRepository(
    private val api: EventsApi = RetrofitClient.api,
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
) {
    private val errorAdapter = moshi.adapter(ApiError::class.java)

    suspend fun getEvents(page: Int = 1, limit: Int = 20): ApiResult<EventsResponse> = safeCall {
        api.getEvents(page, limit)
    }

    suspend fun getEventById(id: String): ApiResult<EventDetail> = safeCall {
        api.getEventById(id).event
    }

    suspend fun createEvent(
        title: String,
        description: String?,
        date: String,
        location: String,
        category: String?
    ): ApiResult<Event> = safeCall {
        api.createEvent(CreateEventRequest(title, description, date, location, category)).event
    }

    suspend fun updateEvent(
        id: String,
        title: String,
        description: String?,
        date: String,
        location: String,
        category: String?
    ): ApiResult<Event> = safeCall {
        api.updateEvent(id, UpdateEventRequest(title, description, date, location, category)).event
    }

    suspend fun deleteEvent(id: String): ApiResult<SuccessResponse> = safeCall {
        api.deleteEvent(id)
    }

    suspend fun attend(eventId: String): ApiResult<EventAttendance> = safeCall {
        api.attendEvent(eventId).attendance
    }

    suspend fun cancelAttendance(eventId: String): ApiResult<EventAttendance> = safeCall {
        api.cancelAttendance(eventId).attendance
    }

    suspend fun getMyEvents(): ApiResult<com.eventos.comunitarios.data.network.MyEventsResponse> = safeCall {
        api.getMyEvents()
    }

    suspend fun getProfile(): ApiResult<UserProfile> = safeCall {
        api.getProfile().profile
    }

    suspend fun updateProfile(displayName: String): ApiResult<UserProfile> = safeCall {
        api.updateProfile(UpdateProfileRequest(displayName)).profile
    }

    private suspend fun <T> safeCall(block: suspend () -> T): ApiResult<T> = try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val body = e.response()?.errorBody()?.string().orEmpty()
        val parsed = runCatching { errorAdapter.fromJson(body) }.getOrNull()
        ApiResult.Error(e.code(), parsed?.error ?: "HTTP ${e.code()}")
    } catch (e: IOException) {
        ApiResult.Error(-1, "Sin conexión: ${e.message}")
    } catch (e: Exception) {
        ApiResult.Error(-2, e.message ?: "Error desconocido")
    }
}
