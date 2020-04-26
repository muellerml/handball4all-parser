package de.muellerml.handball4all.report

import org.apache.pdfbox.pdmodel.PDPage
import java.time.Duration
import java.time.LocalTime

internal interface H4aEventParser {
    fun parseEvents(page: PDPage): Iterable<ParsedEvent<*>>
}

private data class EventParseState(val state: STATE = STATE.START,
                                   val actionDateTime: LocalTime = LocalTime.MIN,
                                   val actionGameTime: Duration = Duration.ZERO,
                                   val score: Pair<Int, Int> = 0 to 0,
                                   private val _events: MutableList<ParsedEvent<*>> = mutableListOf()) {
    fun addEvent(event: ParsedEvent<*>) = _events.add(event)

    val events
        get() = _events
}

internal class H4aEventParserImpl : H4aEventParser {

    override fun parseEvents(page : PDPage) : Iterable<ParsedEvent<*>> {
        val streams = page.contentStreams
        return streams.asSequence().fold(EventParseState()) { globalParseState, stream ->
            stream.cosObject.toTextString().lines().fold(globalParseState) { state, line ->
                when (state.state) {
                    STATE.START ->  {
                        val actionDateTime = this.extractRealTime(line) ?: state.actionDateTime
                        val parseState = when (actionDateTime == LocalTime.MIN) {
                            true -> STATE.START
                            false -> STATE.REAL_TIME
                        }
                        state.copy(state = parseState, actionDateTime = actionDateTime)
                    }
                    STATE.REAL_TIME -> {
                        val actionGameTime = this.extractGameTime(line) ?: state.actionGameTime
                        state.copy(state = STATE.GAME_TIME, actionGameTime = actionGameTime)
                    }
                    STATE.GAME_TIME -> {
                        val score = this.extractScore(line, state.score) ?: state.score
                        state.copy(state = STATE.SCORE, score = score)
                    }
                    STATE.SCORE -> {
                        val action = this.extractAction(line, state.actionDateTime, state.actionGameTime, state.score)
                        if (action != null) {
                            state.addEvent(action)
                        }
                        state.copy(state = STATE.START)
                    }
                }
            }
        }.events
    }

    private fun extractAction(action: String,
                              actionDateTime: LocalTime,
                              actionGameTime: Duration,
                              score: Pair<Int, Int>) : ParsedEvent<*>? {
        val actionGroups = actionRegex.find(action)?.groupValues
        return if(actionGroups != null) {
            val event = actionGroups.getOrNull(EVENT_INDEX)
            if(event != null) {
                val eventType = when (event) {
                    "Tor" -> EventType.GOAL
                    "7m-Tor" -> EventType.SEVENM_GOAL
                    "7m, KEIN Tor" -> EventType.SEVENM_NO_GOAL
                    "2-min Strafe" -> EventType.TWO_MIN
                    "Verwarnung" -> EventType.YELLOW
                    else -> error("Unknown event type: $event")
                }
                val name = actionGroups[NAME_INDEX].trim().split(" ").joinToString(" ") { it.capitalize() }.trim().ifEmpty { null }
                val number = actionGroups[NUMBER_INDEX].trim().ifEmpty { null }
                ParsedPersonEvent(action = eventType, actor = if (number == null) null else ParsedPerson(number = number,
                        firstname = name!!.substringBeforeLast(" "),
                        lastname = name.substringAfterLast(" ")),
                        actionDateTime = actionDateTime,
                        gameTime = actionGameTime,
                        score = score)
            } else null
        } else {
            val timeoutTeam = timeoutRegex.find(action)?.groupValues?.getOrNull(DEFAULT_VALUE_COLUMN)
            if(timeoutTeam != null) {
                ParsedTeamEvent(action = EventType.TIMEOUT,
                        actor = timeoutTeam,
                        actionDateTime = actionDateTime,
                        gameTime = actionGameTime,
                        score = score)
            } else null
        }
    }

    private fun extractScore(scoreString: String, currentScore: Pair<Int, Int>): Pair<Int, Int>? {
        val score = scoreRegex.find(scoreString)?.groupValues ?: return null
        val homeScore = score.getOrNull(HOME_SCORE_INDEX)?.toInt()
        val awayScore = score.getOrNull(AWAY_SCORE_INDEX)?.toInt()
        return Pair(homeScore ?: currentScore.first, awayScore ?: currentScore.second)
    }

    private fun extractGameTime(time: String) : Duration? {
        val gameTime = gameTimeRegex.find(time)?.groupValues?.getOrNull(DEFAULT_VALUE_COLUMN) ?: return null
        val timeArray : List<Long> = gameTime.split(TIME_SPLITTER).map { Integer.valueOf(it).toLong() }
        return Duration.ofMinutes(timeArray[HOURS_INDEX]).plusSeconds(timeArray[MINUTES_INDEX])
    }

    private fun extractRealTime(time: String) : LocalTime? {
        val dateTime = timeRegex.find(time)?.groupValues?.getOrNull(DEFAULT_VALUE_COLUMN) ?: return null
        val timeArray = dateTime.split(TIME_SPLITTER).map { Integer.valueOf(it) }
        return LocalTime.of(timeArray[HOURS_INDEX], timeArray[MINUTES_INDEX], timeArray[SECONDS_INDEX])
    }
}



enum class STATE {
    START,
    REAL_TIME,
    GAME_TIME,
    SCORE,
}

private val actionRegex = Regex(".*\\((.*) (?:für|durch) ((.*)\\\\\\((.+), (.+)\\\\\\)\\).*)?")
private val timeRegex = Regex(".*\\((\\d\\d:\\d\\d:\\d\\d)\\).*")
private val gameTimeRegex = Regex(".*\\((\\d\\d:\\d\\d)\\).*")
private val timeoutRegex = Regex(".*\\(Auszeit (.*)\\).*")
private val scoreRegex = Regex(".*\\((\\d?\\d):(\\d?\\d)\\).*")

private const val DEFAULT_VALUE_COLUMN = 1

private const val HOURS_INDEX = 0
private const val MINUTES_INDEX = 1
private const val SECONDS_INDEX = 2

private const val EVENT_INDEX = 1
private const val NAME_INDEX = 3
private const val NUMBER_INDEX = 4

private const val TIME_SPLITTER = ":"

private const val HOME_SCORE_INDEX = 1
private const val AWAY_SCORE_INDEX = 2
