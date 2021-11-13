package com.contented.contentlet


import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document
data class ContentletEntity (

    @Id
    val id: String,
    val schemalessData: MutableMap<String, Any> = LinkedHashMap()
) {
    @JsonAnySetter
    fun add(key:String, value: Any) {
        schemalessData[key] = value;
    }

    @JsonAnyGetter
    fun get(): Map<String, Any> = schemalessData
}