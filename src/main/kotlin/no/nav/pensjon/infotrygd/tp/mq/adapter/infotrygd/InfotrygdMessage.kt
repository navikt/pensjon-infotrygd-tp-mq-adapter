package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import java.nio.ByteBuffer.wrap
import java.nio.charset.Charset
import java.time.LocalDate
import java.time.format.DateTimeFormatter.*
import java.util.concurrent.atomic.AtomicInteger

data class InfotrygdMessage(
    // K278MHEA
    val kodeAksjon: String?,        // :TAG:-KODE-AKSJON         PIC X(08).
    val kilde: String?,             // :TAG:-KILDE               PIC X(04).
    val brukerId: String?,          // :TAG:-BRUKERID            PIC X(08).
    val lengde: Int?,               // :TAG:-LENGDE              PIC 9(07).
    val dato: String?,              // :TAG:-DATO                PIC X(08).
    val klokke: String?,            // :TAG:-TID                 PIC X(06).

    // K469MMEL
    val systemId: String?,          // :TAG:-SYSTEM-ID           PIC X(08).
    val kodeMelding: String?,       // :TAG:-KODE-MELDING        PIC X(08).
    val alvorlighetsgrad: Int?,     // :TAG:-ALVORLIGHETSGRAD    PIC 9(02).
    val beskMelding: String?,       // :TAG:-BESKR-MELDING       PIC X(75).
    val sqlKode: String?,           // :TAG:-SQL-KODE            PIC X(04).
    val sqlState: String?,          // :TAG:-SQL-STATE           PIC X(05).
    val sqlMelding: String?,        // :TAG:-SQL-MELDING         PIC X(80).
    val mqCompletionCode: String?,  // :TAG:-MQ-COMPLETION-CODE  PIC X(04).
    val mqReasonCode: String?,      // :TAG:-MQ-REASON-CODE      PIC X(04).
    val progId: String?,            // :TAG:-PROGRAM-ID          PIC X(08).
    val sectionNavn: String?,       // :TAG:-SECTION-NAVN        PIC X(30).

    // K278M8ID
    val copyId: String?,            // :TAG:-COPY-ID             PIC X(08).
    val antall: Int,                // :TAG:-ANTALL              PIC 9(05).

    // K278M402
    val outputRecords: List<K278M402>
) {
    data class K278M402(
        val iFnr: String?,          // :TAG:-I-fnr               Pic 9(11).
        val iFom: LocalDate?,       // :TAG:-I-fom               Pic S9(6) Comp-3.
        val iTom: LocalDate?,       // :TAG:-I-tom               Pic S9(6) Comp-3.
        val oTPnr: Int?,            // :TAG:-O-TPnr              Pic S9(4) Comp-3.
        val oTPart: Int?,           // :TAG:-O-TPart             Pic 9.
        val oFom: LocalDate?,       // :TAG:-O-fom               Pic S9(6) Comp-3.
        val oTom: LocalDate?,       // :TAG:-O-tom               Pic S9(6) Comp-3.
    )

    companion object {
        fun deserialize(bytes: ByteArray, charset: Charset): InfotrygdMessage {
            val string = String(bytes, charset)
            val cursor = AtomicInteger()
            val antallRecords = AtomicInteger()

            fun readDecimal(length: Int): Int =
                readDecimal(string, cursor.getAndAdd(length), length)
            fun readString(length: Int): String? =
                readString(string, cursor.getAndAdd(length), length)
            fun readPackedDecimal(length: Int): Int? =
                readPackedDecimal(
                    bytes,
                    cursor.getAndAdd(packedDecimalBytes(length)),
                    length
                )

            return InfotrygdMessage(
                // K278MHEA
                kodeAksjon = readString(8),
                kilde = readString(4),
                brukerId = readString(8),
                lengde = readDecimal(7),
                dato = readString(8),
                klokke = readString(6),

                // K469MMEL
                systemId = readString(8),
                kodeMelding = readString(8),
                alvorlighetsgrad = readDecimal(2),
                beskMelding = readString(75),
                sqlKode = readString(4),
                sqlState = readString(5),
                sqlMelding = readString(80),
                mqCompletionCode = readString(4),
                mqReasonCode = readString(4),
                progId = readString(8),
                sectionNavn = readString(30),

                // K278M8ID
                copyId = readString(8),
                antall = readDecimal(5).also { antallRecords.set(it) },

                // K278M402
                outputRecords = (0..<antallRecords.get()).map { _ ->
                    K278M402(
                        iFnr = readString(11),
                        iFom = readPackedDecimal(6)?.parseK278M402Date(),
                        iTom = readPackedDecimal(6)?.parseK278M402Date(),
                        oTPnr = readPackedDecimal(4).takeUnless { it == 0 },
                        oTPart = readDecimal(1).takeUnless { it == 0 },
                        oFom = readPackedDecimal(6)?.parseK278M402Date(),
                        oTom = readPackedDecimal(6)?.parseK278M402Date(),
                    )
                }
            )
        }

        fun serialize(message: InfotrygdMessage, charset: Charset): ByteArray {
            fun Int?.writeDecimal(length: Int) = writeDecimal(charset, this, length)
            fun String?.writeString(length: Int) = writeString(charset, this, length)

            // K278MHEA
            return message.kodeAksjon.writeString(8) +
                    message.kilde.writeString(4) +
                    message.brukerId.writeString(8) +
                    message.lengde.writeDecimal(7) +
                    message.dato.writeString(8) +
                    message.klokke.writeString(6) +

                    // K469MMEL
                    message.systemId.writeString(8) +
                    message.kodeMelding.writeString(8) +
                    message.alvorlighetsgrad.writeDecimal(2) +
                    message.beskMelding.writeString(75) +
                    message.sqlKode.writeString(4) +
                    message.sqlState.writeString(5) +
                    message.sqlMelding.writeString(80) +
                    message.mqCompletionCode.writeString(4) +
                    message.mqReasonCode.writeString(4) +
                    message.progId.writeString(8) +
                    message.sectionNavn.writeString(30) +

                    // K278M8ID
                    writeString(charset, message.copyId, 8) +
                    writeDecimal(charset, message.antall, 5) +
                    message.outputRecords.map { serialize(it, charset) }
                        .let { outputRecordByteArrays ->
                            wrap(ByteArray(outputRecordByteArrays.sumOf { it.size }))
                                .also { byteBuffer ->
                                    outputRecordByteArrays.forEach { byteBuffer.put(it) }
                                }.array()
                        }
            }

            fun serialize(record: K278M402, charset: Charset): ByteArray {
                fun Int?.writeDecimal(length: Int) = writeDecimal(charset, this, length)
                fun String?.writeString(length: Int) = writeString(charset, this, length)
                fun Int?.writePackedDecimal(length: Int) =
                    writePackedDecimal(this, length)

                return record.iFnr.writeString(11) +
                        record.iFom?.asK278M402Date().writePackedDecimal(6) +
                        record.iTom?.asK278M402Date().writePackedDecimal(6) +
                        record.oTPnr.writePackedDecimal(4) +
                        record.oTPart.writeDecimal(1) +
                        record.oFom?.asK278M402Date().writePackedDecimal(6) +
                        record.oTom?.asK278M402Date().writePackedDecimal(6)
            }

        private fun Int.parseK278M402Date(): LocalDate? = takeUnless { it == 0 }?.toString()?.let { LocalDate.parse(it.padStart(6, '0') + "01", BASIC_ISO_DATE) }

        private fun LocalDate.asK278M402Date(): Int = format(ofPattern("yyyyMM")).toInt()
    }
}

