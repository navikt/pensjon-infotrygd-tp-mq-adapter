package no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd

import no.nav.pensjon.infotrygd.tp.mq.adapter.infotrygd.InfotrygdMessage.Companion.deserialize
import java.nio.charset.Charset

fun readResource(navn: String): InfotrygdMessage =
    deserialize(readResourceBytes(navn), Charset.forName("ibm277"))

fun readResourceBytes(navn: String) = (InfotrygdMessageKtTest::class.java.getResourceAsStream(navn)?.use {
    it.readBytes()
} ?: throw RuntimeException("Fant ikke ressursen $navn"))
