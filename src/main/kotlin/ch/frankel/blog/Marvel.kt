package ch.frankel.blog

import io.micronaut.context.annotation.Factory
import io.micronaut.http.HttpRequest
import io.micronaut.http.annotation.*
import io.micronaut.http.client.HttpClient
import io.micronaut.http.uri.UriBuilder
import jakarta.inject.Singleton
import org.reactivestreams.Publisher
import java.math.BigInteger
import java.security.MessageDigest

@Factory
class BeanFactory {

    @Singleton
    fun messageDigest(): MessageDigest = MessageDigest.getInstance("MD5")
}

@Controller
class MarvelController(
    private val client: HttpClient,
    private val properties: MarvelProperties,
    private val digest: MessageDigest
) {
    @Get
    fun characters(
        @QueryValue limit: String?,
        @QueryValue offset: String?,
        @QueryValue orderBy: String?
    ): Publisher<Model> {
        val uri = UriBuilder
            .of("${properties.serverUrl}/v1/public/characters")
            .queryParamsWith(properties, digest)
            .queryParamsWith(
                mapOf("limit" to limit, "offset" to offset, "orderBy" to orderBy)
            )
            .build()
        val request = HttpRequest.GET<Unit>(uri)
        return client.retrieve(request, Model::class.java)
    }
}

private fun UriBuilder.queryParamsWith(params: Map<String, String?>) = apply {
    params.entries
        .filter { it.value != null }
        .forEach { queryParam(it.key, it.value) }
}

private fun UriBuilder.queryParamsWith(props: MarvelProperties, digest: MessageDigest) = apply {
    val ts = System.currentTimeMillis().toString()
    queryParam("ts", ts)
    queryParam("apikey", props.apiKey)
    val md5 = "$ts${props.privateKey}${props.apiKey}".toMd5(digest)
    queryParam("hash", md5)
}

private fun String.toMd5(digest: MessageDigest) = BigInteger(1, digest.digest(toByteArray()))
    .toString(16)
    .padStart(32, '0')
