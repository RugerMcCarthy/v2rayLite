package com.thoughtcrime.v2raylite.bean

data class NodeConfig(
    val remarks: String,
    val address: String,
    val port: String,
    var uuid: String,
    var alterid: String,
    var transport: String = "tcp",
    var headerType: String = "none",
    val requestHost: String = "",
    var path: String = "",
    var allowInsecure: Boolean = false,
    var streamSecurity: String = "",
    var security: String = "chacha20-poly1305",
    val configType: EConfigType = EConfigType.VMESS
)
