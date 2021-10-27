package ch.frankel.blog

import io.micronaut.context.annotation.ConfigurationInject
import io.micronaut.context.annotation.ConfigurationProperties

@ConfigurationProperties("micronaut.application.marvel")
data class MarvelProperties @ConfigurationInject constructor(
    val serverUrl: String,
    val apiKey: String?,
    val privateKey: String?
)