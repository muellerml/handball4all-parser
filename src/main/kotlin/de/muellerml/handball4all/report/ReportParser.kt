package de.muellerml.handball4all.report

import org.apache.pdfbox.pdmodel.PDDocument
import java.io.InputStream


class H4aReportParserBuilder {
    private val parser = H4aReportParserImpl(statisticsParser = H4aStatisticsParserImpl(),
            actionParser = H4aEventParserImpl())
    fun build() : H4aReportParser = parser
}

interface H4aReportParser {
    fun parseReport(inputStream: InputStream): ParsedReport
}

internal class H4aReportParserImpl(private val statisticsParser: H4aStatisticsParser,
                               private val actionParser: H4aEventParser) : H4aReportParser {

    override fun parseReport(inputStream: InputStream): ParsedReport {
        return PDDocument.load(inputStream).use {document ->
            val statistics = statisticsParser.parseStatisticsPage(document.getPage(1))
            document.removePage(1)
            document.removePage(0)
            val events : List<ParsedEvent<*>> = document.pages.flatMap { actionParser.parseEvents(it) }
            val groupedEvents = events.groupBy { it.actor }
            ParsedReport(homePersons = statistics.homePersons.map { it to groupedEvents[it].orEmpty() }.toMap(),
                    awayPersons = statistics.awayPersons.map { it to groupedEvents[it].orEmpty() }.toMap(),
                    homeTeam = statistics.homeName,
                    awayTeam = statistics.awayName,
                    parsedEvents = events)
        }
    }
}




class H4aReportFetcher {
    fun fetchReport(gameId: String) : Any {
        TODO("Not implemented")
    }
}

data class StatisticsParseResult(val homePersons: Set<ParsedPerson>,
                                 val awayPersons: Set<ParsedPerson>,
                                 var homeName: String,
                                 var awayName: String)
