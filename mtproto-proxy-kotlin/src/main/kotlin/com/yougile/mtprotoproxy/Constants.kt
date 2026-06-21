package com.yougile.mtprotoproxy

object Constants {

    val PROTO_TAG_ABRIDGED = byteArrayOf(0xef.toByte(), 0xef.toByte(), 0xef.toByte(), 0xef.toByte())
    val PROTO_TAG_INTERMEDIATE = byteArrayOf(0xee.toByte(), 0xee.toByte(), 0xee.toByte(), 0xee.toByte())
    val PROTO_TAG_SECURE = byteArrayOf(0xdd.toByte(), 0xdd.toByte(), 0xdd.toByte(), 0xdd.toByte())

    const val TG_PORT = 443

    val TG_DC_V4 = arrayOf(
        "149.154.175.50", "149.154.167.51", "149.154.175.100",
        "149.154.167.91", "149.154.171.5"
    )
}
