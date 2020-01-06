package de.muellerml.handball4all.report

import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.AnnotationSpec
import org.junit.jupiter.api.assertThrows
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URL
import java.time.Duration
import java.time.LocalTime


internal class H4aReportParserImplTest : AnnotationSpec() {

    private val parser = H4aReportParserBuilder().build()

    @Test
    fun parseReportThrowsException() {
        assertThrows<IOException> {
            parser.parseReport(ByteArrayInputStream("Hello".toByteArray()))
        }
    }

    @Test
    fun readRemoteReportSuccessfully() {
        val url = URL("http://spo.handball4all.de/misc/sboPublicReports.php?sGID=880729")
        val result = parser.parseReport(url.openStream())
        result.homeTeam shouldBe "CVJM Möglingen"
        result.awayTeam shouldBe "HSG Sulzbach-Murrhardt"
        result.personsForTeam(result.homeTeam).count() shouldBe 15
        result.personsForTeam(result.awayTeam).count() shouldBe 17
        result.parsedEvents shouldHaveSize 71

        result.parsedEvents[2].run {
            score shouldBe (1 to 0)
            this.actor shouldBe ParsedPerson(number = "13", firstname = "Fabian", lastname = "Weller")
            this.action shouldBe EventType.YELLOW
            this.gameTime shouldBe Duration.ofMinutes(3).plusSeconds(5)
        }
        result.parsedEvents.first().run { this as ParsedPersonEvent }.run {
            score shouldBe (1 to 0)
            this.actor shouldBe null
        }
        val personToTest = ParsedPerson(number = "4", firstname = "Michael", lastname = "Müller")
        result.eventsFor(personToTest) shouldHaveSize 3
        result.eventsFor(personToTest, EventType.GOAL) shouldBe listOf(
                ParsedPersonEvent(action = EventType.GOAL,
                        actor = personToTest, actionDateTime = LocalTime.of(20, 6, 54),
                        gameTime = Duration.ofMinutes(5).plusSeconds(52), score = 1 to 2),
                ParsedPersonEvent(action = EventType.GOAL,
                actor = personToTest, actionDateTime = LocalTime.of(20, 55, 44),
                gameTime = Duration.ofMinutes(36).plusSeconds(5), score = 14 to 15))
        result.eventsFor(personToTest, EventType.TWO_MIN) shouldBe listOf(
                ParsedPersonEvent(actor = personToTest, action = EventType.TWO_MIN, actionDateTime = LocalTime.of(21, 11, 57),
                        gameTime = Duration.ofMinutes(48).plusSeconds(34), score = Pair(17, 19))
        )
    }
}
