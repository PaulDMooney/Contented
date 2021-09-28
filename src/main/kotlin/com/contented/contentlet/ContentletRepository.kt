package com.contented.contentlet

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface ContentletRepository: ReactiveMongoRepository<ContentletEntity, String> {

}