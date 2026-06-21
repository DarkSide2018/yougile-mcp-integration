package com.yougile.mtprotoproxy.handler

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class RelayBackendHandler(
    private val clientChannel: Channel,
    private val frontHandler: RelayFrontendHandler
) : SimpleChannelInboundHandler<ByteBuf>() {

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val tgDec = frontHandler.tgDecryptor
        if (tgDec == null || !clientChannel.isActive) return
        val clientEnc = frontHandler.clientEncryptor

        val input = ByteArray(msg.readableBytes())
        msg.readBytes(input)

        val decrypted = tgDec.process(input)
        val encrypted = clientEnc.process(decrypted)

        clientChannel.writeAndFlush(Unpooled.wrappedBuffer(encrypted))
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        clientChannel.close()
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        clientChannel.close()
        ctx.close()
    }
}
