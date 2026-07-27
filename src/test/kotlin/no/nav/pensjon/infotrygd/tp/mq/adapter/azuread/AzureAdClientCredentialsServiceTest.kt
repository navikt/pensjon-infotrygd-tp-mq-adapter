package no.nav.pensjon.infotrygd.tp.mq.adapter.azuread

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val TOKEN_ENDPOINT = "https://login.microsoftonline.com/tenant-id/oauth2/v2.0/token"
private const val CLIENT_ID = "test-client-id"
private const val CLIENT_SECRET = "test-client-secret"
private const val SCOPE = "api://test/.default"
private val SCOPES = setOf(SCOPE)

private val TOKEN_RESPONSE = """
    {
        "token_type": "Bearer",
        "expires_in": 3600,
        "access_token": "eyJtest.token.value"
    }
""".trimIndent()

class AzureAdClientCredentialsServiceTest {

    private lateinit var server: MockRestServiceServer
    private lateinit var service: AzureAdClientCredentialsService

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder()
        server = MockRestServiceServer.bindTo(builder).build()
        service = AzureAdClientCredentialsService(
            azureRestClient = builder.build(),
            clientId = CLIENT_ID,
            clientSecret = CLIENT_SECRET,
            tokenEndpoint = TOKEN_ENDPOINT,
        )
    }

    @Test
    fun `henter access token`() {
        server.expect(requestTo(TOKEN_ENDPOINT))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON))

        val token = service.accessToken(SCOPES)

        assertEquals("eyJtest.token.value", token)
        server.verify()
    }

    @Test
    fun `token caches og hentes ikke på nytt ved andre kall`() {
        server.expect(requestTo(TOKEN_ENDPOINT))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON))

        val token1 = service.accessToken(SCOPES)
        val token2 = service.accessToken(SCOPES)

        assertEquals(token1, token2)
        server.verify() // verifiserer at det kun ble gjort ett kall
    }

    @Test
    fun `TokenResponse isValid er true for nylig hentet token`() {
        val response = ClientCredentialsTokenResponse(
            tokenType = "Bearer",
            expiresIn = 3600,
            accessToken = "eyJtest"
        )
        assertTrue(response.isValid)
    }

    @Test
    fun `kaster ClientCredentialsException ved 404`() {
        server.expect(requestTo(TOKEN_ENDPOINT))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertFailsWith<ClientCredentialsException> {
            service.accessToken(SCOPES)
        }
    }

    @Test
    fun `kaster ClientCredentialsException ved 500`() {
        server.expect(requestTo(TOKEN_ENDPOINT))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertFailsWith<ClientCredentialsException> {
            service.accessToken(SCOPES)
        }
    }

    @Test
    fun `fetch returnerer token med korrekte felter`() {
        server.expect(requestTo(TOKEN_ENDPOINT))
            .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON))

        val response = service.fetch(SCOPES)

        assertNotNull(response)
        assertEquals("Bearer", response.tokenType)
        assertEquals(3600L, response.expiresIn)
        assertEquals("eyJtest.token.value", response.accessToken)
        assertTrue(response.isValid)
    }
}
