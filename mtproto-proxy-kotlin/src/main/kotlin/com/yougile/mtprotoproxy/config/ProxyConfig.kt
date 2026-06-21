package com.yougile.mtprotoproxy.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mtproto-proxy")
data class ProxyConfig(
    var port: Int = 443,
    var host: String = "0.0.0.0",
    var secret: String = "00000000000000000000000000000001",
    var tlsDomain: String = "www.google.com",
    var adTag: String = "",
    var modes: Modes = Modes(),
    var socks: SocksConfig = SocksConfig()
) {
    data class Modes(
        var classic: Boolean = false,
        var secure: Boolean = false,
        var tls: Boolean = true
    )

    data class SocksConfig(
        var enabled: Boolean = false,
        var port: Int = 1080,
        var host: String = "0.0.0.0"
    )
}
