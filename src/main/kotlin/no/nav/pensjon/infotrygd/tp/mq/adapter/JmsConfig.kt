package no.nav.pensjon.infotrygd.tp.mq.adapter

import jakarta.jms.ConnectionFactory
import org.slf4j.LoggerFactory.getLogger
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jms.config.DefaultJmsListenerContainerFactory
import org.springframework.util.backoff.FixedBackOff

@Configuration
class JmsConfig {
    private val logger = getLogger(javaClass)

    @Bean
    fun jmsListenerContainerFactory(
        configurer: DefaultJmsListenerContainerFactoryConfigurer,
        connectionFactory: ConnectionFactory,
    ): DefaultJmsListenerContainerFactory = DefaultJmsListenerContainerFactory().apply {
        configurer.configure(this, connectionFactory)
        setConcurrency("1-5")
        setErrorHandler { e -> logger.error("Feil ved behandling av JMS-melding", e) }
        setBackOff(FixedBackOff(1000L, 3))
    }
}
