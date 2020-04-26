package de.muellerml.handball4all.results.league

import com.fasterxml.jackson.databind.DeserializationFeature
import de.muellerml.handball4all.results.CurrentGameHolder
import de.muellerml.handball4all.util.detectBreakingApiChanges
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.url
import io.ktor.http.ContentType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val ALL_GAMES_URL = "https://spo.handball4all.de/service/if_g_json.php?ca=1&cl={leagueId}&cmd=ps&og=3"
private const val CURRENT_GAMES_URL = "https://spo.handball4all.de/service/if_g_json.php?ca=0&cl={leagueId}&cmd=ps&og=3"

class LeagueParser {

    private val client = HttpClient(OkHttp) {
        install(JsonFeature) {
            this.acceptContentTypes = listOf(ContentType.Application.JavaScript)
            serializer = JacksonSerializer {
                findAndRegisterModules()
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

    internal suspend fun allGames(leagueId: Int): List<ParsedGame>
        = detectBreakingApiChanges {
        client.get<List<H4aCompleteParsedLeague>> {
            url(ALL_GAMES_URL.replace("{leagueId}", leagueId.toString()))
        }.first().allGames()
    }

    internal suspend fun currentGames(leagueId: Int, date: Instant = Instant.now()): CurrentGameHolder
        = detectBreakingApiChanges {
        client.get<List<H4aPartlyParsedLeague>>(CURRENT_GAMES_URL.replace("{leagueId}", leagueId.toString()))
                .first()
                .currentGames()
    }
}

internal data class H4aPartlyParsedLeague(private val content: H4APartlyLeagueContent, val head: H4aLeagueHeader) {
    fun currentGames() = CurrentGameHolder(
            currentGames = content.currentGames(),
            futureGames = content.futureGames())
}

internal data class H4aCompleteParsedLeague(private val content : H4ACompleteLeagueContent, val head: H4aLeagueHeader) {
    fun allGames() = content.allGames()
}
internal data class H4aLeagueHeader(private val name: String,
                                    private val sname: String,
                                    private val headline1: String?,
                                    private val headline2: String)

internal data class H4APartlyLeagueContent(private val futureGames: H4AGameHolder,
                                     private val actualGames: H4AGameHolder?,
                                     private val scoreComments: List<String>) {
    fun currentGames(): List<ParsedGame> = actualGames?.parsedGames.orEmpty()
    fun futureGames(): List<ParsedGame> = futureGames.parsedGames
}
internal data class H4ACompleteLeagueContent(private val futureGames: H4AGameHolder,
                                     private val scoreComments: List<String>) {
    fun allGames(): List<ParsedGame> = futureGames.parsedGames
}

internal data class H4AGameHolder(private val gClassID: Int,
                         private val games: List<H4AGame>) {
    val parsedGames
            get() = games.map { game -> game.normalized }
}

data class ParsedLeague(
        val orgaid: Int,
        val shortName: String,
        val longName: String,
        val gender: String,
        val ageclass: String,
        val games: List<ParsedGame>
)

data class ParsedGame(
        val orgaId: Int,
        val homeTeam: String,
        val awayTeam: String,
        val homeGoals: Int?,
        val homePoints: Int?,
        val awayGoals: Int?,
        val awayPoints: Int?,
        val gameTime: Instant?,
        val gymNumber: Int?,
        val reportId: String?,
        val referees: List<String>?
) {
    val played: Boolean
        get() = homePoints != null && awayPoints != null
}

data class H4AGame(val gNo: String,
                   val gGuestTeam: String,
                   val gHomeTeam: String,
                   val gHomeGoals: String?,
                   val gHomePoints: String?,
                   val gGuestPoints: String?,
                   val gGuestGoals: String?,
                   val gDate: String?,
                   val gTime: String?,
                   val gGymnasiumNo: String?,
                   val sGID: String?,
                   val gReferee: String?
) {
    val normalized = ParsedGame(orgaId = gNo.toInt(),
            homeTeam = gHomeTeam,
            awayTeam = gGuestTeam,
            homeGoals = gHomeGoals?.toIntOrNull(),
            homePoints = gHomePoints?.toIntOrNull(),
            awayGoals = gGuestGoals?.toIntOrNull(),
            awayPoints = gGuestPoints?.toIntOrNull(),
            gameTime = Pair(gDate, gTime).toInstant(),
            gymNumber = gGymnasiumNo?.toIntOrNull(),
            reportId = sGID,
            referees = if(gReferee.isNullOrBlank()) null else gReferee.trim().split(",").map { it.trim() }
    )
}

private fun Pair<String?, String?>.toInstant(): Instant? {
    val (date, time) = this
    return when {
        date != null && date != "" && time != null && time != "" -> ZonedDateTime.of(
                LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yy")),
                LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm")),
                ZoneId.of("Europe/Berlin")
        ).toInstant()
        date != null && date != "" -> ZonedDateTime.of(LocalDate.parse(date, DateTimeFormatter.ofPattern("dd.MM.yy"))
                .atStartOfDay(), ZoneId.of("Europe/Berlin"))
                .toInstant()
        else -> null
    }

}

