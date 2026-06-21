package com.yougile.mtprotoproxy.handler

import com.yougile.mtprotoproxy.crypto.AesCtrCryptor
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class RelayFrontendHandler(
    val clientDecryptor: AesCtrCryptor,
    val clientEncryptor: AesCtrCryptor
) : SimpleChannelInboundHandler<ByteBuf>() {

    var tgChannel: Channel? = null
    var tgDecryptor: AesCtrCryptor? = null
    var tgEncryptor: AesCtrCryptor? = null

    private var pendingBuffer = Unpooled.buffer()

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val tg = tgChannel
        val tgEnc = tgEncryptor

        if (tg == null || tgEnc == null || !tg.isActive) {
            pendingBuffer.writeBytes(msg)
            return
        }

        flushPending(tg, tgEnc)

        val input = ByteArray(msg.readableBytes())
        msg.readBytes(input)

        val decrypted = clientDecryptor.process(input)
        val encrypted = tgEnc.process(decrypted)

        tg.writeAndFlush(Unpooled.wrappedBuffer(encrypted))
    }

    private fun flushPending(tg: Channel, tgEnc: AesCtrCryptor) {
        if (!pendingBuffer.isReadable) return
        val input = ByteArray(pendingBuffer.readableBytes())
        pendingBuffer.readBytes(input)
        val decrypted = clientDecryptor.process(input)
        val encrypted = tgEnc.process(decrypted)
        tg.writeAndFlush(Unpooled.wrappedBuffer(encrypted))
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        tgChannel?.close()
        super.channelInactive(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        tgChannel?.close()
        ctx.close()
    }
}
