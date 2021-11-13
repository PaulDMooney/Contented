package com.contented.contentlet

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class ContentletServiceTest {

    private lateinit var contentletRepository: ContentletRepository;

    private lateinit var contentletService: ContentletService;

    val saveHook = ContentletService.SaveHook(){ contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
        next(contentlet, isNew)
            .doOnEach { _ -> println("SaveHookExecuted!") }
    }

    val saveHook2 = ContentletService.SaveHook() { contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
        next(contentlet, isNew)
            .doOnEach { _ -> println("SaveHook2Executed!") }
    }

    @BeforeEach()
    fun setup() {
        contentletRepository = mock();
        contentletService = ContentletService(contentletRepository, listOf(saveHook, saveHook2))
    }

    fun `when repository#save() return input argument`() {
        whenever(contentletRepository.save(any()))
            .thenAnswer { answer -> Mono.just(answer.getArgument(0, ContentletEntity::class.java))}
    }

    fun `when repository#existsById return`(returnValue: Boolean) {
        whenever(contentletRepository.existsById(anyString()))
            .thenReturn(Mono.just(returnValue));
    }

    @Nested()
    inner class BasicOperations {
        @Test
        fun `save should return saved contentlet`() {

            // given
            val contentletToSave = ContentletEntity("1234")
            `when repository#save() return input argument`()
            `when repository#existsById return`(true)

            // when
            val contentletResult = contentletService.save(contentletToSave);

            // then
            StepVerifier.create(contentletResult)
                .assertNext { resultPair ->
                    val contentletEntity = resultPair.contentlet
                    assertThat(contentletEntity.id).isEqualTo(contentletToSave.id)
                }
                .expectComplete()
                .verify()
        }

        @Test
        fun `save should return true if contentlet is new`() {
            // given
            val contentletToSave = ContentletEntity("1234")
            `when repository#save() return input argument`()
            `when repository#existsById return`(false)

            // when
            val contentletResult = contentletService.save(contentletToSave);

            // then
            StepVerifier.create(contentletResult)
                .assertNext { resultPair ->
                    assertThat(resultPair.isNew).isTrue()
                }
                .expectComplete()
                .verify()
        }

        @Test
        fun `save should return false if contentlet is only updated`() {
            // given
            val contentletToSave = ContentletEntity("1234")
            `when repository#save() return input argument`()
            `when repository#existsById return`(true)

            // when
            val contentletResult = contentletService.save(contentletToSave);

            // then
            StepVerifier.create(contentletResult)
                .assertNext { resultPair ->
                    assertThat(resultPair.isNew).isFalse()
                }
                .expectComplete()
                .verify()
        }

        @Test
        fun `delete should call delete in underlying repository with given identifier`() {

            // given
            val identifierToDelete = "12356";
            whenever(contentletRepository.deleteById(anyString())).thenReturn(Mono.empty())
            val deleteIdCaptor: ArgumentCaptor<String> = ArgumentCaptor.forClass(String::class.java)

            // when
            val deleteResult = contentletService.deleteById(identifierToDelete)

            // then
            StepVerifier.create(deleteResult)
                .then {
                    verify(contentletRepository, times(1))
                        .deleteById(deleteIdCaptor.capture())
                    assertThat(deleteIdCaptor.value).isEqualTo(identifierToDelete)
                }
                .verifyComplete()
        }
    }

    @Nested
    inner class SaveHooks {

        @Test
        fun `save should call savehooks in order`() {

            // Given
            val contentletToSave = ContentletEntity("1234")
            `when repository#save() return input argument`()
            `when repository#existsById return`(true)

            val saveHooksCalled = arrayListOf<String>();

            val saveHook1 = ContentletService.SaveHook(){ contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
                saveHooksCalled.add("first");
                next(contentlet, isNew)
            }

            val saveHook2 = ContentletService.SaveHook(){ contentlet: ContentletEntity, isNew: Boolean, next: ContentletService.Next ->
                saveHooksCalled.add("second");
                next(contentlet, isNew)
            }

            var contentletService = ContentletService(contentletRepository, listOf(saveHook1, saveHook2));

            // When
            var result = contentletService.save(contentletToSave);

            // Then
            StepVerifier.create(result)
                .assertNext {
                    assertThat(saveHooksCalled).asList().containsExactly("first", "second");
                }.verifyComplete();

        }
    }
}