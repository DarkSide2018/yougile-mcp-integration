package com.yougile.mtprotoproxy.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesCtrCryptor(key: ByteArray, iv: ByteArray) {
    private val cipher: Cipher = Cipher.getInstance("AES/CTR/NoPadding")

    init {
        require(key.size == 32)
        require(iv.size == 16)
        val spec = IvParameterSpec(iv.copyOf())
        val keySpec = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec)
    }

    fun process(data: ByteArray): ByteArray =
        cipher.update(data) ?: ByteArray(0)

    fun process(data: ByteArray, offset: Int, length: Int): ByteArray =
        cipher.update(data, offset, length) ?: ByteArray(0)

    companion object {
        fun deriveKey(prekey: ByteArray, secret: ByteArray): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(prekey)
            md.update(secret)
            return md.digest()
        }
    }
}

fun hexToBytes(hex: String): ByteArray {
    val len = hex.length / 2
    val result = ByteArray(len)
    for (i in 0 until len) {
        result[i] = (hex.substring(i * 2, i * 2 + 2).toInt(16) and 0xFF).toByte()
    }
    return result
}
