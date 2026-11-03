package uk.lloyd.tvhome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Weather(
    val tempC: Int,
    val summary: String,
    val icon: IconKind,
    val highC: Int?,
    val lowC: Int?,
    val rainChance: Int?,
)

/** Current weather and today's range for Horsford, from Open-Meteo (free, no account). */
object WeatherService {
    private const val URL_STRING =
        "https://api.open-meteo.com/v1/forecast?latitude=52.69&longitude=1.24" +
            "&current=temperature_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&forecast_days=1&timezone=Europe%2FLondon"

    suspend fun fetch(): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL(URL_STRING).openConnection() as HttpURLConnection
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val daily = json.optJSONObject("daily")
                val code = current.getInt("weather_code")
                fun dailyInt(key: String): Int? =
                    daily?.optJSONArray(key)?.takeIf { it.length() > 0 && !it.isNull(0) }
                        ?.let { Math.round(it.getDouble(0)).toInt() }
                Weather(
                    tempC = Math.round(current.getDouble("temperature_2m")).toInt(),
                    summary = describe(code),
                    icon = iconFor(code),
                    highC = dailyInt("temperature_2m_max"),
                    lowC = dailyInt("temperature_2m_min"),
                    rainChance = dailyInt("precipitation_probability_max"),
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

    private fun iconFor(code: Int): IconKind = when (code) {
        0, 1 -> IconKind.Sun
        2, 3, 45, 48 -> IconKind.Cloud
        else -> IconKind.Rain
    }
}
