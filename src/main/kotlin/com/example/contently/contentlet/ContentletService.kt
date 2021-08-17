package com.example.contently.contentlet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class ContentletService @Autowired constructor(var contentletRepository: ContentletRepository) {

    fun save(contentletToSave: ContentletEntity): Mono<ResultPair> {

        return contentletRepository.existsById(contentletToSave.id)
            .flatMap { result ->
                contentletRepository.save(contentletToSave)
                    .map { savedContentlet -> ResultPair(contentlet = savedContentlet, isNew = !result)}
            }
    }

    fun deleteById(identifierToDelete: String): Mono<Void> {
        return contentletRepository.deleteById(identifierToDelete)
    }

    class ResultPair(val contentlet: ContentletEntity, val isNew: Boolean)

}