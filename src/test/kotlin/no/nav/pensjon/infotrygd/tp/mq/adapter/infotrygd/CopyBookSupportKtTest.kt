package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import kotlin.test.Test
import kotlin.test.assertContentEquals

class CopyBookSupportKtTest {
    @Test
    fun packeddecimal() {
        assertContentEquals(byteArrayOfInts(0x00, 0x00, 0x0C), writePackedDecimal(0, 4))
        assertContentEquals(byteArrayOfInts(0x01, 0x23, 0x4C), writePackedDecimal(1234, 4))
        assertContentEquals(byteArrayOfInts(0x03, 0x01, 0x0C), writePackedDecimal(3010, 4))
        assertContentEquals(byteArrayOfInts(0x03, 0x20, 0x0C), writePackedDecimal(3200, 4))
    }

    @Test
    fun `skriving og lesing av packed decimal gir samme verdi`() {
        for (i in -9..<10) {
            val bytes = writePackedDecimal(i, 1)
            kotlin.test.assertEquals(1, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, true))
        }
        for (i in -99..<100) {
            val bytes = writePackedDecimal(i, 2)
            kotlin.test.assertEquals(2, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, false))
        }
        for (i in -999..<1000 step 10) {
            val bytes = writePackedDecimal(i, 3)
            kotlin.test.assertEquals(2, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, true))
        }
        for (i in -9999..<10000 step 10) {
            val bytes = writePackedDecimal(i, 4)
            kotlin.test.assertEquals(3, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, false))
        }
        for (i in -99999..<100000 step 100) {
            val bytes = writePackedDecimal(i, 5)
            kotlin.test.assertEquals(3, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, true))
        }
        for (i in -999999..<1000000 step 100) {
            val bytes = writePackedDecimal(i, 6)
            kotlin.test.assertEquals(4, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, false))
        }
        for (i in -9999999..<10000000 step 1000) {
            val bytes = writePackedDecimal(i, 7)
            kotlin.test.assertEquals(4, bytes.size)
            kotlin.test.assertEquals(i, readPackedDecimal(bytes, 0, bytes.size, true))
        }
    }

    private fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
}
