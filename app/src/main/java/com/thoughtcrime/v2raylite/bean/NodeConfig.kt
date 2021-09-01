package com.thoughtcrime.v2raylite.bean

import com.google.gson.annotations.SerializedName

data class NodeConfig(
    var v: String = "2",
    var ps: String = "Cfg_Export",
    var add: String,
    var port: String,
    var id: String,
    var aid: String = "4",
    var net: String = "tcp",
    var type: String = "none",
    var host: String = "",
    var path: String = "",
    var allowInsecure: Boolean = false,
    var _streamSecurity: String? = null,
    var _security: String? = null,
    var _configType: EConfigType? = null
) {
    val streamSecurity
        get() = _streamSecurity ?:""
    val security
        get() = _security?:"chacha20-poly1305"
    val configType
        get() = _configType?: EConfigType.VMESS
}
