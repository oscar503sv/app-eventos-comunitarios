package com.eventos.comunitarios.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val formatter = DateTimeFormatter.ofPattern("EEE, d MMM · HH:mm", Locale("es", "ES"))
    private val fullFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale("es", "ES"))
    private val dayFormatter = DateTimeFormatter.ofPattern("d", Locale.ENGLISH)
    private val monthFormatter = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    fun formatToShort(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            instant.atZone(ZoneId.systemDefault()).format(formatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatToFull(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            instant.atZone(ZoneId.systemDefault()).format(fullFormatter)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatTime(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            instant.atZone(ZoneId.systemDefault()).format(timeFormatter)
        } catch (e: Exception) {
            ""
        }
    }

    fun getDay(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            instant.atZone(ZoneId.systemDefault()).format(dayFormatter)
        } catch (e: Exception) {
            ""
        }
    }

    fun getMonth(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            instant.atZone(ZoneId.systemDefault()).format(monthFormatter).uppercase()
        } catch (e: Exception) {
            ""
        }
    }
}
