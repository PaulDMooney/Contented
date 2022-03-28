package com.contented.testutil

import io.kotest.core.script.context
import org.mockito.kotlin.mock
import org.springframework.cloud.sleuth.CurrentTraceContext
import org.springframework.cloud.sleuth.TraceContext
import org.springframework.cloud.sleuth.Tracer
import reactor.core.publisher.Mono

/**
 * Adds just enough for code using sleuth to get by in a Unit test where Sleuth is not important
 */
class MockSleuthTraceContext(
    val currentTraceContext: CurrentTraceContext = mock(),
    val traceContext: TraceContext = mock(),
    val tracer: Tracer = mock()
)    {
    fun <T> addMockTraceContext(mono: Mono<T>):Mono<T> {
        return mono.contextWrite { context -> context.put(CurrentTraceContext::class.java, currentTraceContext)
            .put(Tracer::class.java, tracer)
            .put(TraceContext::class.java, traceContext) }
    }
}