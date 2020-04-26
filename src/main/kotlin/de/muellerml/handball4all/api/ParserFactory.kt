package de.muellerml.handball4all.api

object ParserFactory {

 fun forLeague(leagueId: Int) : LeagueFactory = LeagueFactoryImpl(leagueId = leagueId)

}


