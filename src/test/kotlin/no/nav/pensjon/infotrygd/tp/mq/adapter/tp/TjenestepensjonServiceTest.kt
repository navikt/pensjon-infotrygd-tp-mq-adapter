package no.nav.pensjon.infotrygd.tp.mq.adapter.tp

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.retry.annotation.EnableRetry
import org.springframework.test.web.client.ExpectedCount.times
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
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
    private lateinit var restClient: RestClient
    private lateinit var service: TjenestepensjonService

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        restClient = builder.build()
        service = TjenestepensjonService(restClient)
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

    @Test
    fun `hentTjenestepensjon ignores unknown fields and reads nullable dates`() {
        val json = """
            {
                "unknown": "ignored",
                "forhold": [
                    {"ordning": "3010", "ytelser": [], "unknown": true},
                    {
                        "ordning": "3200",
                        "ytelser": [{
                            "type": "ALDER",
                            "datoYtelseIverksattFom": null,
                            "datoYtelseIverksattTom": "2025-12-31",
                            "unknown": 42
                        }]
                    }
                ]
            }
        """.trimIndent()

        server.expect(requestTo("/api/tjenestepensjon/"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON))

        val result = service.hentTjenestepensjon("12345678901")

        assertEquals(emptyList(), result[0].ytelser)
        assertNull(result[1].ytelser[0].datoYtelseIverksattFom)
        assertEquals(LocalDate.of(2025, 12, 31), result[1].ytelser[0].datoYtelseIverksattTom)
        server.verify()
    }

    @Test
    fun `hentTjenestepensjon retries failed requests up to three attempts`() {
        server.expect(times(2), requestTo("/api/tjenestepensjon/"))
            .andRespond(withServerError())
        server.expect(requestTo("/api/tjenestepensjon/"))
            .andRespond(withSuccess("""{"forhold":[]}""", MediaType.APPLICATION_JSON))

        AnnotationConfigApplicationContext().use { context ->
            context.register(RetryConfiguration::class.java)
            context.beanFactory.registerSingleton("tpRestClient", restClient)
            context.refresh()

            val result = context.getBean(TjenestepensjonService::class.java)
                .hentTjenestepensjon("12345678901")

            assertEquals(emptyList(), result)
            server.verify()
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableRetry
    class RetryConfiguration {
        @Bean
        fun tjenestepensjonService(tpRestClient: RestClient) = TjenestepensjonService(tpRestClient)
    }
}
