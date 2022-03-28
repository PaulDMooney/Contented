package com.contented.contentlet

import com.contented.testutil.MockSleuthTraceContext
import io.kotest.core.spec.style.DescribeSpec
import org.assertj.core.api.Assertions
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.cloud.sleuth.CurrentTraceContext
import org.springframework.cloud.sleuth.TraceContext
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.brave.bridge.BraveCurrentTraceContext
import org.springframework.cloud.sleuth.instrument.web.WebFluxSleuthOperators
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

val saveHook = ContentletService.SaveHook(){ contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
    next(contentlet, isNew)
        .doOnEach { _ -> println("SaveHookExecuted!") }
}

val saveHook2 = ContentletService.SaveHook() { contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
    next(contentlet, isNew)
        .doOnEach { _ -> println("SaveHook2Executed!") }
}

class KotestContentletServiceTest: DescribeSpec() {

    lateinit var contentletRepository: ContentletRepository

    lateinit var contentletService: ContentletService

    lateinit var mockSleuthTraceContext: MockSleuthTraceContext

    fun `when repository#save() return input argument`() {
        whenever(contentletRepository.save(ArgumentMatchers.any()))
            .thenAnswer { answer -> Mono.just(answer.getArgument(0, ContentletEntity::class.java))}
    }

    fun `when repository#existsById return`(returnValue: Boolean) {
        whenever(contentletRepository.existsById(ArgumentMatchers.anyString()))
            .thenReturn(Mono.just(returnValue));
    }
    init {

        beforeEach {

            contentletRepository = mock()
            mockSleuthTraceContext = MockSleuthTraceContext()
            contentletService = ContentletService(contentletRepository, listOf(saveHook, saveHook2))
        }

        describe("save") {

            it("should return saved contentlet") {
                // given
                val contentletToSave = ContentletEntity("1234")
                `when repository#save() return input argument`()
                `when repository#existsById return`(true)

                // when
                val contentletResult = contentletService.save(contentletToSave)


                // then
                StepVerifier.create(mockSleuthTraceContext.addMockTraceContext(contentletResult))
                    .assertNext { resultPair ->
                        val contentletEntity = resultPair.contentlet
                        Assertions.assertThat(contentletEntity.id).isEqualTo(contentletToSave.id)
                    }
                    .expectComplete()
                    .verify()
            }

            it("should return true if contentlet is new") {
                // given
                val contentletToSave = ContentletEntity("1234")
                `when repository#save() return input argument`()
                `when repository#existsById return`(false)

                // when
                val contentletResult = contentletService.save(contentletToSave)

                // then
                StepVerifier.create(mockSleuthTraceContext.addMockTraceContext(contentletResult))
                    .assertNext { resultPair ->
                        Assertions.assertThat(resultPair.isNew).isTrue()
                    }
                    .expectComplete()
                    .verify()
            }
        }

        describe("delete") {
            it("should call delete in underlying repository with given identifier") {
                // given
                val identifierToDelete = "12356";
                whenever(contentletRepository.deleteById(ArgumentMatchers.anyString())).thenReturn(Mono.empty())
                val deleteIdCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)

                // when
                val deleteResult = contentletService.deleteById(identifierToDelete)

                // then
                StepVerifier.create(mockSleuthTraceContext.addMockTraceContext(deleteResult))
                    .then {
                        Mockito.verify(contentletRepository, Mockito.times(1))
                            .deleteById(deleteIdCaptor.capture())
                        Assertions.assertThat(deleteIdCaptor.value).isEqualTo(identifierToDelete)
                    }
                    .verifyComplete()
            }
        }

        describe("save with savehooks") {
            it("should call savehooks in order") {
                // Given
                val contentletToSave = ContentletEntity("1234")
                `when repository#save() return input argument`()
                `when repository#existsById return`(true)

                val saveHooksCalled = arrayListOf<String>();

                val saveHook1 = ContentletService.SaveHook(){ contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
                    saveHooksCalled.add("first")
                    next(contentlet, isNew)
                }

                val saveHook2 = ContentletService.SaveHook(){ contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
                    saveHooksCalled.add("second")
                    next(contentlet, isNew)
                }

                var contentletService = ContentletService(contentletRepository, listOf(saveHook1, saveHook2));

                // When
                var result = contentletService.save(contentletToSave);

                // Then
                StepVerifier.create(mockSleuthTraceContext.addMockTraceContext(result))
                    .assertNext {
                        Assertions.assertThat(saveHooksCalled).asList().containsExactly("first", "second");
                    }.verifyComplete();
            }
        }
    }
}