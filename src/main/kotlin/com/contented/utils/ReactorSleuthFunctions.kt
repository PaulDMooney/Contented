package com.contented.utils

import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators
import reactor.core.publisher.Mono

fun <T> Mono<T>.doOnNextWithSpanInScope(signalConsumer: Runnable): Mono<T> {
    return this.doOnEach { signal ->
        if (signal.isOnNext) {
            WebFluxSleuthOperators.withSpanInScope(signal.contextView, signalConsumer)
        }
    }
}