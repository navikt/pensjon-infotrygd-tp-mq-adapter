package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import java.lang.Character.getNumericValue
import java.nio.charset.Charset
import kotlin.math.abs


fun readDecimal(str: String, start: Int, length: Int): Int = str.substring(start, start + length).toInt()

fun writeDecimal(charset: Charset, value: Number?, length: Int): ByteArray =
    (value ?: 0).toString().padStart(length, '0').toByteArray(charset)

fun readString(str: String, start: Int, length: Int): String? = str.substring(start, start + length).trim().takeUnless { it.isEmpty() }

fun writeString(charset: Charset, value: String?, length: Int): ByteArray = (value ?: "").padEnd(length).take(length).toByteArray(charset)

fun packedDecimalBytes(length: Int): Int = ((length + 1) / 2) + if (length % 2 == 0) 1 else 0

fun readPackedDecimal(bytes: ByteArray, start: Int, length: Int): Int? {
    val end = start + packedDecimalBytes(length)
    return if (bytes.size >= end) {
        readPackedDecimal(bytes, start, end, length % 2 != 0)
    } else {
        null
    }
}

fun readPackedDecimal(bytes: ByteArray, start: Int, end: Int, oddNumberOfDigits: Boolean): Int {
    val digits = StringBuilder()

    for (i in start until end - 1) {
        if (oddNumberOfDigits || i > start) {
            digits.append((bytes[i].toInt() shr 4) and 0xF)
        }

        digits.append(bytes[i].toInt() and 0xF)
    }

    digits.append((bytes[end - 1].toInt() shr 4) and 0xF)

    return when (bytes[end - 1].toInt() and 0x0F) {
        0x0B, 0x0D -> digits.toString().toInt().unaryMinus()
        else -> digits.toString().toInt()
    }
}

fun writePackedDecimal(value: Int?, length: Int): ByteArray = writePackedDecimal(value?.toLong() ?: 0, length)

private fun writePackedDecimal(value: Long, length: Int): ByteArray {
    val absValue = abs(value).toString().padStart(length, '0')
    val byteLength: Int = packedDecimalBytes(length)

    val totalBytes = (absValue.length + 1 + 1) / 2

    if (totalBytes > byteLength) {
        throw IllegalArgumentException("Verdien $value får ikke plass i $byteLength byte trenger $totalBytes byte")
    }

    val bytes = ByteArray(byteLength)

    var nibbleIndex: Int = if (length % 2 == 0) 1 else 0

    absValue.map { getNumericValue(it) }.forEach { digit ->
        val bytePos = nibbleIndex / 2

        if (nibbleIndex % 2 == 0) {
            bytes[bytePos] = (digit shl 4).toByte()
        } else {
            bytes[bytePos] = (bytes[bytePos].toInt() or digit).toByte()
        }

        nibbleIndex++
    }

    val signNibble = if (value < 0) 0xD else 0xC
    val lastBytePos = nibbleIndex / 2

    if (nibbleIndex % 2 == 0) {
        bytes[lastBytePos] = signNibble.toByte()
    } else {
        bytes[lastBytePos] = (bytes[lastBytePos].toInt() or signNibble).toByte()
    }

    return bytes
}
