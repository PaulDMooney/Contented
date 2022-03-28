package com.contented.contentlet

import com.contented.utils.companionLogger
import com.contented.utils.doOnNextWithSpanInScope
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators
import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators.withSpanInScope
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.util.context.ContextView
import java.util.*
import java.util.concurrent.Callable
import kotlin.collections.ArrayDeque

@Service
class ContentletService @Autowired constructor(
    val contentletRepository: ContentletRepository,
    val saveHooks: List<SaveHook> = Collections.emptyList()) {

    companion object {
        val logger: Logger = companionLogger()
    }

    fun save(contentletToSave: ContentletEntity): Mono<ResultPair> {
        return Mono.deferContextual { contextView ->
            withSpanInScope(contextView) {
                logger.info("Creating or updating contentlet ${contentletToSave.id}")
            }
            contentletRepository.existsById(contentletToSave.id)
                .flatMap { result ->
                    withSpanInScope(contextView) {
                        if (result) {
                            logger.info("Contentlet ${contentletToSave.id} already exists, updating...")
                        } else {
                            logger.info("Contentlet ${contentletToSave.id} does not exist, creating...")
                        }
                    }
                    callSaveHookChain(contentletToSave, !result);
                }
        }
    }

    private fun callSaveHookChain(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair> {
        val saveHookChain = SaveHookChain(saveHooks, this::finalSaveHook);
        return saveHookChain.next(contentletToSave, isNew);
    }

    private fun finalSaveHook(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair> {
        return contentletRepository.save(contentletToSave)
            .doOnNextWithSpanInScope {
                logger.info("Contentlet ${contentletToSave.id} saved to repository")
            }
            .map { savedContentlet -> ResultPair(contentlet = savedContentlet, isNew = isNew)}
    }

    fun deleteById(identifierToDelete: String): Mono<Void> {
        return Mono.deferContextual { contextView ->
            withSpanInScope(contextView) {
                logger.info("Deleting contentlet $identifierToDelete")
            }
            contentletRepository.deleteById(identifierToDelete).doOnSuccess {
                withSpanInScope(contextView) {
                    logger.info("Deleted contentlet $identifierToDelete successfully")
                }
            }
        }

    }

    class ResultPair(val contentlet: ContentletEntity, val isNew: Boolean)

    class SaveHookChain(savehookList: List<SaveHook>, val saveFunction: Next) {

        companion object {
            val logger: Logger = companionLogger()
        }

        private val saveHookStack: ArrayDeque<SaveHook> = ArrayDeque(savehookList)

        fun next(contentletToSave: ContentletEntity, isNew: Boolean):Mono<ResultPair> {
            val saveHook = saveHookStack.removeFirstOrNull();

            return withSpanScope {
                if (saveHook != null) {
                    logger.debug("Calling savehook", saveHook)
                    saveHook(contentletToSave, isNew, this::next)
                } else {
                    logger.debug("Calling final save function")
                    saveFunction(contentletToSave, isNew)
                }
            }
        }

        private fun <T> withSpanScope(callable: Callable<Mono<T>>):Mono<T> {
            return Mono.deferContextual { contextView ->
                withSpanInScope(contextView, callable)
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