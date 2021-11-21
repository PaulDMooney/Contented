package com.contented.contentlet

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
@DirtiesContext
class ContentletControllerTest(@Autowired contentletRepository:ContentletRepository, @LocalServerPort port:Int): DescribeSpec() {

    override fun extensions() = listOf(SpringExtension)

    init {

        lateinit var webTestClient: WebTestClient;

        fun saveOneContentlet() = contentletRepository.save(ContentletEntity("12345",hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string")))

        fun getRootUrl(): String = "http://localhost:$port/contentlets"

        beforeContainer {
            webTestClient = WebTestClient.bindToServer().baseUrl(getRootUrl()).build();
        }

        afterEach {
            contentletRepository.deleteAll();
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
                        contentlets[0].id shouldBe "12345"
                        contentlets[0].get() shouldContain ("myint" to 1)
                    };
            }
        }

        describe("PUT endpoint") {

            describe("save a new contentlet") {

                // Given
                val toSave = ContentletDTO("ABCDE", hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string"))

                // When
                val response = webTestClient.put().bodyValue(toSave).exchange()

                it("should return status 201") {
                    response.expectStatus().isEqualTo(HttpStatus.CREATED)
                }

                it("should have saved to the database") {
                    val savedContentlet = contentletRepository.findById(toSave.id).block()

                    // Found entry in database with same id and fields
                    savedContentlet!!.id shouldBe toSave.id
                    savedContentlet.get() shouldContain ("mystring" to toSave.get()["mystring"])
                }

                it("should return the saved contentlet") {
                    response.expectBody().jsonPath("$.id").isEqualTo(toSave.id)
                        .jsonPath("$.mystring").isEqualTo(toSave.get()["mystring"]!!)
                }
            }

            describe("update an existing contentlet") {

                // Given
                val firstContentlet = ContentletEntity("FGHIJK", hashMapOf("myint" to 1) )
                contentletRepository.save(firstContentlet).block()
                val toSave = ContentletDTO(firstContentlet.id, hashMapOf("myboolean" to true))

                // When
                val response = webTestClient.put().bodyValue(toSave).exchange()

                it("should return status 200") {
                    response.expectStatus().isEqualTo(HttpStatus.OK)
                }

                it("should have saved updates to the database with old fields removed") {
                    val updatedContentlet = contentletRepository.findById(toSave.id).block()

                    updatedContentlet!!.get() shouldContain ("myboolean" to true)
                    updatedContentlet.get() shouldNotContainKey "myint"
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

                // Given
                val storedContentlet = ContentletEntity("FGHIJK", hashMapOf("myint" to 1))
                contentletRepository.save(storedContentlet).block()

                // When
                val response = webTestClient.delete().uri("/${storedContentlet.id}").exchange()


                it("should return status 200") {
                    response.expectStatus().isEqualTo(HttpStatus.OK)
                }

                it("should delete the contentlet from the database") {
                    val result = contentletRepository.findById(storedContentlet.id).block()
                    result shouldBe null
                }
            }

            describe( "delete non-existent content by identifier") {

                // When
                val response = webTestClient.delete().uri("/ABCDE").exchange()

                it("should return status 200") {
                    response.expectStatus().isEqualTo(HttpStatus.OK)
                }
            }
        }
    }

}