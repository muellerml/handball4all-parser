package de.muellerml.handball4all.results

import de.muellerml.handball4all.results.league.ParsedGame

data class CurrentGameHolder(val currentGames: List<ParsedGame>, val futureGames: List<ParsedGame>)
