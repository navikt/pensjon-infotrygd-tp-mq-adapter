package no.nav.pensjon.infotrygd.tp.mq.adapter.utils

import net.logstash.logback.marker.Markers.appendEntries
import org.slf4j.LoggerFactory
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.io.IOException
import java.lang.System.nanoTime
import java.util.concurrent.TimeUnit.NANOSECONDS

internal class LoggingClientHttpRequestInterceptor : ClientHttpRequestInterceptor {
    override fun intercept(request: HttpRequest, body: ByteArray, execution: ClientHttpRequestExecution): ClientHttpResponse {
        val startTime = nanoTime()

        var response: ClientHttpResponse? = null
        var exception: Exception? = null

        return try {
            response = execution.execute(request, body)
            response
        } catch (e: Exception) {
            exception = e
            throw e
        } finally {
            val timeUsage = NANOSECONDS.toMillis(nanoTime() - startTime)

            val stackStraceMap: Map<String, String?> = exception?.let {
                mapOf(
                    "stack_trace" to exception.stackTraceToString()
                )
            } ?:emptyMap()

            logger.info(
                appendEntries(
                    mapOf<String, Any?>(
                        "x_upstream_host" to request.uri.host,
                        "method" to request.method,
                        "request_uri" to request.uri,
                        "response_code" to response?.statusCode,
                        "response_time" to timeUsage,
                    ) + stackStraceMap
                ),
                "{} {} {} {} ms",
                request.method,
                request.uri,
                getStatusMessage(response),
                timeUsage
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LoggingClientHttpRequestInterceptor::class.java)

        private fun getStatusMessage(response: ClientHttpResponse?): String = try {
            response?.statusCode?.toString() ?: "CLIENT_ERROR"
        } catch (ex: IOException) {
            "IO_ERROR"
        }
    }
}
