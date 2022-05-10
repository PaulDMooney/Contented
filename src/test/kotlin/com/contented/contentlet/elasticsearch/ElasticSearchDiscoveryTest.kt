package com.contented.contentlet.elasticsearch

import com.contented.MongodemoApplication
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.testcontainers.perSpec
import io.kotest.extensions.testcontainers.perTest
import io.kotest.matchers.shouldBe
import io.kotest.spring.SpringListener
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import java.time.Duration

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@ContextConfiguration(
    initializers = [PropertyOverride::class],
    classes = [MongodemoApplication::class]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
@DirtiesContext
@ActiveProfiles("elasticsearch")
class ElasticSearchDiscoveryTest(@Value("\${elasticsearch.port}") elasticSearchPort:Int): FreeSpec() {

    override fun listeners() = listOf(SpringListener)

    @Autowired
    private lateinit var reactiveElasticSearchClient: ReactiveElasticsearchClient

    init {
        "check existence of index" - {

            var elasticSearch:GenericContainer<*>? = null;
            beforeContainer {
                elasticSearch = elasticSearchContainer(elasticSearchPort)
                elasticSearch?.start()
            }
            afterContainer {
                elasticSearch?.stop()
            }

            "with a new instance of elasticsearch" -{
                val getIndex = GetIndexRequest("myindex")
                val result = reactiveElasticSearchClient.indices()
                    .existsIndex(getIndex)
                    .block(Duration.ofSeconds(60))

                "it should not exist" {
                    result shouldBe false
                }
            }

            "after creating an index" - {
                val myIndexName = "myindex"
                val createIndex = CreateIndexRequest(myIndexName)

                val createIndexResult = reactiveElasticSearchClient.indices()
                    .createIndex(createIndex)
                    .block(Duration.ofSeconds(60))

                "the response should be true" {
                    createIndexResult shouldBe true
                }

                "The index should exist" {
                    val getIndex = GetIndexRequest(myIndexName)
                    val getIndexResult = reactiveElasticSearchClient.indices()
                        .existsIndex(getIndex)
                        .block(Duration.ofSeconds(60))

                    getIndexResult shouldBe true
                }
            }
        }
    }
}