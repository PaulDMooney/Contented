package com.example.contently.contentlet

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

    @BeforeEach()
    fun setup() {
        contentletRepository = mock();
        contentletService = ContentletService(contentletRepository)
    }

    @Nested()
    inner class BasicOperations {
        @Test
        fun `save should return saved contentlet`() {

            // given
            val contentletToSave = ContentletEntity("1234", "someValue")
            whenever(contentletRepository.save(any()))
                .thenAnswer {answer -> Mono.just(answer.getArgument(0, ContentletEntity::class.java))}
            whenever(contentletRepository.existsById(anyString()))
                .thenReturn(Mono.just(true));

            // when
            val contentletResult = contentletService.save(contentletToSave);

            // then
            StepVerifier.create(contentletResult)
                .assertNext { resultPair ->
                    val contentletEntity = resultPair.contentlet
                    assertThat(contentletEntity.id).isEqualTo(contentletToSave.id)
                    assertThat(contentletEntity.someValue).isEqualTo(contentletToSave.someValue)
                }
                .expectComplete()
                .verify()
        }

        @Test
        fun `save should return true if contentlet is new`() {
            // given
            val contentletToSave = ContentletEntity("1234", "someValue")
            whenever(contentletRepository.save(any()))
                .thenAnswer {answer -> Mono.just(answer.getArgument(0, ContentletEntity::class.java))}
            whenever(contentletRepository.existsById(anyString()))
                .thenReturn(Mono.just(false));

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
            val contentletToSave = ContentletEntity("1234", "someValue")
            whenever(contentletRepository.save(any()))
                .thenAnswer {answer -> Mono.just(answer.getArgument(0, ContentletEntity::class.java))}
            whenever(contentletRepository.existsById(anyString()))
                .thenReturn(Mono.just(true));

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
}