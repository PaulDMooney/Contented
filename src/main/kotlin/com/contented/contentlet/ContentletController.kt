package com.contented.contentlet

import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
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
    @GetMapping("/all")
    fun getAllContentlets():Flux<ContentletEntity> {
//        Remove this API with more advanced paginated version.
        return contentletRepository.findAll();
    }

    @PutMapping
    @ApiResponses(*[ApiResponse(responseCode = "201", description = "created"),
        ApiResponse(responseCode = "200", description = "updated")])
    fun createContentlet(@RequestBody request: ContentletDTO):Mono<ResponseEntity<ContentletEntity>> {
        return contentletService.save(ContentletEntity(
            id = request.id,
            someValue = request.someValue
        )).map { result ->
            val status = if (result.isNew) HttpStatus.CREATED else HttpStatus.OK
            ResponseEntity<ContentletEntity>(result.contentlet, status)
        }
    }

    @DeleteMapping("/{id}")
    fun deleteContentlet(@PathVariable id:String):Mono<Void> {
        return contentletService.deleteById(id)
    }
}