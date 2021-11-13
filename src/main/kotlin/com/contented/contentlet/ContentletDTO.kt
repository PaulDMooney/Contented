package com.contented.contentlet

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import java.util.LinkedHashMap

class ContentletDTO(
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