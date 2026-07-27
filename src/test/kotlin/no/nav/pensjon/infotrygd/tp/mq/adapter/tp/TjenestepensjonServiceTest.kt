package no.nav.pensjon.infotrygd.tp.mq.adapter.tp

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TjenestepensjonServiceTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var service: TjenestepensjonService

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        service = TjenestepensjonService(builder.build())
    }

    // --- YtelseModel datofiltrering ---

    @Test
    fun `ytelse uten TOM er alltid gyldig etter FOM`() {
        val ytelse = TjenestepensjonService.YtelseModel("ALDER", LocalDate.of(2020, 1, 1), null)
        assertTrue(ytelse.isIverksattDatesOverlapping(LocalDate.of(2099, 1, 1)))
    }

    @Test
    fun `ytelse med TOM etter from er gyldig`() {
        val ytelse = TjenestepensjonService.YtelseModel("ALDER", LocalDate.of(2020, 1, 1), LocalDate.of(2025, 1, 1))
        assertTrue(ytelse.isIverksattDatesOverlapping(LocalDate.of(2024, 1, 1)))
    }

    @Test
    fun `ytelse med TOM lik from er gyldig`() {
        val dato = LocalDate.of(2024, 1, 1)
        val ytelse = TjenestepensjonService.YtelseModel("ALDER", LocalDate.of(2020, 1, 1), dato)
        assertTrue(ytelse.isIverksattDatesOverlapping(dato))
    }

    @Test
    fun `ytelse med TOM før from er ikke gyldig`() {
        val ytelse = TjenestepensjonService.YtelseModel("ALDER", LocalDate.of(2020, 1, 1), LocalDate.of(2022, 1, 1))
        assertFalse(ytelse.isIverksattDatesOverlapping(LocalDate.of(2023, 1, 1)))
    }

    @Test
    fun `ytelse overlapper med periode fom-tom`() {
        val ytelse = TjenestepensjonService.YtelseModel("ALDER", LocalDate.of(2020, 1, 1), LocalDate.of(2023, 12, 31))
        assertTrue(ytelse.isIverksattDatesOverlapping(LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1)))
    }

    @Test
    fun `ytelse overlapper ikke med periode fom-tom`() {
        val ytelse = TjenestepensjonService.YtelseModel("ALDER", LocalDate.of(2020, 1, 1), LocalDate.of(2021, 12, 31))
        assertFalse(ytelse.isIverksattDatesOverlapping(LocalDate.of(2022, 1, 1), LocalDate.of(2025, 1, 1)))
    }

    // --- REST-kall ---

    @Test
    fun `hentTjenestepensjon returnerer tom liste når ingen forhold`() {
        server.expect(requestTo("/api/tjenestepensjon/"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header("fnr", "12345678901"))
            .andRespond(withSuccess("""{"forhold":[]}""", MediaType.APPLICATION_JSON))

        val result = service.hentTjenestepensjon("12345678901")

        assertEquals(emptyList(), result)
        server.verify()
    }

    @Test
    fun `hentTjenestepensjon mapper ytelser korrekt`() {
        val json = """
            {
                "forhold": [{
                    "ordning": "3010",
                    "ytelser": [{
                        "type": "ALDER",
                        "datoYtelseIverksattFom": "2025-01-01",
                        "datoYtelseIverksattTom": null
                    }]
                }]
            }
        """.trimIndent()

        server.expect(requestTo("/api/tjenestepensjon/"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val result = service.hentTjenestepensjon("12345678901")

        assertEquals(1, result.size)
        assertEquals("3010", result[0].ordning)
        assertEquals(1, result[0].ytelser.size)
        assertEquals("ALDER", result[0].ytelser[0].type)
        assertEquals(LocalDate.of(2025, 1, 1), result[0].ytelser[0].datoYtelseIverksattFom)
        assertNull(result[0].ytelser[0].datoYtelseIverksattTom)
        server.verify()
    }

    @Test
    fun `hentTjenestepensjon kaster exception ved tomt svar`() {
        server.expect(requestTo("/api/tjenestepensjon/"))
            .andRespond(withSuccess("null", MediaType.APPLICATION_JSON))

        assertFailsWith<RuntimeException> {
            service.hentTjenestepensjon("12345678901")
        }
    }
}
