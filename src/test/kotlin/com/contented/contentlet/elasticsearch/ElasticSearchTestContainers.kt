package com.contented.contentlet.elasticsearch

import com.contented.MongodemoApplication
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.core.spec.style.StringSpec
import io.kotest.extensions.testcontainers.perTest
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.Network
import org.testcontainers.containers.wait.strategy.Wait

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@ContextConfiguration(
    initializers = [PropertyOverride::class],
    classes = [MongodemoApplication::class]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
@DirtiesContext
@ActiveProfiles("elasticsearch")
class ElasticSearchTestContainers(@Value("\${elasticsearch.port}") elasticSearchPort:Int): FreeSpec() {

    init {

        val elasticSearch = elasticSearchContainer(elasticSearchPort)
        listener(elasticSearch.perTest())

        lateinit var webTestClient: WebTestClient;

        beforeContainer {
            webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://${elasticSearch.host}:$elasticSearchPort")
                .build()
        }

        "Given there is an elasticsearch test container" - {

            "when we attempt a rest call to it" - {
                var response = webTestClient.get()
                    .exchange()

                "we should get a response from the container" {
                    response.expectStatus().is2xxSuccessful
                }
            }

        }
    }
}

fun elasticSearchContainer(elasticSearchPort:Int): GenericContainer<*> {
    val elasticSearch = GenericContainer("elasticsearch:7.17.3")
        .withExposedPorts(9200)
        .withNetwork(Network.SHARED)
        .withEnv("discovery.type", "single-node")
        .waitingFor (
            Wait
                .forHttp("/_cat/health?v&pretty")
                .forStatusCode(200)
        );
    elasticSearch.portBindings = listOf("$elasticSearchPort:9200")
    return elasticSearch
}