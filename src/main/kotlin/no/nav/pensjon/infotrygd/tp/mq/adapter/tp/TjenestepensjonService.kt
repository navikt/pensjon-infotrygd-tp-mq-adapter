package no.nav.pensjon.infotrygd.tp.mq.adapter.tp

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import no.nav.pensjon.infotrygd.tp.mq.adapter.utils.isOverlapping
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.time.LocalDate

@Service
class TjenestepensjonService(
    private val tpRestClient: RestClient,
) {
    fun hentTjenestepensjon(fnr: String): List<ForholdModel> {
        val tjenestepensjon: TjenestepensjonModel = tpRestClient.get()
            .uri("/api/tjenestepensjon/")
            .accept(APPLICATION_JSON)
            .header("fnr", fnr)
            .retrieve()
            .body()
            ?: throw RuntimeException("Fikk tomt svar fra tp-registeret")

        return tjenestepensjon.forhold
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class TjenestepensjonModel(
        val forhold: List<ForholdModel> = emptyList()
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ForholdModel(
        val ordning: String,
        val ytelser: List<YtelseModel> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class YtelseModel(
        val type: String,
        val datoYtelseIverksattFom: LocalDate?,
        val datoYtelseIverksattTom: LocalDate?
    ) {
        fun isIverksattDatesOverlapping(from: LocalDate) =
            datoYtelseIverksattTom == null || !datoYtelseIverksattTom.isBefore(from)

        fun isIverksattDatesOverlapping(from: LocalDate?, to: LocalDate?) =
            isOverlapping(from, to, datoYtelseIverksattFom, datoYtelseIverksattTom)
    }
}
