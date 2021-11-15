package com.contented.contentlet

import com.contented.MongodemoApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.function.Consumer

inline fun <reified T> typeReference() = object : ParameterizedTypeReference<T>() {}

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT )
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureDataMongo
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

    private fun saveOneContentlet() = contentletRepository.save(ContentletEntity("12345"))

    @Test
    fun `should return all contentlets`() {

        // Given
        saveOneContentlet().block();

        // When
        var response = webTestClient.get().uri("/all").exchange()

        // Then
        response.expectStatus()
            .is2xxSuccessful()
            .expectBodyList(ContentletEntity::class.java)
                .hasSize(1).value<WebTestClient.ListBodySpec<ContentletEntity>> {contentlets ->
                    assertThat(contentlets[0]).hasFieldOrPropertyWithValue("id", "12345")
                };
    }
}