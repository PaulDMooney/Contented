package com.contented.contentlet

import com.contented.utils.companionLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*
import kotlin.collections.ArrayDeque

@Service
class ContentletService @Autowired constructor(
    val contentletRepository: ContentletRepository,
    val saveHooks: List<SaveHook> = Collections.emptyList()) {

    companion object {
        val logger: Logger = companionLogger()
    }

    fun save(contentletToSave: ContentletEntity): Mono<ResultPair> {
        return contentletRepository.existsById(contentletToSave.id)
            .flatMap { result ->
                if (result) {
                    logger.info("Contentlet ${contentletToSave.id} already exists, updating...")
                } else {
                    logger.info("Contentlet ${contentletToSave.id} does not exist, creating...")
                }
                callSaveHookChain(contentletToSave, !result);
            }
    }

    private fun callSaveHookChain(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair> {
        val saveHookChain = SaveHookChain(saveHooks, this::finalSaveHook);
        return saveHookChain.next(contentletToSave, isNew);
    }

    private fun finalSaveHook(contentletToSave: ContentletEntity, isNew: Boolean): Mono<ResultPair> {
        return contentletRepository.save(contentletToSave)
            .doOnNext { logger.info("Contentlet ${contentletToSave.id} saved to repository") }
            .map { savedContentlet -> ResultPair(contentlet = savedContentlet, isNew = isNew)}
    }

    fun deleteById(identifierToDelete: String): Mono<Void> {
        logger.info("Deleting contentlet $identifierToDelete")
        return contentletRepository.deleteById(identifierToDelete).doOnSuccess {
            logger.info("Deleted contentlet $identifierToDelete successfully")
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
            return if (saveHook != null) {
                logger.debug("Calling savehook", saveHook)
                saveHook(contentletToSave, isNew, this::next)
            } else {
                logger.debug("Calling final save function")
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