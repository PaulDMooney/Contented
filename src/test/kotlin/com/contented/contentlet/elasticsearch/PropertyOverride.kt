package com.contented.contentlet.elasticsearch

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.util.SocketUtils

class PropertyOverride : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        val freeEsSocket = SocketUtils.findAvailableTcpPort(9201,9299)
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(applicationContext,"elasticsearch.port=$freeEsSocket")
    }

}