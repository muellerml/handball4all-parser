package de.muellerml.handball4all.results

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import de.muellerml.handball4all.results.league.H4AGame
import de.muellerml.handball4all.results.league.ParsedLeague
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.http.ContentType
import java.time.Instant
import java.time.ZoneId

private const val CLUB_SEARCH_URL = "https://spo.handball4all.de/service/if_g_json.php?cmd=cs&cs="
private const val CLUB_GAMES_URL = "https://spo.handball4all.de/service/if_g_json.php?c={clubid}&cmd=pcu&do={date}&og=3"

class ClubParser {

    private val client = HttpClient(OkHttp) {
        install(JsonFeature) {
            this.acceptContentTypes = listOf(ContentType.Application.JavaScript)
            serializer = JacksonSerializer {
                findAndRegisterModules()
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }

    suspend fun findClubId(nameOrNumber: String): Int? {
        val result = client.get<List<ClubSearchResultHolder>>(CLUB_SEARCH_URL + nameOrNumber)
        return result.first().searchResult.list.firstOrNull()?.getId()
    }

    suspend fun parseClubGames(nameOrNumber: String, date: Instant = Instant.now()): List<ParsedLeague> {
        val clubId = findClubId(nameOrNumber) ?: throw IllegalStateException("Unknown club $nameOrNumber")
        return this.parseClubGames(id = clubId, date = date)
    }

    suspend fun parseClubGames(id: Int, date: Instant = Instant.now()): List<ParsedLeague> {
        val formattedDate = date.atZone(ZoneId.systemDefault()).run {
            "$year-$monthValue-$dayOfMonth"
        }
        val url = CLUB_GAMES_URL.replace("{clubid}", id.toString()).replace("{date}", formattedDate)
        return client.get<List<ClubGamesHolder>>(url).firstOrNull()?.leagues ?: listOf()
    }
}

data class ClubSearchResultHolder(val searchResult: ClubSearchResultPage) {

}

data class ClubSearchResultPage(val list: List<ClubSearchResult>)

data class ClubSearchResult(private val id: String) {
    fun getId() = id.toInt()
}


data class ClubGamesHolder(private val content: ClubGamesContentHolder, val head: Any) {
    val leagues
        get() = content.leagues
}
data class ClubGamesContentHolder(@JsonProperty("classes") private val classes: List<ClubLeagueClass>) {
    val leagues
        get() = classes.map { it.toLeague() }
}

data class ClubLeagueClass(private val gClassID: String,
                           private val gClassSname: String,
                           private val gClassLname: String,
                           private val gClassGender: String,
                           private val gClassAGsDesc: String,
                         val games: List<H4AGame>) {
    fun toLeague() = ParsedLeague(
            orgaid = gClassID.toInt(),
            shortName = gClassSname,
            longName = gClassLname,
            ageclass = gClassAGsDesc,
            gender = gClassGender,
            games = games.map { it.normalized }
    )
}

