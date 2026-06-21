package com.yougile.mtprotoproxy.handler

import com.yougile.mtprotoproxy.Constants
import com.yougile.mtprotoproxy.config.ProxyConfig
import com.yougile.mtprotoproxy.crypto.AesCtrCryptor
import com.yougile.mtprotoproxy.crypto.hexToBytes
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOption
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class HandshakeHandler(private val config: ProxyConfig) : ByteToMessageDecoder() {

    private var prometheusState = 0
    private var isTls = false
    private var clientHello: ByteArray? = null
    private var clientDecryptor: AesCtrCryptor? = null
    private var clientEncryptor: AesCtrCryptor? = null
    private var dcAddress: String? = null
    private val secret = hexToBytes(config.secret)
    private val random = SecureRandom()
    private var frontHandler: RelayFrontendHandler? = null

    override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
        while (input.isReadable) {
            when (prometheusState) {
                0 -> {
                    val b = input.readByte().toInt() and 0xFF
                    isTls = b == 0x16
                    prometheusState = 1
                }
                1 -> {
                    if (!isTls) { prometheusState = 3; continue }
                    if (!config.modes.tls) { fail(ctx); return }
                    if (input.readableBytes() < 4) return
                    val v1 = input.readByte().toInt() and 0xFF
                    val v2 = input.readByte().toInt() and 0xFF
                    if (v1 != 0x03 || v2 != 0x01) { fail(ctx); return }
                    val len = input.readUnsignedShort()
                    if (len < 512) { fail(ctx); return }
                    prometheusState = 2
                }
                2 -> {
                    if (input.readableBytes() < 2) return
                    val bodyLen = input.readUnsignedShort()
                    if (input.readableBytes() < bodyLen) return
                    val hello = ByteArray(bodyLen)
                    input.readBytes(hello)
                    clientHello = hello
                    prometheusState = 3
                    if (!sendFakeTls(ctx)) { fail(ctx); return }
                }
                3 -> {
                    val need = if (isTls) 64 else 63
                    if (input.readableBytes() < need) return

                    val pre = ByteArray(64)
                    if (isTls) {
                        input.readBytes(pre, 0, 64)
                    } else {
                        pre[0] = 0xef.toByte()
                        input.readBytes(pre, 1, 63)
                    }

                    if (!processPre(pre)) { fail(ctx); return }

                    val fh = RelayFrontendHandler(clientDecryptor!!, clientEncryptor!!)
                    frontHandler = fh
                    ctx.pipeline().addAfter(ctx.name(), "relay", fh)
                    ctx.pipeline().remove(this)

                    connectToTg(ctx, fh)

                    if (input.isReadable) {
                        val data = ByteArray(input.readableBytes())
                        input.readBytes(data)
                        out.add(Unpooled.wrappedBuffer(data))
                    }
                    return
                }
            }
        }
    }

    private fun sendFakeTls(ctx: ChannelHandlerContext): Boolean {
        val hello = clientHello ?: return false
        val dp = 11; val dl = 32
        if (hello.size < dp + dl) return false

        val received = hello.sliceArray(dp until dp + dl)
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(secret, "HmacSHA256"))
        }
        val msg = hello.copyOf().also { for (i in dp until dp + dl) it[i] = 0 }
        val computed = mac.doFinal(msg)

        val xored = ByteArray(dl) { (received[it].toInt() xor computed[it].toInt()).toByte() }
        if (xored.sliceArray(0 until dl - 4).any { it.toInt() != 0 }) return false

        val sidLen = hello[dp + dl].toInt() and 0xFF
        val sid = hello.sliceArray(dp + dl + 1 until dp + dl + 1 + sidLen)
        val srvRand = ByteArray(32).also { random.nextBytes(it) }

        val srv = concat(
            byteArrayOf(0x03, 0x03), ByteArray(dl),
            byteArrayOf(sidLen.toByte()), sid,
            byteArrayOf(0x13, 0x01, 0x00),
            hexToBytes("002b000302030400330024001d0020"), srvRand,
            hexToBytes("002b00020304")
        )

        val body = concat(byteArrayOf(0x02), int24(srv.size), srv)
        val ccs = byteArrayOf(0x14, 0x03, 0x03, 0x00, 0x01, 0x01)
        val fake = ByteArray(512).also { random.nextBytes(it) }
        val app = concat(byteArrayOf(0x17, 0x03, 0x03), short16(fake.size), fake)

        val pkt = concat(
            byteArrayOf(0x16, 0x03, 0x03),
            short16(body.size + ccs.size + app.size),
            body, ccs, app
        )

        mac.reset()
        mac.init(SecretKeySpec(secret, "HmacSHA256"))
        val srvDig = mac.doFinal(concat(received, pkt))

        val finalPkt = pkt.copyOf()
        System.arraycopy(srvDig, 0, finalPkt, 7 + 3 + 2 + 11, dl)

        ctx.writeAndFlush(Unpooled.wrappedBuffer(finalPkt))
        return true
    }

    private fun processPre(pre: ByteArray): Boolean {
        if (!config.modes.classic && !config.modes.secure) return false

        val preIv = pre.sliceArray(8 until 56)
        val prekey = preIv.sliceArray(0 until 32)
        val iv = preIv.sliceArray(32 until 48)
        val rev = preIv.reversedArray()

        val decKey = AesCtrCryptor.deriveKey(prekey, secret)
        val dec = AesCtrCryptor(decKey, iv).process(pre)

        val tag = dec.sliceArray(56 until 60)
        val ok = tag.contentEquals(Constants.PROTO_TAG_ABRIDGED) ||
            tag.contentEquals(Constants.PROTO_TAG_INTERMEDIATE) ||
            tag.contentEquals(Constants.PROTO_TAG_SECURE)
        if (!ok) return false

        val sec = tag.contentEquals(Constants.PROTO_TAG_SECURE)
        if (sec && !config.modes.secure) return false
        if (!sec && !config.modes.classic) return false

        val raw = (dec[60].toInt() and 0xFF) or ((dec[61].toInt() and 0xFF) shl 8)
        val dc = if (raw and 0x8000 != 0) -(raw and 0x7FFF) else raw

        clientDecryptor = AesCtrCryptor(decKey, iv)
        clientEncryptor = AesCtrCryptor(
            AesCtrCryptor.deriveKey(rev.sliceArray(0 until 32), secret),
            rev.sliceArray(32 until 48)
        )

        val idx = kotlin.math.abs(dc) - 1
        dcAddress = Constants.TG_DC_V4[idx.coerceIn(0, 4)]
        return true
    }

    private fun connectToTg(ctx: ChannelHandlerContext, fh: RelayFrontendHandler) {
        val addr = dcAddress ?: return

        Bootstrap()
            .group(ctx.channel().eventLoop())
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(object : io.netty.channel.ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(RelayBackendHandler(ctx.channel(), fh))
                }
            })
            .connect(addr, Constants.TG_PORT)
            .addListener { future ->
                if (!future.isSuccess) {
                    ctx.close()
                    return@addListener
                }
                val tgCh = (future as io.netty.channel.ChannelFuture).channel()
                fh.tgChannel = tgCh

                val rnd = ByteArray(64).also { random.nextBytes(it) }
                System.arraycopy(Constants.PROTO_TAG_ABRIDGED, 0, rnd, 56, 4)

                val keyIv = rnd.sliceArray(8 until 56)
                val rev = keyIv.reversedArray()
                val tgEnc = AesCtrCryptor(
                    AesCtrCryptor.deriveKey(rev.sliceArray(0 until 32), secret),
                    rev.sliceArray(32 until 48)
                )

                val rndEnc = rnd.copyOf()
                val encTail = tgEnc.process(rnd, 56, 8)
                System.arraycopy(encTail, 0, rndEnc, 56, encTail.size)

                val tgDec = AesCtrCryptor(
                    AesCtrCryptor.deriveKey(keyIv.sliceArray(0 until 32), secret),
                    keyIv.sliceArray(32 until 48)
                )

                fh.tgDecryptor = tgDec
                fh.tgEncryptor = tgEnc
                tgCh.writeAndFlush(Unpooled.wrappedBuffer(rndEnc))
            }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ctx.close()
    }

    private fun fail(ctx: ChannelHandlerContext) {
        ctx.close()
    }

    companion object {
        private fun short16(v: Int) = byteArrayOf(((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte())
        private fun int24(v: Int) = byteArrayOf(((v shr 16) and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), (v and 0xFF).toByte())

        private fun concat(vararg arrays: ByteArray): ByteArray {
            var total = 0
            for (a in arrays) total += a.size
            val result = ByteArray(total)
            var pos = 0
            for (a in arrays) {
                System.arraycopy(a, 0, result, pos, a.size)
                pos += a.size
            }
            return result
        }
    }
}
