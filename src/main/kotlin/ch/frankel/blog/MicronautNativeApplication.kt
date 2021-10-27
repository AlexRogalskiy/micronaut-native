package ch.frankel.blog

import io.micronaut.runtime.Micronaut.build

fun main(args: Array<String>) {
    build().args(*args)
        .packages("ch.frankel.blog")
        .start()
}

