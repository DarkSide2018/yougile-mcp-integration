package com.yougile.mtprotoproxy.server

import com.yougile.mtprotoproxy.config.ProxyConfig
import com.yougile.mtprotoproxy.handler.HandshakeHandler
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component

@Component
class MtProtoProxyServer(private val config: ProxyConfig) {

    private val bossGroup: EventLoopGroup = NioEventLoopGroup(1)
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()
    private var mtprotoChannel: Channel? = null
    private var socksChannel: Channel? = null

    @PostConstruct
    fun start() {
        startMtProto()
        startSocks()
    }

    private fun startMtProto() {
        println("MTProto Proxy starting on ${config.host}:${config.port}")
        println("Modes: classic=${config.modes.classic}, secure=${config.modes.secure}, tls=${config.modes.tls}")
        println("TLS domain: ${config.tlsDomain}")
        println()

        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(HandshakeHandler(config))
                }
            })
            .option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)

        try {
            val future = b.bind(config.host, config.port).sync()
            mtprotoChannel = future.channel()
            println("MTProto listening on ${config.host}:${config.port}")
        } catch (e: Exception) {
            System.err.println("Failed to start MTProto proxy: ${e.message}")
        }
    }

    private fun startSocks() {
        if (!config.socks.enabled) return
        println("SOCKS5 proxy is currently not configured.")
    }

    @PreDestroy
    fun stop() {
        println("Shutting down...")
        mtprotoChannel?.close()
        socksChannel?.close()
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }
}
