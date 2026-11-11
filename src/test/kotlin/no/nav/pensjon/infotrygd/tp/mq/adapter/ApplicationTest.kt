package no.nav.pensjon.infotrygd.tp.mq.adapter

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jms.config.JmsListenerEndpointRegistry
import org.springframework.web.client.RestClient
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "logging.config=classpath:logback-nais.xml",
        "spring.jms.listener.auto-startup=false",
        "management.endpoint.health.probes.enabled=true",
        "ibm.mq.user=",
        "ibm.mq.password=",
        "ibm.mq.queueManager=TEST",
        "ibm.mq.channel=TEST",
        "ibm.mq.connName=localhost(1414)",
        "infotrygd.k278m402.queue=TEST.QUEUE",
        "tp.base.url=http://localhost",
        "tp.scope=api://test/.default",
        "AZURE_APP_CLIENT_ID=test-client",
        "AZURE_APP_CLIENT_SECRET=test-secret",
        "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT=http://localhost/token",
    ],
)
class ApplicationTest @Autowired constructor(
    @Value("\${local.server.port}") private val port: Int,
    private val listenerRegistry: JmsListenerEndpointRegistry,
) {
    @Test
    fun `application starts with JMS listener and healthy probes without connecting to MQ`() {
        assertEquals(1, listenerRegistry.listenerContainers.size)
        listenerRegistry.listenerContainers.forEach { assertFalse(it.isRunning) }

        val client = RestClient.create("http://localhost:$port")
        for (probe in listOf("liveness", "readiness")) {
            val response = client.get()
                .uri("/actuator/health/$probe")
                .retrieve()
                .toEntity(String::class.java)

            assertEquals(200, response.statusCode.value())
            assertEquals("""{"status":"UP"}""", response.body)
        }
    }
}
