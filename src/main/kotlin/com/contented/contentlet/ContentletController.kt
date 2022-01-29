package com.contented.contentlet

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/contentlets")
class ContentletController(private val contentletRepository: ContentletRepository,
                           private val contentletService: ContentletService
) {

    val logger: Logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/all")
    fun getAllContentlets():Flux<ContentletEntity> {
//        Remove this API with more advanced paginated version.
        logger.info("Getting all")
        return contentletRepository.findAll().doOnComplete {
            logger.info("Retrieved")
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
        }.doOnSuccess {
            logger.info("Contentlet ${request.id} saved successfully")
        }.doOnError {
            logger.error("Error saving contentlet ${request.id}", it);
        }
    }

    @DeleteMapping("/{id}")
    fun deleteContentlet(@PathVariable id:String):Mono<Void> {
        return contentletService.deleteById(id)
    }
}