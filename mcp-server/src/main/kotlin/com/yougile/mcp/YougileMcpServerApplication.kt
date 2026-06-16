package com.yougile.mcp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class YougileMcpServerApplication

fun main(args: Array<String>) {
    runApplication<YougileMcpServerApplication>(*args)
}
