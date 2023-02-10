package com.contented.contentlet.elasticsearch

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.client.reactive.DefaultReactiveElasticsearchClient
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.stereotype.Component

@Component
class SaveHook(@Autowired val elasticsearchClient: ReactiveElasticsearchClient) {
}