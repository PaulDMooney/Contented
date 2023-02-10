package com.contented

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories

@SpringBootApplication
@EnableWebFlux
@OpenAPIDefinition(info = Info(title="APIs"))
class MongodemoApplication

fun main(args: Array<String>) {
	runApplication<MongodemoApplication>(*args)
}