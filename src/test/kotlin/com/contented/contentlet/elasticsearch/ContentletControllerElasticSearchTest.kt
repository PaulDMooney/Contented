package com.contented.contentlet.elasticsearch

import com.contented.MongodemoApplication
import com.contented.contentlet.CONTENTLETS_PATH
import com.contented.contentlet.ContentletDTO
import io.kotest.core.spec.AfterContainer
import io.kotest.core.spec.BeforeContainer
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.core.spec.style.FreeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.shouldBe
import io.kotest.spring.SpringListener
import org.elasticsearch.index.query.QueryBuilders
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.containers.GenericContainer

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@ContextConfiguration(
    initializers = [PropertyOverride::class],
    classes = [MongodemoApplication::class]
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
@DirtiesContext
@ActiveProfiles("elasticsearch")
class ContentletControllerElasticSearchTest(
    @Value("\${elasticsearch.port}") elasticSearchPort:Int,
    @LocalServerPort port:Int): BehaviorSpec() {

    override fun listeners() = listOf(SpringListener)

    @Autowired
    private lateinit var reactiveElasticSearchClient: ReactiveElasticsearchClient

    @Autowired
    private lateinit var reactiveElasticSearchOperations: ReactiveElasticsearchOperations

//    override fun extensions() = listOf(SpringExtension)

    init {

        fun getRootUrl(): String = "http://localhost:$port/$CONTENTLETS_PATH"

        lateinit var webTestClient: WebTestClient;

        lateinit var elasticSearch: GenericContainer<*>;

        beforeSpec {
            webTestClient = WebTestClient.bindToServer().baseUrl(getRootUrl()).build();
        }

        val testSetup: BeforeContainer = {
            elasticSearch = elasticSearchContainer(elasticSearchPort)
            elasticSearch?.start()
        }

        val testTearDown: AfterContainer = {
            elasticSearch?.stop()
        }

        given("New content is saved to the PUT endpoint") {

            beforeContainer(testSetup)

            afterContainer(testTearDown)

            // Given
            val toSave = ContentletDTO("ABCDE", hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string"))

            // When
            val response = webTestClient.put().bodyValue(toSave).exchange();

            `when`("querying the content by id in elasticsearch") {

                val query = NativeSearchQueryBuilder()
                    .withQuery(QueryBuilders.matchQuery("id", toSave.id))
                    .build();
                var queryResults = reactiveElasticSearchOperations
                    .search(query, Map::class.java, IndexCoordinates.of("My Index"))
                    .collectList()
                    .block()

                then("1 result should be found") {
                    queryResults?.size shouldBe 1
                }

                then ("the result should have the same id as the saved content") {
                    var esEntity = queryResults?.get(0)?.content
                    esEntity?.get("id") shouldBe toSave.id
                }
            }
        }
    }
}