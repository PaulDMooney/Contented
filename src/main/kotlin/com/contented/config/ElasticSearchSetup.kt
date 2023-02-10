package com.contented.config

import com.contented.utils.companionLogger
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Conditional
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import javax.annotation.PostConstruct


@Component
@ConditionalOnProperty(
    "elasticsearch.setup",
    havingValue = "true",
    matchIfMissing = false)
class ElasticSearchSetup(@Autowired val elasticsearchClient: ReactiveElasticsearchClient,
                         @Value("\${elasticsearch.indexname}") val indexName:String) {


    companion object {
        val logger: Logger = companionLogger()
    }

    @PostConstruct
    fun createIndexIfNonePostConstruct():Boolean? {
        return createIndexIfNone(elasticsearchClient, indexName).block();
    }

    fun createIndexIfNone(elasticsearchClient: ReactiveElasticsearchClient,
                          indexName:String): Mono<Boolean> {

        val getIndex = GetIndexRequest(indexName);

        val result = elasticsearchClient.indices()
            .existsIndex(getIndex)
            .doOnError { error ->
                logger.error("Error checking existence of index $indexName", error)
                throw error
            }
            .flatMap {indexExists ->
                logger.info("Index $indexName exists: $indexExists")
                if (indexExists) {
                    return@flatMap Mono.just(false)
                }
                return@flatMap createIndex(indexName)
            }

        return result;
    }

    fun createIndex(indexName:String):Mono<Boolean> {
        val createIndex = CreateIndexRequest(indexName)

        val createIndexResult = elasticsearchClient.indices()
            .createIndex(createIndex)
            .doOnSuccess { logger.info("Created Index $indexName") }
            .doOnError { error ->
                logger.error("Error creating index $indexName", error)
                throw error
            }
            .map { true }
        return createIndexResult
    }
}


