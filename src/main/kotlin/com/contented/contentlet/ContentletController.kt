package com.contented.contentlet

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cloud.sleuth.CurrentTraceContext
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

const val CONTENTLETS_PATH = "contentlets"

@RestController
@RequestMapping("/$CONTENTLETS_PATH")
class ContentletController(private val contentletRepository: ContentletRepository,
                           private val contentletService: ContentletService,
                           private val tracer: Tracer,
                           private val currentTraceContext: CurrentTraceContext
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/all")
    fun getAllContentlets(exchange: ServerWebExchange):Flux<ContentletEntity> {
//        Remove this API with more advanced paginated version.
        logger.info("Getting all")
        return contentletRepository.findAll().doOnComplete {
            WebFluxSleuthOperators.withSpanInScope (tracer, currentTraceContext, exchange) {
                logger.info("All Retrieved")
            }
        }
    }

    @PutMapping
    @ApiResponses(*[ApiResponse(responseCode = "201", description = "created"),
        ApiResponse(responseCode = "200", description = "updated")])
    fun createContentlet(@RequestBody request: ContentletDTO):Mono<ResponseEntity<ContentletEntity>> {
        logger.info("Saving contentlet ${request.id}")
        return contentletService.save(ContentletEntity(
            id = request.id,
            schemalessData = request.schemalessData
        )).map { result ->
            val status = if (result.isNew) HttpStatus.CREATED else HttpStatus.OK
            ResponseEntity<ContentletEntity>(result.contentlet, status)
        }
        .doOnEach {
            WebFluxSleuthOperators.withSpanInScope(it.contextView) {
                if (it.isOnComplete) {
                    logger.info("Contentlet ${request.id} saved successfully")
                }
                if (it.isOnError) {
                    logger.error("Error saving contentlet ${request.id}", it.throwable)
                }
            }
        }
    }

    @DeleteMapping("/{id}")
    fun deleteContentlet(@PathVariable id:String):Mono<Void> {
        return contentletService.deleteById(id)
    }
}