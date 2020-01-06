package de.muellerml.handball4all.report

import org.apache.pdfbox.pdmodel.PDPage
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

interface H4aStatisticsParser {
    fun parseStatisticsPage(page: PDPage): StatisticsParseResult
}

internal class H4aStatisticsParserImpl : H4aStatisticsParser {

    override fun parseStatisticsPage(page: PDPage): StatisticsParseResult {
        val streams = page.contentStreams
        val initialState = DocumentParsingState()
        if(streams.hasNext()) {
            val obj = streams.next().cosObject
            val result = obj.toTextString().lines().fold(initialState) {
                state, line ->
                val parsingState  = extractTeamNames(state, line)
                if (parsingState.currentTeam != null) {
                    val (tableState, parsedPerson) = this.extractPerson(line, parsingState.tableState)
                    parsedPerson?.let { parsingState.addPerson(it) }
                    parsingState.copy(tableState = tableState)
                }  else {
                    parsingState
                }

            }
            val homeName = result.teamNames.homeName ?: throw IllegalStateException("Home name should not be null.")
            val awayName = result.teamNames.awayName ?: throw IllegalStateException("Away name should not be null.")
            return StatisticsParseResult(homePersons = result.parsedPersons[H4aTeamPlace.HOME].orEmpty(),
                    awayPersons = result.parsedPersons[H4aTeamPlace.AWAY].orEmpty(),
                    homeName = homeName,
                    awayName = awayName)
        } else {
            throw IllegalStateException("Invalid pdf object.")
        }


    }


    private fun extractTeamNames(state: DocumentParsingState, line: String) : DocumentParsingState {
        return when(extractTeamPlaceInformation(line)) {
            H4aTeamPlace.HOME -> {
                state.copy(currentTeam = H4aTeamPlace.HOME,
                        tableState = TableState.START,
                        teamNames = state.teamNames.copy(homeName = line.extractNameFromPDF(H4aTeamPlace.HOME.placeName)))
            }
            H4aTeamPlace.AWAY -> {
                state.copy(currentTeam = H4aTeamPlace.AWAY,
                        tableState = TableState.START,
                        teamNames = state.teamNames.copy(awayName = line.extractNameFromPDF(H4aTeamPlace.AWAY.placeName)))
            }
            else -> state
        }
    }



    private fun extractPerson(line: String, tableState: TableState): Pair<TableState, ParsedPerson?> {
        when {
            // check for line ending
            (line == "BT /F1 9.00 Tf ET" || line == "BT /F1 8.00 Tf ET") -> return Pair(TableState.START, null)
            // check if there is a number written in current line
            tableState == TableState.START && NUMBER_REGEX.containsMatchIn(line)
            -> {
                currentNumber = NUMBER_REGEX.find(line)?.groupValues?.get(1)
                return Pair(TableState.NUMBER_PARSED, null)
            }
            // check if there's a name written in the current line
            tableState == TableState.NUMBER_PARSED && NAME_REGEX.containsMatchIn(line) -> {
                val nameResult = NAME_REGEX.find(line) ?: throw IllegalArgumentException()
                return Pair(TableState.NAME_PARSED,
                        ParsedPerson(currentNumber!!, nameResult.groupValues[2].trim().capitalize(), nameResult.groupValues[3].trim().capitalize()))
            }
        }
        return Pair(tableState, null)
    }

    private fun extractTeamPlaceInformation(line: String): H4aTeamPlace?
        = H4aTeamPlace.values().find { line.contains(it.placeName) }
}

private val NUMBER_REGEX = Regex("\\((.+)\\)")
private val NAME_REGEX = Regex("\\(((.+) )+(.+)+\\)")
private var currentNumber : String? = null
private enum class H4aTeamPlace(val placeName: String) {
    HOME("Heim: "),
    AWAY("Gast: ")
}

private enum class TableState {START, NUMBER_PARSED, NAME_PARSED}
private data class TeamNames(val homeName: String?, val awayName: String?)

private data class DocumentParsingState(val currentTeam: H4aTeamPlace? = null,
                                        val tableState: TableState = TableState.START,
                                        val teamNames: TeamNames  = TeamNames(homeName = null, awayName = null),
                                        val parsedPersons: Map<H4aTeamPlace, MutableSet<ParsedPerson>> = mapOf(
                                                H4aTeamPlace.HOME to mutableSetOf(),
                                                H4aTeamPlace.AWAY to mutableSetOf())) {
    fun addPerson(parsedPerson: ParsedPerson) = when(currentTeam) {
        H4aTeamPlace.HOME -> parsedPersons[H4aTeamPlace.HOME]!!.add(parsedPerson)
        H4aTeamPlace.AWAY -> parsedPersons[H4aTeamPlace.AWAY]!!.add(parsedPerson)
        else -> error("should not occur")
    }

}
private fun String.extractNameFromPDF(after: String) = this.substringAfter(after).substringBefore(") Tj").trim()
