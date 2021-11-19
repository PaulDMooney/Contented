package com.contented.contentlet

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
@DirtiesContext
class ContentletControllerTest @Autowired constructor(

    private val contentletRepository: ContentletRepository) {

    private lateinit var webTestClient: WebTestClient;

    @LocalServerPort
    protected var port: Int = 0;

    @BeforeEach
    fun setup() {
        contentletRepository.deleteAll();
        webTestClient = WebTestClient.bindToServer().baseUrl(getRootUrl()).build();
    }

    private fun getRootUrl(): String = "http://localhost:$port/contentlets"

    private fun saveOneContentlet() = contentletRepository.save(ContentletEntity("12345",hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string")))

    @Test
    fun `should return saved contentlet`() {

        // Given
        saveOneContentlet().block();

        // When
        val response = webTestClient.get().uri("/all").exchange()

        // Then
        response.expectStatus()
            .is2xxSuccessful()
            .expectBodyList(ContentletEntity::class.java)
                .hasSize(1).value<WebTestClient.ListBodySpec<ContentletEntity>> {contentlets ->
                    assertThat(contentlets[0]).hasFieldOrPropertyWithValue("id", "12345")
                    assertThat(contentlets[0].get()).hasFieldOrPropertyWithValue("myint",1)
                };
    }

    @Test
    fun `should save new contentlet and return 201 and contentlet`() {

        // Given
        val toSave = ContentletDTO("ABCDE", hashMapOf("myint" to 1, "myboolean" to true, "mystring" to "string"))

        // When
        val response = webTestClient.put().bodyValue(toSave).exchange()

        // Then
        // Status 201 when created
        response.expectStatus().isEqualTo(HttpStatus.CREATED)
        val savedContentlet = contentletRepository.findById(toSave.id).block()

        // Found entry in database with same id and fields
        assertThat(savedContentlet).hasFieldOrPropertyWithValue("id", toSave.id)
        assertThat(savedContentlet?.get()).hasFieldOrPropertyWithValue("mystring", toSave.get()["mystring"])

        //
        response.expectBody().jsonPath("$.id").isEqualTo(toSave.id)
            .jsonPath("$.mystring").isEqualTo(toSave.get()["mystring"]!!)

    }

    @Test
    fun `should update existing contentlet and return 200 and contentlet`() {

        // Given
        val firstContentlet = ContentletEntity("FGHIJK", hashMapOf("myint" to 1) )
        contentletRepository.save(firstContentlet).block()
        val toSave = ContentletDTO(firstContentlet.id, hashMapOf("myboolean" to true))

        // When
        val response = webTestClient.put().bodyValue(toSave).exchange()

        // Then
        response.expectStatus().isEqualTo(HttpStatus.OK)
        val updatedContentlet = contentletRepository.findById(toSave.id).block()
        assertThat(updatedContentlet?.get()).hasFieldOrPropertyWithValue("myboolean", true)
        assertThat(updatedContentlet?.get()).doesNotContainEntry("myint", 1)
    }
}