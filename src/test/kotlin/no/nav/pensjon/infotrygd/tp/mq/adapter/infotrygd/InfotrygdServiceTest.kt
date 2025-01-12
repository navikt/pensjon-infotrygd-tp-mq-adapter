package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InfotrygdServiceTest {
    @Test
    fun `to identiske meldinger anses som aa vaere like`() {
        val message = readResourceBytes("/infotrygd/k278m402_04445747268_3200_3010.msg")

        assertTrue { InfotrygdService.erMeldingerLike(message, message) }
    }

    @Test
    fun `to forskjellige meldinger anses som aa vaere forskjellige`() {
        val message1 = readResourceBytes("/infotrygd/k278m402_04445747268_3200_3010.msg")
        val message2 = readResourceBytes("/infotrygd/k278m402_04445747268_ingen_data.msg")

        assertFalse { InfotrygdService.erMeldingerLike(message1, message2) }
    }
}