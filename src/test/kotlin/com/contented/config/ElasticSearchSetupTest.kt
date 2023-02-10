package com.contented.config

import com.contented.MongodemoApplication
import com.contented.contentlet.elasticsearch.PropertyOverride
import com.contented.contentlet.elasticsearch.elasticSearchContainer
import io.kotest.core.spec.AfterContainer
import io.kotest.core.spec.BeforeContainer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.spring.SpringListener
import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
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
class ElasticSearchSetupTest(@Value("\${elasticsearch.port}") elasticSearchPort:Int) : BehaviorSpec() {

    override fun listeners() = listOf(SpringListener)

    @Autowired
    private lateinit var elasticSearchClient: ReactiveElasticsearchClient

    lateinit var elasticSearch: GenericContainer<*>;

    val testSetup: BeforeContainer = {
        elasticSearch = elasticSearchContainer(elasticSearchPort)
        elasticSearch?.start()
    }

    val testTearDown: AfterContainer = {
        elasticSearch?.stop()
    }

    init {

        given("ElasticSearchSetup.createIndexIfNonePostConstruct") {

            beforeContainer(testSetup)

            afterContainer(testTearDown)

            val indexName = "mytestindex"

            When("invoked with no existing index") {

                val elasticSearchSetup = ElasticSearchSetup(elasticSearchClient, indexName)

                val result = elasticSearchSetup.createIndexIfNonePostConstruct()

                then("it should return true") {
                    result shouldBe true
                }

                then("it should have created the index") {
                    val getIndex = GetIndexRequest(indexName)
                    val indexExists = elasticSearchClient.indices()
                        .existsIndex(getIndex)
                        .block(Duration.ofSeconds(10))

                    indexExists shouldBe true
                }

                then("invoking again with an existing index should return false") {
                    val secondAttemptResult = elasticSearchSetup.createIndexIfNonePostConstruct()
                    secondAttemptResult shouldBe false
                }
            }
        }
    }

}
