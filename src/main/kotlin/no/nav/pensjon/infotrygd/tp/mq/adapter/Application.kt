package no.nav.pensjon.infotrygd.tp.mq.adapter

import no.nav.pensjon.infotrygd.tp.mq.adapter.azuread.AzureAdClientCredentialsService
import no.nav.pensjon.infotrygd.tp.mq.adapter.utils.LoggingClientHttpRequestInterceptor
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestClient

@SpringBootApplication
class Application {
    @Bean
    fun azureRestClient(): RestClient = RestClient.builder()
        .requestInterceptor(LoggingClientHttpRequestInterceptor())
        .build()

    @Bean
    fun tpRestClient(
        @Value("\${tp.base.url}") baseUrl: String,
        @Value("\${tp.scope}") scope: Set<String>,
        azureAdClientCredentialsService: AzureAdClientCredentialsService,
    ): RestClient = RestClient.builder()
        .baseUrl(baseUrl)
        .requestInterceptor(LoggingClientHttpRequestInterceptor())
        .requestInterceptor { request, body, execution ->
            request.headers.setBearerAuth(azureAdClientCredentialsService.accessToken(scope))
            execution.execute(request, body)
        }
        .build()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
