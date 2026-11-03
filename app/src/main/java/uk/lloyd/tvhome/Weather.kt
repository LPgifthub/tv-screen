package uk.lloyd.tvhome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(val tempC: Int, val summary: String)

/** Current weather for Horsford from Open-Meteo (free, no account needed). */
object WeatherService {
    private const val URL_STRING =
        "https://api.open-meteo.com/v1/forecast?latitude=52.69&longitude=1.24" +
            "&current=temperature_2m,weather_code&timezone=Europe%2FLondon"

    suspend fun fetch(): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(URL_STRING).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                Weather(
                    tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
                    summary = describe(current.getInt("weather_code")),
                )
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    private fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorms"
        else -> "—"
    }
}
