package com.example.contently.contentlet

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.RestTemplate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(SpringExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ContentletControllerTest @Autowired constructor(
    private val contentletRepository: ContentletRepository,
    private val restTemplate: RestTemplate) {

    @LocalServerPort
    protected var port: Int = 0;

    @BeforeEach
    fun setup() {
        contentletRepository.deleteAll();
    }

    private fun getRootUrl(): String = "http://localhost:$port/contentlets"

    private fun saveOneContentlet() = contentletRepository.save(ContentletEntity("12345","SomeValue"))

    @Test
    fun `should return all contentlets`() {

        // Given
        saveOneContentlet();

        // When
        val response = restTemplate.getForEntity("${getRootUrl()}/all",List::class.java);

        // Then
        assertEquals(200, response.statusCode.value());
        assertEquals(1, response.body?.size);
    }
}