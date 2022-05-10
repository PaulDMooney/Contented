package com.contented.contentlet.elasticsearch

import com.contented.MongodemoApplication
import io.kotest.core.spec.style.FreeSpec
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

        val elasticSearch = elasticSearchContainer(elasticSearchPort)
        listener(elasticSearch.perTest())


        "check existence of indices" {
            val getIndex = GetIndexRequest("myindex")
            val result = reactiveElasticSearchClient.indices()
                .existsIndex(getIndex)
                .block(Duration.ofSeconds(60))

            result shouldBe false
        }

        "create index" {
            val myIndexName = "myindex"
            val createIndex = CreateIndexRequest(myIndexName)

            val createIndexResult = reactiveElasticSearchClient.indices()
                .createIndex(createIndex)
                .block(Duration.ofSeconds(60))

            createIndexResult shouldBe true

            val getIndex = GetIndexRequest(myIndexName)
            val getIndexResult = reactiveElasticSearchClient.indices()
                .existsIndex(getIndex)
                .block(Duration.ofSeconds(60))

            getIndexResult shouldBe true

        }
    }
}