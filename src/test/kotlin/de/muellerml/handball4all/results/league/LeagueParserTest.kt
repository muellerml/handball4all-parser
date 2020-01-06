package de.muellerml.handball4all.results.league

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.haveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import kotlinx.coroutines.runBlocking
import java.time.Instant

class LeagueParserTest : AnnotationSpec() {

    private lateinit var parser: LeagueParser

    @BeforeEach
    fun setup() {
        this.parser = LeagueParser()
    }

    @Test
    fun testParsingAllGames() = runBlocking {
        val leagueId = 48941
        val games = parser.allGames(leagueId)
        games should haveSize(132)
        val game = games.first { it.homeTeam == "HSG Sulz-Murr" && it.awayTeam == "HSG Marb/Riel" }
        game.homeGoals shouldBe 24
        game.awayGoals shouldBe 26
        checkNotNull(game.gameTime)
        game.gameTime shouldBe Instant.parse("2019-10-05T18:00:00Z")
        game.orgaId shouldBe 20015

        val gameWithoutResult = games.first { it.orgaId == 20065 }

        gameWithoutResult.homeGoals shouldBe null
        gameWithoutResult.awayGoals shouldBe null
        gameWithoutResult.homeTeam shouldBe "TSV Asperg"
        gameWithoutResult.awayTeam shouldBe "TV Tamm"
        gameWithoutResult.gameTime shouldBe null
    }

    @Test
    fun testCurrentGames() = runBlocking {
        parser.currentGames(48941).run {
            currentGames shouldHaveSize 3
            futureGames shouldHaveSize 3
        }
    }
}
