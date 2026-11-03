package uk.lloyd.tvhome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One match, as football-data.org describes it. */
data class Fixture(
    val id: Long,
    val competition: String,
    val homeFull: String,
    val awayFull: String,
    val home: String,
    val away: String,
    val kickoff: Instant,
    val status: String,
    val homeScore: Int?,
    val awayScore: Int?,
    val minute: Int?,
) {
    val live: Boolean get() = status == "IN_PLAY" || status == "PAUSED"
    val upcoming: Boolean get() = status == "SCHEDULED" || status == "TIMED"
    val finished: Boolean get() = status == "FINISHED" || status == "AWARDED"
}

/** One row of the Next up card. [result] marks a full-time score shown for a few hours after a match. */
data class TeamLine(
    val label: String,
    val initials: String,
    val detail: String,
    val live: Boolean,
    val result: Boolean = false,
)

/** What the Next up card shows. */
data class FootballView(val lines: List<TeamLine>)

class FootballHttpError(val code: Int) : Exception("HTTP $code")

/** Thin client for football-data.org v4 (free tier: Premier League, Championship, World Cup, Euros). */
object FootballApi {
    private const val BASE = "https://api.football-data.org/v4"

    private suspend fun get(path: String, key: String): JSONObject = withContext(Dispatchers.IO) {
        val conn = URL(BASE + path).openConnection() as HttpURLConnection
        conn.setRequestProperty("X-Auth-Token", key)
        conn.connectTimeout = 10_000
        conn.readTimeout = 20_000
        try {
            val code = conn.responseCode
            if (code != 200) throw FootballHttpError(code)
            JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
        } finally {
            conn.disconnect()
        }
    }

    /** Every match in a competition this season. */
    suspend fun competition(code: String, key: String, query: String = ""): List<Fixture> {
        val json = get("/competitions/$code/matches$query", key)
        val arr = json.optJSONArray("matches") ?: return emptyList()
        return (0 until arr.length()).mapNotNull { i -> parse(arr.optJSONObject(i), code) }
    }

    private fun JSONObject.intOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun parse(m: JSONObject?, competition: String): Fixture? = runCatching {
        m ?: return@runCatching null
        val homeTeam = m.getJSONObject("homeTeam")
        val awayTeam = m.getJSONObject("awayTeam")
        val fullTime = m.optJSONObject("score")?.optJSONObject("fullTime")
        Fixture(
            id = m.getLong("id"),
            competition = competition,
            homeFull = homeTeam.optString("name"),
            awayFull = awayTeam.optString("name"),
            home = shortName(homeTeam),
            away = shortName(awayTeam),
            kickoff = Instant.parse(m.getString("utcDate")),
            status = m.optString("status"),
            homeScore = fullTime?.intOrNull("home"),
            awayScore = fullTime?.intOrNull("away"),
            minute = m.intOrNull("minute"),
        )
    }.getOrNull()

    private fun shortName(team: JSONObject): String {
        val s = team.optString("shortName").takeIf { it.isNotBlank() && it != "null" }
        val n = team.optString("name").takeIf { it.isNotBlank() && it != "null" }
        return s ?: n ?: "TBC"
    }
}

/**
 * Keeps the Next up card current while using as few requests as possible:
 * full season lists every few hours, then one small request per tracked match
 * each minute only while that match is on (or about to start).
 */
class FootballTracker(private val key: String) {

    private data class Team(val label: String, val initials: String, val nameKey: String, val comps: List<String>)

    private val teams = listOf(
        Team("Norwich", "NC", "Norwich", listOf("PL", "ELC")),
        Team("Forest", "NF", "Nottingham Forest", listOf("PL", "ELC")),
        Team("England", "EN", "England", listOf("WC", "EC")),
    )

    private val fixtures = mutableMapOf<String, List<Fixture>>()
    private val lastFull = mutableMapOf<String, Long>()
    private var keyRejected = false
    private var everLoaded = false
    private var activeWindow = false

    private val refreshEvery = mapOf(
        "PL" to Duration.ofHours(3).toMillis(),
        "ELC" to Duration.ofHours(3).toMillis(),
        "WC" to Duration.ofHours(12).toMillis(),
        "EC" to Duration.ofHours(12).toMillis(),
    )

    /** How long to wait before the next cycle. */
    fun nextDelayMillis(): Long = if (activeWindow) 60_000L else 10 * 60_000L

    suspend fun cycle(): FootballView {
        val nowMs = System.currentTimeMillis()

        // 1. Season lists, when due.
        for ((code, every) in refreshEvery) {
            if (nowMs - (lastFull[code] ?: 0L) < every) continue
            try {
                fixtures[code] = FootballApi.competition(code, key)
                lastFull[code] = nowMs
                everLoaded = true
                keyRejected = false
            } catch (e: FootballHttpError) {
                when (e.code) {
                    400, 401 -> keyRejected = true
                    // 403/404 on WC/EC just means no tournament in the free plan right now.
                    403, 404 -> if (code == "PL" || code == "ELC") keyRejected = true else lastFull[code] = nowMs
                    else -> {} // 429 or server trouble: keep what we have, try again next cycle
                }
            } catch (e: Exception) {
                // No connection: keep what we have.
            }
        }

        // 2. Live updates: one small request per competition that has a tracked match on now
        //    (yesterday–today in UTC, so late kick-offs abroad are still covered).
        val now = Instant.now()
        val tracked = teams.flatMap { t -> matchesFor(t) } + fixtures["PL"].orEmpty()
        val liveComps = tracked.filter { inWindow(it, now) }.map { it.competition }.toSet()
        if (liveComps.isNotEmpty()) {
            val today = LocalDate.now(ZoneId.of("UTC"))
            val range = "?dateFrom=${today.minusDays(1)}&dateTo=$today"
            for (code in liveComps) {
                runCatching { FootballApi.competition(code, key, range) }.getOrNull()?.forEach { replace(it) }
            }
        }
        activeWindow = liveComps.isNotEmpty()

        return view()
    }

    private fun inWindow(f: Fixture, now: Instant): Boolean =
        !f.finished &&
            now.isAfter(f.kickoff.minus(Duration.ofMinutes(10))) &&
            now.isBefore(f.kickoff.plus(Duration.ofMinutes(180)))

    private fun replace(updated: Fixture) {
        val list = fixtures[updated.competition] ?: return
        fixtures[updated.competition] = list.map { if (it.id == updated.id) updated else it }
    }

    private fun matchesFor(t: Team): List<Fixture> =
        t.comps.flatMap { fixtures[it].orEmpty() }
            .filter { it.homeFull.contains(t.nameKey, ignoreCase = true) || it.awayFull.contains(t.nameKey, ignoreCase = true) }

    private fun view(): FootballView {
        if (keyRejected && !everLoaded) {
            return FootballView(placeholder("Key not accepted – check Setup"))
        }
        if (!everLoaded) {
            return FootballView(placeholder("Can't reach scores yet"))
        }
        val now = Instant.now()
        val lines = teams.map { t ->
            val mine = matchesFor(t)
            // A "live" status more than 3 hours after kick-off is stale data, not a match still on.
            val live = mine.firstOrNull { it.live && now.isBefore(it.kickoff.plus(Duration.ofHours(3))) }
            val recent = mine.filter { it.finished && now.isBefore(it.kickoff.plus(Duration.ofHours(5))) }
                .maxByOrNull { it.kickoff }
            val next = mine.filter { it.upcoming && it.kickoff.isAfter(now.minus(Duration.ofHours(3))) }
                .minByOrNull { it.kickoff }
            when {
                live != null -> TeamLine(t.label, t.initials, liveDetail(live, t.nameKey), true)
                recent != null -> TeamLine(t.label, t.initials, resultDetail(recent, t.nameKey), false, result = true)
                next != null -> TeamLine(t.label, t.initials, nextDetail(next, t.nameKey), false)
                t.label == "England" -> TeamLine(t.label, t.initials, "Between tournaments", false)
                else -> TeamLine(t.label, t.initials, "No fixtures listed", false)
            }
        } + premierLeagueLine(now)
        return FootballView(lines)
    }

    private fun premierLeagueLine(now: Instant): TeamLine {
        val pl = fixtures["PL"].orEmpty()
        val liveCount = pl.count { it.live && now.isBefore(it.kickoff.plus(Duration.ofHours(3))) }
        if (liveCount > 0) {
            return TeamLine("Prem", "PL", if (liveCount == 1) "1 game live" else "$liveCount games live", true)
        }
        val next = pl.filter { it.upcoming && it.kickoff.isAfter(now) }.minByOrNull { it.kickoff }
            ?: return TeamLine("Prem", "PL", "No fixtures listed", false)
        return TeamLine("Prem", "PL", "${next.home} v ${next.away} · ${whenText(next.kickoff)}", false)
    }

    private fun liveDetail(f: Fixture, nameKey: String): String {
        val isHome = f.homeFull.contains(nameKey, ignoreCase = true)
        val ours = (if (isHome) f.homeScore else f.awayScore) ?: 0
        val theirs = (if (isHome) f.awayScore else f.homeScore) ?: 0
        val opponent = if (isHome) f.away else f.home
        val clock = when {
            f.status == "PAUSED" -> "HT"
            f.minute != null -> "${f.minute}'"
            else -> "live"
        }
        return "$ours–$theirs v $opponent · $clock"
    }

    private fun resultDetail(f: Fixture, nameKey: String): String {
        val isHome = f.homeFull.contains(nameKey, ignoreCase = true)
        val ours = (if (isHome) f.homeScore else f.awayScore) ?: 0
        val theirs = (if (isHome) f.awayScore else f.homeScore) ?: 0
        val opponent = if (isHome) f.away else f.home
        val outcome = when {
            ours > theirs -> "Won"
            ours < theirs -> "Lost"
            else -> "Drew"
        }
        return "$outcome $ours–$theirs v $opponent"
    }

    private fun nextDetail(f: Fixture, nameKey: String): String {
        val isHome = f.homeFull.contains(nameKey, ignoreCase = true)
        val opponent = if (isHome) f.away else f.home
        return "v $opponent (${if (isHome) "H" else "A"}) · ${whenText(f.kickoff)}"
    }

    private fun placeholder(detail: String) =
        teams.map { TeamLine(it.label, it.initials, detail, false) } + TeamLine("Prem", "PL", detail, false)

    companion object {
        /** What the card shows before a key is entered. */
        fun noKeyView() = FootballView(
            listOf(
                TeamLine("Norwich", "NC", "Add free key in Setup", false),
                TeamLine("Forest", "NF", "Add free key in Setup", false),
                TeamLine("England", "EN", "Add free key in Setup", false),
                TeamLine("Prem", "PL", "Add free key in Setup", false),
            ),
        )

        /** "Today 15:00", "Tomorrow 19:45", "Sat 12:30" or "Sat 14 Nov". */
        fun whenText(instant: Instant): String {
            val zone = ZoneId.systemDefault()
            val local = instant.atZone(zone)
            val today = LocalDate.now(zone)
            val time = local.format(DateTimeFormatter.ofPattern("HH:mm", Locale.UK))
            return when (local.toLocalDate()) {
                today -> "Today $time"
                today.plusDays(1) -> "Tomorrow $time"
                in today.plusDays(2)..today.plusDays(6) ->
                    local.format(DateTimeFormatter.ofPattern("EEE", Locale.UK)) + " $time"
                else -> local.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))
            }
        }
    }
}
