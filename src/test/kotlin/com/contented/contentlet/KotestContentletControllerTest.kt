package com.contented.contentlet

import io.kotest.core.listeners.TestListener
import io.kotest.core.script.describe
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.core.spec.style.FunSpec
import io.kotest.spring.SpringListener
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
class KotestContentletControllerTest: DescribeSpec() {

    override fun listeners(): List<TestListener> = listOf(SpringListener)

    @Autowired
    private lateinit var contentletRepository: ContentletRepository

    private lateinit var webTestClient: WebTestClient;

    @LocalServerPort
    protected var port: Int = 0;

    private fun getRootUrl(): String = "http://localhost:$port/contentlets"

    private fun saveOneContentlet() = contentletRepository.save(ContentletEntity("12345",hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string")))

    init {

        beforeEach {
            contentletRepository.deleteAll();
            webTestClient = WebTestClient.bindToServer().baseUrl(getRootUrl()).build();
        }

        describe("GET /all endpoint") {
            it("should return all saved contentlets") {
                // Given
                saveOneContentlet().block();

                // When
                val response = webTestClient.get().uri("/all").exchange()

                // Then
                response.expectStatus()
                    .is2xxSuccessful()
                    .expectBodyList(ContentletEntity::class.java)
                    .hasSize(1).value<WebTestClient.ListBodySpec<ContentletEntity>> { contentlets ->
                        Assertions.assertThat(contentlets[0]).hasFieldOrPropertyWithValue("id", "12345")
                        Assertions.assertThat(contentlets[0].get()).hasFieldOrPropertyWithValue("myint", 1)
                    };
            }
        }

        describe("PUT endpoint") {

            describe("save a new contentlet") {

                lateinit var toSave: ContentletDTO;
                lateinit var response: WebTestClient.ResponseSpec
                beforeEach {
                    // Given
                    toSave = ContentletDTO("ABCDE", hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string"))

                    // When
                    response = webTestClient.put().bodyValue(toSave).exchange()
                }

                it("should return status ${HttpStatus.CREATED.value()}") {
                    response.expectStatus().isEqualTo(HttpStatus.CREATED)
                }

                it("should have saved to the database") {
                    val savedContentlet = contentletRepository.findById(toSave.id).block()

                    // Found entry in database with same id and fields
                    Assertions.assertThat(savedContentlet).hasFieldOrPropertyWithValue("id", toSave.id)
                    Assertions.assertThat(savedContentlet?.get()).hasFieldOrPropertyWithValue("mystring", toSave.get()["mystring"])
                }

                it("should return the saved contentlet") {
                    response.expectBody().jsonPath("$.id").isEqualTo(toSave.id)
                        .jsonPath("$.mystring").isEqualTo(toSave.get()["mystring"]!!)
                }
            }

            describe("update an existing contentlet") {

                lateinit var toSave: ContentletDTO;
                lateinit var response: WebTestClient.ResponseSpec;

                beforeEach {
                    // Given
                    val firstContentlet = ContentletEntity("FGHIJK", hashMapOf("myint" to 1) )
                    contentletRepository.save(firstContentlet).block()
                    toSave = ContentletDTO(firstContentlet.id, hashMapOf("myboolean" to true))

                    // When
                    response = webTestClient.put().bodyValue(toSave).exchange()
                }

                it("should return status ${HttpStatus.OK.value()}") {
                    response.expectStatus().isEqualTo(HttpStatus.OK)
                }

                it("should have saved updates to the database with old fields removed") {
                    val updatedContentlet = contentletRepository.findById(toSave.id).block()
                    Assertions.assertThat(updatedContentlet?.get()).hasFieldOrPropertyWithValue("myboolean", true)
                    Assertions.assertThat(updatedContentlet?.get()).doesNotContainEntry("myint", 1)
                }

                it("should return the saved contentlet with old fields removed") {
                    response.expectBody().jsonPath("$.id").isEqualTo(toSave.id)
                        .jsonPath("$.myboolean").isEqualTo(toSave.get()["myboolean"]!!)
                        .jsonPath("$.myint").doesNotExist()
                }

            }
        }
        describe("DELETE endpoint") {

            describe("delete existing content by identifier") {

                val storedContentlet = ContentletEntity("FGHIJK", hashMapOf("myint" to 1))

                lateinit var response: WebTestClient.ResponseSpec

                beforeEach {
                    // Given
                    contentletRepository.save(storedContentlet).block()

                    // When
                    response = webTestClient.delete().uri("/${storedContentlet.id}").exchange()
                }

                it("should return status ${HttpStatus.OK.value()}") {
                    response.expectStatus().isEqualTo(HttpStatus.OK)
                }

                it("should delete the contentlet from the database") {
                    val result = contentletRepository.findById(storedContentlet.id).block()
                    assertThat(result).isNull()
                }
            }
        }
    }

}