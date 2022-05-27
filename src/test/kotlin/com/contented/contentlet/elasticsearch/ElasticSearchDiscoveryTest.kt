package com.contented.contentlet.elasticsearch

import com.contented.MongodemoApplication
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldHaveKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.spring.SpringListener
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders.matchQuery
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.containers.GenericContainer
import reactor.core.publisher.Mono
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

    @Autowired
    private lateinit var reactiveElasticSearchOperations: ReactiveElasticsearchOperations

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
                    .block(Duration.ofSeconds(20))

                "the response should be true" {
                    createIndexResult shouldBe true
                }

                "The index should exist" {
                    val getIndex = GetIndexRequest(myIndexName)
                    val getIndexResult = reactiveElasticSearchClient.indices()
                        .existsIndex(getIndex)
                        .block(Duration.ofSeconds(20))

                    getIndexResult shouldBe true
                }
            }
        }

        "Index with settings" - {
            var elasticSearch:GenericContainer<*>? = null;
            var stopESContainer = true
            beforeContainer {
                if (stopESContainer) {
                    println("Starting ES Container")
                    elasticSearch = elasticSearchContainer(elasticSearchPort)
                    elasticSearch?.start()
                }
            }
            afterContainer {
                if (stopESContainer) {
                    println("Stopping ES Container")
                    elasticSearch?.stop()
                }
            }

            "create an index with settings" - {
                val myIndexName = "myindex"
                val createIndexResult = `setup index with mapping file`(myIndexName,
                    "/elasticsearch/mappings.json", reactiveElasticSearchClient)
                    .block(Duration.ofSeconds(20))

                "the response should be true" {
                    createIndexResult shouldBe true
                }

                "when saving content" {
                    var myEntity = ContentletDoc("my id with spaces")
                    var myContent = reactiveElasticSearchOperations.save(
                        Mono.just(myEntity),
                        IndexCoordinates.of(myIndexName))
                        .block(Duration.ofSeconds(20))

                    myContent shouldBe myEntity

                }

            }

            "save content with keyword type field" - {
                val myIndexName = "myindex"
                val firstWordsOfId = "my id with"
                val fullId = "${firstWordsOfId} spaces"
                val createIndexResult = `setup index with mapping file`(myIndexName,
                    "/elasticsearch/mappings.json", reactiveElasticSearchClient)
                    .block(Duration.ofSeconds(20))

                var myEntity = ContentletDoc(fullId)
                var returnedEntity = reactiveElasticSearchOperations.save(
                    Mono.just(myEntity),
                    IndexCoordinates.of(myIndexName))
                    .block(Duration.ofSeconds(20))

                "should return the entity saved" {
                    returnedEntity shouldBe myEntity
                }

                "entity should be queryable by exact match of its keyword field" {
                    val query = NativeSearchQueryBuilder()
                        .withQuery(matchQuery("id",fullId))
                        .build();
                    var queryResults = reactiveElasticSearchOperations
                        .search(query, ContentletDoc::class.java, IndexCoordinates.of(myIndexName))
                        .collectList()
                        .block()

                    queryResults?.size shouldBe 1

                    var esEntity = queryResults?.get(0)?.content
                    esEntity?.id shouldBe myEntity.id
                }

                "entity should not be queryable by only partial match of its keyword field" {
                    val query = NativeSearchQueryBuilder()
                        .withQuery(matchQuery("id",firstWordsOfId))
                        .build();
                    var queryResults = reactiveElasticSearchOperations
                        .search(query, ContentletDoc::class.java, IndexCoordinates.of(myIndexName))
                        .collectList()
                        .block()

                    queryResults?.size shouldBe 0
                }
            }

            "save hashmap" - {
                val myIndexName = "myindex"
                val firstWordsOfId = "my id with"
                val fullId = "${firstWordsOfId} spaces 2"
                val createIndexResult = `setup index with mapping file`(myIndexName,
                    "/elasticsearch/mappings.json", reactiveElasticSearchClient)
                    .block(Duration.ofSeconds(20))

                val result = reactiveElasticSearchOperations.save(
                    mutableMapOf("id" to fullId),
                    IndexCoordinates.of(myIndexName))
                    .block(Duration.ofSeconds(20))

                "it should return a result" {
                    result shouldNotBe null
                    result?.shouldHaveKey("id")
                    result?.get("id") shouldBe fullId
                }

                "entity should be queryable by exact match of its keyword field" {
                    val query = NativeSearchQueryBuilder()
                        .withQuery(matchQuery("id",fullId))
                        .build();
                    var queryResults = reactiveElasticSearchOperations
                        .search(query, Map::class.java, IndexCoordinates.of(myIndexName))
                        .collectList()
                        .block()

                    queryResults?.size shouldBe 1

                    var esEntity = queryResults?.get(0)?.content
                    esEntity?.get("id") shouldBe fullId
                }

                "entity should not be queryable by only partial match of its keyword field" {
                    val query = NativeSearchQueryBuilder()
                        .withQuery(matchQuery("id",firstWordsOfId))
                        .build();
                    var queryResults = reactiveElasticSearchOperations
                        .search(query, Map::class.java, IndexCoordinates.of(myIndexName))
                        .collectList()
                        .block()

                    queryResults?.size shouldBe 0
                }
            }
        }
    }
}

fun `setup index with mapping file`(indexName:String,
                                    mappingFile:String,
                                    reactiveElasticSearchClient: ReactiveElasticsearchClient): Mono<Boolean> {
    val mappingJson = ElasticSearchDiscoveryTest::class.java.getResource(mappingFile)
        ?.readText()

    val createIndexRequest = CreateIndexRequest(indexName)
        .mapping(mappingJson, XContentType.JSON)

    val createIndexResult = reactiveElasticSearchClient
        .indices().createIndex(createIndexRequest)

    return createIndexResult
}