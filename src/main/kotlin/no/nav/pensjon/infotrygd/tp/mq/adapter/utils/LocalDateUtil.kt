package no.nav.pensjon.infotrygd.tp.mq.adapter.utils

import java.time.LocalDate


/**
 * Compares two date periods to see if they are overlapping.
 *
 * See the answer from the following stack overflow question for explanation.
 * https://stackoverflow.com/questions/325933/determine-whether-two-date-ranges-overlap/325964.
 */
fun isOverlapping(startA: LocalDate?, endA: LocalDate?, startB: LocalDate?, endB: LocalDate?) =
    (endB == null || startA == null || startA <= endB)
            && (endA == null || startB == null || endA >= startB)
