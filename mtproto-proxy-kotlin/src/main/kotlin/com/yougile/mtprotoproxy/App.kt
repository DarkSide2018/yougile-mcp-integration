package com.yougile.mtprotoproxy

import com.yougile.mtprotoproxy.config.ProxyConfig
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(ProxyConfig::class)
class App

fun main(args: Array<String>) {
    runApplication<App>(*args)
}
