package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.deserialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.serialize
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InfotrygdMessageKtTest {
    @Test
    fun `parse request fra Infotrygd`() {
        val message = readResource("/infotrygd/k278m402_04445747268_request.msg")

        with(message) {
            assertNull(kodeAksjon)
            assertEquals("IT00", kilde)
            assertEquals("K278CB1X", brukerId)
            assertEquals(313, lengde)
            assertEquals("20240916", dato)

            assertEquals("131515", klokke)
            assertNull(systemId)
            assertNull(kodeMelding)
            assertEquals(0, alvorlighetsgrad)
            assertNull(beskMelding)
            assertNull(sqlKode)
            assertNull(sqlState)
            assertNull(sqlMelding)
            assertNull(mqCompletionCode)
            assertNull(mqReasonCode)
            assertNull(progId)
            assertNull(sectionNavn)
            assertEquals("K278M402", copyId)

            assertEquals(1, antall)
            assertEquals(1, outputRecords.size)

            with(outputRecords[0]) {
                assertEquals("04445747268", iFnr)
                assertNull(iFom)
                assertNull(iTom)
                assertNull(oTPnr)
                assertNull(oTPart)
                assertNull(oFom)
                assertNull(oTom)
            }
        }
    }

    @Test
    fun `parse response til Infotrygd 1`() {
        val message = readResource("/infotrygd/k278m402_04445747268_3200_3010.msg")

        with(message) {
            assertEquals("HENT", kodeAksjon)
            assertEquals("IT00", kilde)
            assertNull(brukerId)
            assertEquals(0, lengde)
            assertNull(dato)

            assertNull(klokke)
            assertNull(systemId)
            assertNull(kodeMelding)
            assertEquals(0, alvorlighetsgrad)
            assertEquals("INGEN FEIL PÅ RETURNERT MELDING", beskMelding)
            assertNull(sqlKode)
            assertNull(sqlState)
            assertNull(sqlMelding)
            assertNull(mqCompletionCode)
            assertNull(mqReasonCode)
            assertNull(progId)
            assertNull(sectionNavn)
            assertEquals("K278M402", copyId)

            assertEquals(2, antall)
            assertEquals(2, outputRecords.size)

            with(outputRecords[0]) {
                assertEquals("04445747268", iFnr)
                assertNull(iFom)
                assertNull(iTom)
                assertEquals(3200, oTPnr)
                assertEquals(6, oTPart)
                assertEquals(LocalDate.of(2024, Month.FEBRUARY, 1), oFom)
                assertNull(oTom)
            }

            with(outputRecords[1]) {
                assertEquals("04445747268", iFnr)
                assertNull(iFom)
                assertNull(iTom)
                assertEquals(3010, oTPnr)
                assertEquals(1, oTPart)
                assertEquals(LocalDate.of(2025, Month.JANUARY, 1), oFom)
                assertNull(oTom)
            }
        }
    }

    @Test
    fun `parse response med ingen data`() {
        val message = readResource("/infotrygd/k278m402_04445747268_ingen_data.msg")

        with(message) {
            assertEquals("HENT", kodeAksjon)
            assertEquals("IT00", kilde)
            assertNull(brukerId)
            assertEquals(0, lengde)
            assertNull(dato)

            assertNull(klokke)
            assertNull(systemId)
            assertNull(kodeMelding)
            assertEquals(4, alvorlighetsgrad)
            assertEquals("INGEN DATA FUNNET", beskMelding)
            assertNull(sqlKode)
            assertNull(sqlState)
            assertNull(sqlMelding)
            assertNull(mqCompletionCode)
            assertNull(mqReasonCode)
            assertNull(progId)
            assertNull(sectionNavn)
            assertEquals("K278M402", copyId)

            assertEquals(0, antall)
            assertEquals(0, outputRecords.size)
        }
    }

    @Test
    fun `parse response til Infotrygd 2`() {
        val message = readResource("/infotrygd/k278m402_04445747268_3750.msg")

        with(message) {
            assertEquals("HENT", kodeAksjon)
            assertEquals("IT00", kilde)
            assertNull(brukerId)
            assertEquals(0, lengde)
            assertNull(dato)

            assertNull(klokke)
            assertNull(systemId)
            assertNull(kodeMelding)
            assertEquals(0, alvorlighetsgrad)
            assertEquals("INGEN FEIL PÅ RETURNERT MELDING", beskMelding)
            assertNull(sqlKode)
            assertNull(sqlState)
            assertNull(sqlMelding)
            assertNull(mqCompletionCode)
            assertNull(mqReasonCode)
            assertNull(progId)
            assertNull(sectionNavn)
            assertEquals("K278M402", copyId)

            assertEquals(2, antall)
            assertEquals(2, outputRecords.size)

            with(outputRecords[0]) {
                assertEquals("04445747268", iFnr)
                assertNull(iFom)
                assertNull(iTom)
                assertEquals(3750, oTPnr)
                assertEquals(2, oTPart)
                assertEquals(LocalDate.of(2021, Month.OCTOBER, 1), oFom)
                assertEquals(LocalDate.of(2022, Month.FEBRUARY, 1), oTom)
            }

            with(outputRecords[1]) {
                assertEquals("04445747268", iFnr)
                assertNull(iFom)
                assertNull(iTom)
                assertEquals(3750, oTPnr)
                assertEquals(2, oTPart)
                assertEquals(LocalDate.of(2023, Month.NOVEMBER, 1), oFom)
                assertNull(oTom)
            }
        }
    }

    @Test
    fun `test at bytes er lik etter deserialize og serialize`() {
        deserializeAndSerialize("/infotrygd/k278m402_04445747268_ingen_data.msg")
        deserializeAndSerialize("/infotrygd/k278m402_04445747268_3200_3010.msg")
        deserializeAndSerialize("/infotrygd/k278m402_04445747268_3750.msg")
    }

    private fun deserializeAndSerialize(navn: String) {
        val input = readResourceBytes(navn)
        val charset = Charset.forName("ibm277")
        val message = deserialize(input, charset)
        val output = serialize(message, charset)
        assertContentEquals(input, output)
    }

    private fun readResource(navn: String): InfotrygdMessage =
        deserialize(readResourceBytes(navn), Charset.forName("ibm277"))

    private fun readResourceBytes(navn: String) = (javaClass.getResourceAsStream(navn)?.use {
        it.readBytes()
    } ?: throw RuntimeException("Fant ikke ressursen $navn"))

}
