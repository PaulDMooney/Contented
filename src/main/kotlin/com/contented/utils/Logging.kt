package com.contented.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gets a logger for the calling class. Assumes it is a companion object and removes the `$Companion` part of the name
 */
inline fun <reified R : Any> R.companionLogger(): Logger =
    LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))