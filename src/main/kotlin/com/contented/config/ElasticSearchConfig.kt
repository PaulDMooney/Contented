package com.contented.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.data.elasticsearch.client.ClientConfiguration
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients
import org.springframework.data.elasticsearch.config.AbstractReactiveElasticsearchConfiguration
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.data.elasticsearch.core.convert.MappingElasticsearchConverter
import org.springframework.web.reactive.function.client.ExchangeStrategies

/**
 * Deriving configuration from this article: https://paolodedominicis.medium.com/reactive-spring-data-elasticsearch-with-spring-boot-dbcfdc9edb3d
 */
class ElasticSearchConfig: AbstractReactiveElasticsearchConfiguration() {

    @Value("\${spring.data.elasticsearch.client.reactive.endpoints}")
    private val elasticSearchEndpoints: String? = null

    @Bean
    override fun reactiveElasticsearchClient(): ReactiveElasticsearchClient {
        val clientConfiguration: ClientConfiguration = ClientConfiguration.builder()
            .connectedTo(elasticSearchEndpoints)
            .withWebClientConfigurer { webClient ->
                val exchangeStrategies: ExchangeStrategies = ExchangeStrategies.builder()
                    .codecs {configurer -> configurer.defaultCodecs().maxInMemorySize(-1)}.build()
                webClient.mutate().exchangeStrategies(exchangeStrategies).build()
            }
            .build()

        return ReactiveRestClients.create(clientConfiguration)
    }

}