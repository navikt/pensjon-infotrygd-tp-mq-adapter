package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import com.ibm.msg.client.jakarta.wmq.WMQConstants.*
import jakarta.jms.BytesMessage
import jakarta.jms.Message
import jakarta.jms.Session
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.entries
import net.logstash.logback.marker.Markers.appendEntries
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.deserialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.serialize
import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.K278M402
import no.nav.pensjon.infotrygd.tp.mq.adapter.tp.TjenestepensjonService
import no.nav.pensjon.infotrygd.tp.mq.adapter.utils.mdc
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.jms.annotation.JmsListener
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service
import java.lang.System.nanoTime
import java.nio.charset.Charset
import java.time.LocalDate
import java.util.concurrent.TimeUnit.NANOSECONDS

@Service
class InfotrygdService(
    val jmsTemplate: JmsTemplate,
    val service: TjenestepensjonService,
    @Value("\${infotrygd.k278m402.queue}") val queueName: String,
    @Value("\${infotrygd.k278m402.busQueue}") val busQueueName: String,
) {
    private val logger = getLogger(javaClass)

    @JmsListener(destination = "\${infotrygd.k278m402.queue}")
    fun hentTjenestepensjonsYtelsesListe(
        bytes: ByteArray,
        message: Message,
    ) = mdc(
        "jms.charset" to message.getStringProperty(JMS_IBM_CHARACTER_SET),
        "jms.correlationId" to message.jmsCorrelationID,
        "jms.messageId" to message.jmsMessageID,
        "jms.queueName" to queueName,
        "jms.replyTo" to message.jmsReplyTo.toString(),
    ) {
        val start = nanoTime()
        logger.info("Mottok melding fra Infotrygd")

        val charset = Charset.forName(message.getStringProperty(JMS_IBM_CHARACTER_SET) ?: "ibm277")
        val responseCorrelationId = message.jmsMessageID

        val messageData = handleMessage(
            charset,
            bytes,
            message.jmsCorrelationID,
            message.jmsMessageID,
            message.getStringProperty(JMS_IBM_CHARACTER_SET)
        )

        if (messageData != null) {
            try {
                jmsTemplate.send(message.jmsReplyTo) {
                    it.createBytesMessage().apply {
                        jmsCorrelationID = responseCorrelationId
                        writeBytes(messageData)
                        setStringProperty(JMS_IBM_CHARACTER_SET, charset.toString())
                        setIntProperty(JMS_IBM_ENCODING, 785)
                    }
                }
            } catch (e: Exception) {
                logger.error("Feil ved sending av svar", e)
                return@mdc
            }
        }

        val executionTimeMillis = NANOSECONDS.toMillis(nanoTime() - start)

        logger.info(
            appendEntries(
                mapOf(
                    "response_time" to executionTimeMillis,
                    "elapsed_time" to executionTimeMillis,
                )
            ),
            "Behandling av melding fra Infotrygd tok {} ms", executionTimeMillis
        )
    }

    fun handleMessage(
        charset: Charset,
        bytes: ByteArray,
        jmsCorrelationID: String,
        messageID: String,
        jmsIbmCharacterSet: String?,
    ): ByteArray? {
        val svarFraBuss = jmsTemplate.sendAndReceive(busQueueName) { session: Session ->
            session.createBytesMessage().also { bytesMessage ->
                bytesMessage.jmsCorrelationID = jmsCorrelationID
                bytesMessage.jmsMessageID = messageID
                bytesMessage.writeBytes(bytes)
                jmsIbmCharacterSet?.let { bytesMessage.setStringProperty(JMS_IBM_CHARACTER_SET, it) }
            }
        }

        if (svarFraBuss == null) {
            logger.error("Ingen svar fra buss")
            return null
        }

        val bytesBus = (svarFraBuss as BytesMessage).asByteArray()

        try {
            val request = try {
                deserialize(bytes, charset)
            } catch (e: Exception) {
                logger.error("Feil ved deserialisering av melding fra Infotrygd (charset={})", charset, e)
                return null
            }

            logger.info("Request fra Infotrygd {}", structuredArguments(charset, request))

            val response = hentTjenestepensjonsYtelsesListe(request)

            logger.info("Response til Infotrygd {}", structuredArguments(charset, response))

            val messageData = serialize(response, charset)

            if (!erMeldingerLike(bytesBus, messageData, charset)) {
                logger.info("Innholdet er forskjellig, bus=${bytesBus.size}, svar=${messageData.size}")

                logger.info("Request hex {}", bytes.toHex())
                logger.info("Bytes bus hex {}", bytesBus.toHex())
                logger.info("Bytes svar hex {}", messageData.toHex())
            } else {
                logger.info("Innholdet er likt med bus")
            }
        } catch (e: Exception) {
            logger.error("Feil ved sammenligning med svar fra buss", e)
        }

        return bytesBus
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
                    opprettMelding(4, "INGEN DATA FUNNET", meldinger)
                } else {
                    opprettMelding(null, "INGEN FEIL PÅ RETURNERT MELDING", meldinger)
                }
            }
        } catch (e: Exception) {
            logger.warn("Uventet feil oppstod", e)
            opprettFeilmeding(inputMelding)
        }

    private fun opprettMelding(
        alvorlighetsgrad: Int?,
        beskMelding: String,
        meldinger: List<K278M402>
    ) = InfotrygdMessage(
        kodeAksjon = "HENT",
        kilde = "IT00",
        brukerId = null,
        lengde = null,
        dato = null,
        klokke = null,

        systemId = null,
        kodeMelding = null,
        alvorlighetsgrad = alvorlighetsgrad,
        beskMelding = beskMelding,
        sqlKode = null,
        sqlState = null,
        sqlMelding = null,
        mqCompletionCode = null,
        mqReasonCode = null,
        progId = null,
        sectionNavn = null,

        copyId = "K278M402",
        antall = meldinger.size,

        outputRecords = meldinger,
    )

    private fun opprettFeilmeding(
        inputMelding: InfotrygdMessage,
    ) = inputMelding.copy(
        kodeAksjon = "HENT",
        alvorlighetsgrad = 8,
        beskMelding = "SYSTEMFEIL",
        copyId = "K278M402",
        antall = 1,
        kilde = "IT00",
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
        }.map { ytelse ->
            K278M402(
                iFnr = ident,
                iFom = null,
                iTom = null,
                oTPnr = forhold.ordning.toInt(),
                oTPart = ytelse.type.asTpArt(),
                oFom = ytelse.datoYtelseIverksattFom,
                oTom = ytelse.datoYtelseIverksattTom,
            )
        }
    }.sortedBy { it.oFom }

    private fun String.asTpArt(): Int? = when (this) {
        "ALDER" -> 1
        "UFORE" -> 2
        "GJENLEVENDE" -> 3
        "BARN" -> 5
        "AFP" -> 6
        else -> null
    }

    private fun BytesMessage.asByteArray() =
        ByteArray(bodyLength.toInt()).also {
            readBytes(it)
        }

    fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

    companion object {
        fun erMeldingerLike(bytesBus: ByteArray, messageData: ByteArray, charset: Charset): Boolean {
            return if (bytesBus.size == messageData.size) {
                val bussMessage = deserialize(bytesBus, charset)
                val message = deserialize(messageData, charset)

                return bussMessage.kodeAksjon == message.kodeAksjon
                        && bussMessage.kilde == message.kilde
                        && bussMessage.brukerId == message.brukerId
                        && bussMessage.lengde == message.lengde
                        && bussMessage.dato == message.dato
                        && bussMessage.klokke == message.klokke

                        && bussMessage.systemId == message.systemId
                        && bussMessage.kodeMelding == message.kodeMelding
                        && bussMessage.alvorlighetsgrad == message.alvorlighetsgrad
                        && bussMessage.beskMelding == message.beskMelding
                        && bussMessage.sqlKode == message.sqlKode
                        && bussMessage.sqlState == message.sqlState
                        && bussMessage.sqlMelding == message.sqlMelding
                        && bussMessage.mqCompletionCode == message.mqCompletionCode
                        && bussMessage.mqReasonCode == message.mqReasonCode
                        && bussMessage.progId == message.progId
                        && bussMessage.sectionNavn == message.sectionNavn

                        && bussMessage.copyId == message.copyId
                        && bussMessage.antall == message.antall
                        && bussMessage.outputRecords.size == message.outputRecords.size
                        && erOutputRecordsSortertEtterOFom(bussMessage.outputRecords)
                        && erOutputRecordsSortertEtterOFom(message.outputRecords)
                        && erOutputRecordsLike(bussMessage.outputRecords, message.outputRecords)
            } else {
                false
            }
        }

        private fun erOutputRecordsSortertEtterOFom(outputRecords: List<K278M402>): Boolean {
            val oFoms = outputRecords.map { it.oFom }
            return oFoms == oFoms.sortedBy { it }
        }

        private fun erOutputRecordsLike(
            outputRecords: List<K278M402>,
            outputRecords1: List<K278M402>
        ): Boolean {
            val comparator = compareBy<K278M402>(
                { it.iFnr },
                { it.iFom },
                { it.iTom },
                { it.oTPnr },
                { it.oTPart },
                { it.oFom },
                { it.oTom },
            )

            return outputRecords.sortedWith(comparator) == outputRecords1.sortedWith(comparator)
        }

    }
}
