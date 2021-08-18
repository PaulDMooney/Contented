package com.example.contently.contentlet

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*
import kotlin.collections.ArrayDeque

@Service
class ContentletService @Autowired constructor(
    val contentletRepository: ContentletRepository,
    val saveHooks: List<SaveHook> = Collections.emptyList()) {

    fun save(contentletToSave: ContentletEntity): Mono<ResultPair> {
        return contentletRepository.existsById(contentletToSave.id)
            .flatMap { result ->
                callSaveHookChain(contentletToSave, !result);
            }
    }

    private fun callSaveHookChain(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair> {
        val saveHookChain = SaveHookChain(saveHooks, this::finalSaveHook);
        return saveHookChain.next(contentletToSave, isNew);
    }

    private fun finalSaveHook(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair> {
        return contentletRepository.save(contentletToSave)
            .map { savedContentlet -> ResultPair(contentlet = savedContentlet, isNew = isNew)}
    }

    fun deleteById(identifierToDelete: String): Mono<Void> {
        return contentletRepository.deleteById(identifierToDelete)
    }

    class ResultPair(val contentlet: ContentletEntity, val isNew: Boolean)

    class SaveHookChain {

        val saveHookStack: ArrayDeque<SaveHook>;
        val saveFunction: Next;

        constructor(savehookList: List<SaveHook>, saveFunction: Next) {
            saveHookStack = ArrayDeque(savehookList)
            this.saveFunction = saveFunction;
        }

        fun next(contentletToSave: ContentletEntity, isNew: Boolean):Mono<ResultPair> {
            val saveHook = saveHookStack.removeFirstOrNull();
            return if (saveHook != null) {
                saveHook(contentletToSave, isNew, this::next)
            } else {
                saveFunction(contentletToSave, isNew);
            }
        }
    }

    fun interface SaveHook {
        operator fun invoke(contentletToSave: ContentletEntity, isNew: Boolean, next: Next): Mono<ResultPair>
    }

    fun interface Next {
        operator fun invoke(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair>
    }

}