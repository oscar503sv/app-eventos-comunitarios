package com.eventos.comunitarios.data.network

import com.eventos.comunitarios.data.model.EventsResponse
import retrofit2.http.GET

interface EventsApi {
    @GET("api/events")
    suspend fun getEvents(): EventsResponse
}
