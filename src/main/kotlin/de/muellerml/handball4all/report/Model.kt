package de.muellerml.handball4all.report

import java.time.Duration
import java.time.LocalTime

/** holds person as map from number/offical letter to concrete person actor **/
data class ParsedReport(private val homePersons: Map<ParsedPerson, List<ParsedEvent<*>>>,
                        private val awayPersons: Map<ParsedPerson, List<ParsedEvent<*>>>,
                        val homeTeam: String,
                        val awayTeam: String,
                        val parsedEvents: List<ParsedEvent<*>>) {

    fun personsForTeam(team: String) = if(homeTeam == team) homePersons.keys else awayPersons.keys

    fun eventsFor(person: ParsedPerson) : List<ParsedEvent<*>> = homePersons[person] ?: awayPersons[person] ?: error("Unknown person in report: $person")
    fun eventsFor(person: ParsedPerson, type: EventType) : List<ParsedEvent<*>> {
        val eventsList = homePersons[person] ?: awayPersons[person] ?: error("Unknown person in report: $person")
        return eventsList.filter { it.action == type }
    }
}


interface ParsedEvent<T> {
    val action: EventType
    val actor: T?
    val actionDateTime: LocalTime
    val gameTime: Duration
    val score: Pair<Int, Int>
}

data class ParsedPersonEvent(override val action: EventType,
                             override val actor: ParsedPerson?,
                             override val actionDateTime: LocalTime,
                             override val gameTime: Duration,
                             override val score: Pair<Int, Int>) : ParsedEvent<ParsedPerson>

data class ParsedTeamEvent(override val action: EventType,
                           override val actor: String?,
                           override val actionDateTime: LocalTime,
                           override val gameTime: Duration,
                           override val score: Pair<Int, Int>) : ParsedEvent<String>
enum class EventType {
    YELLOW,
    RED,
    TWO_MIN,
    GOAL,
    SEVENM_GOAL,
    SEVENM_NO_GOAL,
    TIMEOUT
}

data class ParsedPerson(val number: String, val firstname: String, val lastname: String) {
    val name = "$firstname $lastname"
}

data class GameStatistics(val parsedPerson: ParsedPerson,
                          val goals: Int,
                          val yellow: GameTime?,
                          val twoMinutes: List<GameTime>,
                          val red: GameTime?,
                          val blue: GameTime?)

data class GameTime(val minutes: Int, val seconds: Int)
