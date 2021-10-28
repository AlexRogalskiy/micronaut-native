package ch.frankel.blog

import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpStatus
import io.micronaut.http.MediaType
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.server.EmbeddedServer
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import jakarta.inject.Inject
import net.minidev.json.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockserver.client.MockServerClient
import org.mockserver.model.HttpRequest
import org.mockserver.model.HttpResponse
import org.testcontainers.containers.MockServerContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

@MicronautTest
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MicronautNativeApplicationTest : TestPropertyProvider {

    @Inject
    @Client
    lateinit var client: HttpClient

    @Inject
    lateinit var server: EmbeddedServer

    companion object {

        @Container
        val mockServer = MockServerContainer(
            DockerImageName.parse("mockserver/mockserver")
        ).apply { start() }
    }

    override fun getProperties() = mapOf(
        "micronaut.application.marvel.apiKey" to "dummy",
        "micronaut.application.marvel.privateKey" to "dummy",
        "micronaut.application.marvel.serverUrl" to "http://${mockServer.containerIpAddress}:${mockServer.serverPort}"
    )

    @Test
    fun `should deserialize JSON payload from server and serialize it back again`() {
        val mockServerClient = MockServerClient(mockServer.containerIpAddress, mockServer.serverPort)
        val sample = this::class.java.classLoader.getResource("sample.json")?.readText()

        mockServerClient.`when`(
            HttpRequest.request()
                .withMethod("GET")
                .withPath("/v1/public/characters")
        ).respond(
            HttpResponse()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody(sample)
        )

        // With `retrieve` you just get the body and can assert on it
        val body = client.toBlocking().retrieve(server.url.toExternalForm(), Model::class.java)
        assertEquals(1, body.data.count)
        assertEquals("Anita Blake", body.data.results.first().name)

        // With `exchange` you get the http response and can assert HTTP Status, Headers,...
        val response = client.toBlocking().exchange(server.url.toExternalForm(), Model::class.java)
        assertEquals(HttpStatus.OK, response.status())
        assertEquals(MediaType.APPLICATION_JSON, response.headers[HttpHeaders.CONTENT_TYPE])
        val bodyFromResponse = response.getBody(Model::class.java)
        assertTrue(bodyFromResponse.isPresent)
        assertEquals(1, bodyFromResponse.get().data.count)
        assertEquals("Anita Blake", bodyFromResponse.get().data.results.first().name)
    }
}
