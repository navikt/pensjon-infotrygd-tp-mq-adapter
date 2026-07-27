package no.nav.pensjon.infotrygd.tp.mq.adapter.utils

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalDateUtilTest {

    @Test
    fun `begge perioder er åpne - overlapper alltid`() {
        assertTrue(isOverlapping(null, null, null, null))
    }

    @Test
    fun `perioder overlapper i midten`() {
        assertTrue(isOverlapping(
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 30),
            LocalDate.of(2020, 4, 1), LocalDate.of(2020, 12, 31),
        ))
    }

    @Test
    fun `perioder overlapper ikke - A slutter før B starter`() {
        assertFalse(isOverlapping(
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 3, 31),
            LocalDate.of(2020, 4, 1), LocalDate.of(2020, 12, 31),
        ))
    }

    @Test
    fun `perioder overlapper ikke - B slutter før A starter`() {
        assertFalse(isOverlapping(
            LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31),
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31),
        ))
    }

    @Test
    fun `perioder er identiske`() {
        val date = LocalDate.of(2020, 6, 1)
        assertTrue(isOverlapping(date, date, date, date))
    }

    @Test
    fun `A slutter samme dag som B starter - overlapper`() {
        assertTrue(isOverlapping(
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 6, 1),
            LocalDate.of(2020, 6, 1), LocalDate.of(2020, 12, 31),
        ))
    }

    @Test
    fun `åpen slutt på A - overlapper alltid med fremtidig B`() {
        assertTrue(isOverlapping(
            LocalDate.of(2020, 1, 1), null,
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31),
        ))
    }

    @Test
    fun `åpen start på B - overlapper alltid`() {
        assertTrue(isOverlapping(
            LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31),
            null, LocalDate.of(2025, 12, 31),
        ))
    }

    @Test
    fun `åpen slutt på B - overlapper hvis A starter etter B sin start`() {
        assertTrue(isOverlapping(
            LocalDate.of(2022, 1, 1), LocalDate.of(2022, 12, 31),
            LocalDate.of(2019, 1, 1), null,
        ))
    }

    @Test
    fun `åpen start på A og åpen slutt på B - overlapper alltid`() {
        assertTrue(isOverlapping(
            null, LocalDate.of(2025, 1, 1),
            LocalDate.of(2020, 1, 1), null,
        ))
    }
}
