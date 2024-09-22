package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import com.ibm.msg.client.jakarta.jms.JmsMessage
import com.ibm.msg.client.jakarta.wmq.WMQConstants.JMS_IBM_CHARACTER_SET
import jakarta.jms.Message
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.entries
import no.nav.pensjon.infotrygd.tp.mq.adapter.utils.mdc
import no.nav.pensjon.infotrygd.tp.mq.adapter.tp.TjenestepensjonService
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.deserialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.serialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.K278M402
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.annotation.JmsListener
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service
import java.nio.charset.Charset
import java.time.LocalDate

@Service
class InfotrygdService(
    val jmsTemplate: JmsTemplate,
    val service: TjenestepensjonService,
    @Value("\${infotrygd.k278m402.queue}") val queueName: String,
) {
    private val logger = getLogger(javaClass)

    @JmsListener(destination = "\${infotrygd.k278m402.queue}")
    fun hentTjenestepensjonsYtelsesListe(
        bytes: ByteArray,
        message: Message,
    ) = mdc(
        "jms.charset" to (message as? JmsMessage)?.getStringProperty(JMS_IBM_CHARACTER_SET),
        "jms.correlationId" to message.jmsCorrelationID,
        "jms.messageId" to message.jmsMessageID,
        "jms.queueName" to queueName,
        "jms.replyTo" to message.jmsReplyTo.toString(),
    ) {
        val charset = Charset.forName(message.getStringProperty(JMS_IBM_CHARACTER_SET) ?: "ibm277")

        val request = try {
            deserialize(bytes, charset)
        } catch (e: Exception) {
            logger.error("Feil ved deserialisering av melding fra Infotrygd (charset={})", charset, e)
            return@mdc
        }

        logger.info("Request fra Infotrygd {}", structuredArguments(charset, request))

        val response = hentTjenestepensjonsYtelsesListe(request)

        logger.info("Response til Infotrygd {}", structuredArguments(charset, response))

        val messageData = serialize(response, charset)

        try {
            jmsTemplate.send(message.jmsReplyTo) {
                it.createBytesMessage().apply {
                    writeBytes(messageData)
                    setStringProperty(JMS_IBM_CHARACTER_SET, charset.toString())
                }
            }
        } catch (e: Exception) {
            logger.error("Feil ved sending av svar", e)
            return@mdc
        }
    }

    private fun structuredArguments(
        charset: Charset,
        message: InfotrygdMessage
    ): StructuredArgument? = entries(
        mapOf(
            "charset" to charset.name(),

            "alvorlighetsgrad" to message.alvorlighetsgrad,
            "beskMelding" to message.beskMelding,
            "antall" to message.antall,
            "brukerId" to message.brukerId,
            "copyId" to message.copyId,
            "dato" to message.dato,
            "kodeAksjon" to message.kodeAksjon,
            "kilde" to message.kilde,
            "klokke" to message.klokke,
            "lengde" to message.lengde,
        )
    )

    private fun hentTjenestepensjonsYtelsesListe(inputMelding: InfotrygdMessage) =
        try {
            hentMeldinger(inputMelding.outputRecords[0].iFnr!!, inputMelding.outputRecords[0].iFom, inputMelding.outputRecords[0].iTom).let { meldinger ->
                if (meldinger.isEmpty()) {
                    opprettMelding(inputMelding, 4, "INGEN DATA FUNNET", 0, meldinger)
                } else {
                    opprettMelding(inputMelding, null, "INGEN FEIL PÅ RETURNERT MELDING", meldinger.size, meldinger)
                }
            }
        } catch (e: Exception) {
            logger.warn("Uventet feil oppstod", e)
            opprettMelding(inputMelding, 8, "SYSTEMFEIL", 1, emptyList())
        }

    private fun opprettMelding(
        inputMelding: InfotrygdMessage,
        alvorlighetsgrad: Int?,
        beskMelding: String,
        antall: Int,
        meldinger: List<K278M402>
    ) = inputMelding.copy(
        kodeAksjon = "HENT",
        alvorlighetsgrad = alvorlighetsgrad,
        beskMelding = beskMelding,
        copyId = "K278M402",
        antall = antall,
        kilde = "IT00",
        outputRecords = if (alvorlighetsgrad == 8) inputMelding.outputRecords else meldinger
    )

    private fun hentMeldinger(
        ident: String,
        datoFom: LocalDate?,
        datoTom: LocalDate?
    ) = service.hentTjenestepensjon(
        fnr = ident,
    ).flatMap { forhold ->
        forhold.ytelser.filter {
            when {
                datoFom != null && datoTom != null -> it.isIverksattDatesOverlapping(datoFom, datoTom)
                datoFom != null -> it.isIverksattDatesOverlapping(datoFom)
                else -> true
            }
        }.mapNotNull { ytelse ->
            ytelse.ytelseType.asTpArt()?.let { tpArt ->
                K278M402(
                    iFnr = ident,
                    iFom = null,
                    iTom = null,
                    oTPnr = forhold.tpNr.toInt(),
                    oTPart = tpArt,
                    oFom = ytelse.datoYtelseIverksattFom,
                    oTom = ytelse.datoYtelseIverksattTom,
                )
            }
        }
    } .sortedBy { it.oFom }

    private fun String.asTpArt(): Int? = when (this) {
        "ALDER" -> 1
        "UFORE" -> 2
        "GJENLEVENDE" -> 3
        "BARN" -> 5
        "AFP" -> 6
        else -> null
    }
}
