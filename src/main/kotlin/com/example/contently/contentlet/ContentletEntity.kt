package com.example.contently.contentlet


import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class ContentletEntity (

    @Id
    val id: String,
    val someValue: String
)