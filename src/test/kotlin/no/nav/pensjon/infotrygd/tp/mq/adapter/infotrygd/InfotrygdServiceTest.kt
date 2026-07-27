package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import jakarta.jms.BytesMessage
import jakarta.jms.Destination
import jakarta.jms.Message
import jakarta.jms.Session
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.deserialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.serialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.tp.TjenestepensjonService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.jms.core.JmsTemplate
import org.springframework.jms.core.MessageCreator
import java.nio.charset.Charset
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockitoExtension::class)
class InfotrygdServiceTest {

    @Mock private lateinit var jmsTemplate: JmsTemplate
    @Mock private lateinit var tjenestepensjonService: TjenestepensjonService
    @Mock private lateinit var message: Message
    @Mock private lateinit var replyTo: Destination

    private lateinit var service: InfotrygdService
    private val charset = Charset.forName("ibm277")
    private val capturedResponseBytes = mutableListOf<ByteArray>()

    @BeforeEach
    fun setUp() {
        service = InfotrygdService(jmsTemplate, tjenestepensjonService, "TEST.QUEUE")
        capturedResponseBytes.clear()
    }

    @Test
    fun `ingen ytelser fra TP gir alvorlighetsgrad 4`() {
        stubMessage()
        stubJmsTemplateSend()
        `when`(tjenestepensjonService.hentTjenestepensjon("12345678901")).thenReturn(emptyList())

        service.hentTjenestepensjonsYtelsesListe(lagRequestBytes("12345678901"), message)

        val svar = lesRespons()
        assertEquals(4, svar.alvorlighetsgrad)
        assertEquals("INGEN DATA FUNNET", svar.beskMelding)
        assertEquals(0, svar.antall)
    }

    @Test
    fun `exception fra TP gir alvorlighetsgrad 8`() {
        stubMessage()
        stubJmsTemplateSend()
        `when`(tjenestepensjonService.hentTjenestepensjon("12345678901"))
            .thenThrow(RuntimeException("TP er nede"))

        service.hentTjenestepensjonsYtelsesListe(lagRequestBytes("12345678901"), message)

        val svar = lesRespons()
        assertEquals(8, svar.alvorlighetsgrad)
        assertEquals("SYSTEMFEIL", svar.beskMelding)
    }

    // 🔴 Rød sone — skriv disse selv:
    // - asTpArt-mapping: ALDER→1, UFORE→2, GJENLEVENDE→3, BARN→5, AFP→6, ukjent→null
    // - datofiltrering: kun iFom, begge satt, ingen datoer
    // - output-records er sortert stigende på oFom
    // Bruk [lagRequestBytes] og [lesRespons] som utgangspunkt.

    private fun stubMessage() {
        `when`(message.getStringProperty(any())).thenReturn(charset.name())
        `when`(message.jmsReplyTo).thenReturn(replyTo)
        `when`(message.jmsMessageID).thenReturn("ID:test-message-id")
        `when`(message.jmsCorrelationID).thenReturn("ID:test-correlation-id")
    }

    private fun stubJmsTemplateSend() {
        doAnswer { invocation ->
            val session = mock(Session::class.java)
            val bytesMessage = mock(BytesMessage::class.java)
            `when`(session.createBytesMessage()).thenReturn(bytesMessage)
            doAnswer { args -> capturedResponseBytes.add(args.getArgument(0)); null }
                .`when`(bytesMessage).writeBytes(any())
            invocation.getArgument<MessageCreator>(1).createMessage(session)
            null
        }.`when`(jmsTemplate).send(any(Destination::class.java), any(MessageCreator::class.java))
    }

    private fun lagRequestBytes(fnr: String, iFom: LocalDate? = null, iTom: LocalDate? = null): ByteArray =
        serialize(
            InfotrygdMessage(
                kodeAksjon = null, kilde = "IT00", brukerId = "K278CB1X", lengde = 313,
                dato = "20240916", klokke = "131515", systemId = null, kodeMelding = null,
                alvorlighetsgrad = 0, beskMelding = null, sqlKode = null, sqlState = null,
                sqlMelding = null, mqCompletionCode = null, mqReasonCode = null,
                progId = null, sectionNavn = null, copyId = "K278M402", antall = 1,
                outputRecords = listOf(InfotrygdMessage.K278M402(fnr, iFom, iTom, null, null, null, null)),
            ),
            charset,
        )

    private fun lesRespons(): InfotrygdMessage {
        assertTrue(capturedResponseBytes.isNotEmpty(), "Ingen respons sendt til Infotrygd")
        return deserialize(capturedResponseBytes.last(), charset)
    }
}
