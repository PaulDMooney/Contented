package com.contented.contentlet.elasticsearch

import org.springframework.data.elasticsearch.annotations.Document

@Document(indexName = "myIndex")
class ContentletDoc(val id: String) {

}