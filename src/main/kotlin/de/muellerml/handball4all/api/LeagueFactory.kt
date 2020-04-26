package de.muellerml.handball4all.api

import de.muellerml.handball4all.results.league.LeagueParser
import de.muellerml.handball4all.results.league.ParsedGame

interface LeagueFactory {
    suspend fun allGames(): List<ParsedGame>
}

internal class LeagueFactoryImpl(private val leagueId: Int) : LeagueFactory {

    private val leagueParser: LeagueParser = LeagueParser()

    override suspend fun allGames(): List<ParsedGame> {
        return leagueParser.allGames(leagueId)
    }
}
