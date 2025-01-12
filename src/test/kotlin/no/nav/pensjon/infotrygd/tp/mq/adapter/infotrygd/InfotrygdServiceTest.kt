package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.serialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.K278M402
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdService.Companion.erMeldingerLike
import java.nio.charset.Charset
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfotrygdServiceTest {
    private val charset: Charset = Charset.forName("ibm277")

    @Test
    fun `avvik i hodeverdier gjoer at meldinger anses som ulike`() {
        assertFalse { erMeldingerLike(lagInfotrygdmelding(kodeAksjon = "a"), lagInfotrygdmelding(kodeAksjon = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(kodeAksjon = "a"), lagInfotrygdmelding(kodeAksjon = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(kilde = "a"), lagInfotrygdmelding(kilde = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(brukerId = "a"), lagInfotrygdmelding(brukerId = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(lengde = 1), lagInfotrygdmelding(lengde = 2), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(dato = "a"), lagInfotrygdmelding(dato = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(klokke = "a"), lagInfotrygdmelding(klokke = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(systemId = "a"), lagInfotrygdmelding(systemId = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(kodeMelding = "a"), lagInfotrygdmelding(kodeMelding = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(alvorlighetsgrad = 1), lagInfotrygdmelding(alvorlighetsgrad = 2), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(beskMelding = "a"), lagInfotrygdmelding(beskMelding = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(sqlKode = "a"), lagInfotrygdmelding(sqlKode = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(sqlState = "a"), lagInfotrygdmelding(sqlState = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(sqlMelding = "a"), lagInfotrygdmelding(sqlMelding = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(mqCompletionCode = "a"), lagInfotrygdmelding(mqCompletionCode = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(mqReasonCode = "a"), lagInfotrygdmelding(mqReasonCode = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(progId = "a"), lagInfotrygdmelding(progId = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(sectionNavn = "a"), lagInfotrygdmelding(sectionNavn = "b"), charset ) }
        assertFalse { erMeldingerLike(lagInfotrygdmelding(copyId = "a"), lagInfotrygdmelding(copyId = "b"), charset ) }
    }

    @Test
    fun `to identiske meldinger anses som aa vaere like`() {
        assertTrue { erMeldingerLike(lagInfotrygdmelding(), lagInfotrygdmelding(), charset ) }
    }

    @Test
    fun `to identiske meldinger anses som aa vaere like2`() {
        val message = readResourceBytes("/infotrygd/k278m402_04445747268_3200_3010.msg")

        assertTrue { erMeldingerLike(message, message, charset) }
    }

    @Test
    fun `to forskjellige meldinger anses som aa vaere forskjellige`() {
        val message1 = readResourceBytes("/infotrygd/k278m402_04445747268_3200_3010.msg")
        val message2 = readResourceBytes("/infotrygd/k278m402_04445747268_ingen_data.msg")

        assertFalse { erMeldingerLike(message1, message2, charset) }
    }

    @Test
    fun `ulik sortering paa data gir ulik`() {
        val fom1 = k278M402(oFom = LocalDate.of(2025, 1, 1))
        val fom2 = k278M402(oFom = LocalDate.of(2025, 2, 1))

        val melding1 = lagInfotrygdmelding(outputRecords = listOf(fom1, fom2))
        val melding2  = lagInfotrygdmelding(outputRecords = listOf(fom2, fom1))

        assertFalse { erMeldingerLike(melding1, melding2, charset) }
    }

    private fun k278M402(
        iFnr: String? = "12345678901",
        iFom: LocalDate? = null,
        iTom: LocalDate? = null,
        oTPnr: Int? = 1234,
        oTPart: Int? = 1,
        oFom: LocalDate? = LocalDate.of(2025, 1, 1),
        oTom: LocalDate? = LocalDate.of(2025, 2, 1),
    ) = K278M402(
        iFnr = iFnr,
        iFom = iFom,
        iTom = iTom,
        oTPnr = oTPnr,
        oTPart = oTPart,
        oFom = oFom,
        oTom = oTom,
    )

    private fun lagInfotrygdmelding(
        kodeAksjon: String? = "A123",
        kilde: String? = "KILD",
        brukerId: String? = "BRUKER01",
        lengde: Int? = 1234567,
        dato: String? = "20250112",
        klokke: String? = "120000",

        systemId: String? = "SYSID123",
        kodeMelding: String? = "MELD123",
        alvorlighetsgrad: Int? = 2,
        beskMelding: String? = "Testmelding",
        sqlKode: String? = "SQL1",
        sqlState: String? = "S1234",
        sqlMelding: String? = "SQL feilmelding",
        mqCompletionCode: String? = "MQCC",
        mqReasonCode: String? = "MQRC",
        progId: String? = "PROG001",
        sectionNavn: String? = "SEC001",

        copyId: String? = "COPY001",

        outputRecords: List<K278M402> = listOf(k278M402(
            iFnr = "12345678901",
            iFom = LocalDate.of(2025, 1, 1),
            iTom = LocalDate.of(2025, 2, 1),
            oTPnr = 1234,
            oTPart = 1,
            oFom = LocalDate.of(2025, 1, 1),
            oTom = LocalDate.of(2025, 2, 1)
        ), k278M402(
            iFnr = "22345678901",
            iFom = null,
            iTom = null,
            oTPnr = 3010,
            oTPart = 1,
            oFom = LocalDate.of(2025, 1, 1),
            oTom = null
        ))
    ) = InfotrygdMessage(
        kodeAksjon = kodeAksjon,
        kilde = kilde,
        brukerId = brukerId,
        lengde = lengde,
        dato = dato,
        klokke = klokke,
        systemId = systemId,
        kodeMelding = kodeMelding,
        alvorlighetsgrad = alvorlighetsgrad,
        beskMelding = beskMelding,
        sqlKode = sqlKode,
        sqlState = sqlState,
        sqlMelding = sqlMelding,
        mqCompletionCode = mqCompletionCode,
        mqReasonCode = mqReasonCode,
        progId = progId,
        sectionNavn = sectionNavn,
        copyId = copyId,
        antall = outputRecords.size,
        outputRecords = outputRecords
    ).let { serialize(it, charset) }
}
