package com.contented.contentlet.elasticsearch

import com.contented.contentlet.CONTENTLETS_PATH
import com.contented.contentlet.ContentletDTO
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.spring.SpringExtension
import org.assertj.core.api.Assertions
import org.elasticsearch.http.HttpStats
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.core.publisher.Mono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
@DirtiesContext
class ElasticSearchControllerTest(@LocalServerPort port: Int): StringSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {

        lateinit var contentletControllerClient: WebClient;

        lateinit var webTestClient: WebTestClient;

        beforeContainer {
            contentletControllerClient = WebClient.create("http://localhost:$port/$CONTENTLETS_PATH")

            webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:9200/search").build()
        }

        "When a contentlet has been saved".config(enabled = false) {

            // Given
            val toSave = ContentletDTO("ABCDE")

            contentletControllerClient.put().bodyValue(toSave)
                .retrieve().onStatus({ status -> !status.is2xxSuccessful })
                {_ -> Mono.error(RuntimeException("Error calling /$CONTENTLETS_PATH"))}.toBodilessEntity().block()

            "and search endpoint is called" {

                // When
                val response = webTestClient.get().exchange()

                "it should respond with a result" {

                    // Then
                    response.expectStatus()
                        .is2xxSuccessful
                        .expectBodyList(String::class.java)
                        .hasSize(1)
                }
            }

        }
    }
}