package de.muellerml.handball4all.results

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import kotlinx.coroutines.runBlocking
import java.time.Instant

internal class ClubParserTest : AnnotationSpec() {

    private val parser = ClubParser()

    @Test
    fun findClubById() = runBlocking {
        parser.findClubId("200") shouldBe 1352
    }

    @Test
    fun findClubByName() = runBlocking {
        parser.findClubId("HSG Sulzbach-Murrhardt") shouldBe 1352
    }

    @Test
    fun findClubByInvalidName() = runBlocking {
        parser.findClubId("HSG Sulzbach-Murrhardt1") shouldBe null
    }

    @Test
    fun findGamesForClubOnDate() = runBlocking {
        val result = parser.parseClubGames("HSG Sulzbach-Murrhardt", Instant.parse("2020-01-31T20:00:00Z"))
        result shouldHaveSize 16
        result.filter { it.games.isNotEmpty() } shouldHaveSize 7
        result.flatMap { it.games } shouldHaveSize 11
    }

    @Test
    fun findGamesForClubOnDate2() = runBlocking {
        val result = parser.parseClubGames("HSG Sulzbach-Murrhardt", Instant.parse("2020-02-06T00:00:00Z"))
        result shouldHaveSize 16
        result.filter { it.games.isNotEmpty() } shouldHaveSize 7
        result.flatMap { it.games } shouldHaveSize 13
    }
}
