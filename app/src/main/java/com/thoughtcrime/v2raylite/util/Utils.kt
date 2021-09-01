package com.thoughtcrime.v2raylite.util

import android.content.Context
import android.util.Base64
import com.thoughtcrime.v2raylite.bean.AppConfig
import java.util.*

object Utils {

    /**
     * uuid
     */
    fun getUuid(): String {
        try {
            return UUID.randomUUID().toString().replace("-", "")
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }


    /**
     * is ip address
     */
    fun isIpAddress(value: String): Boolean {
        try {
            var addr = value
            if (addr.isEmpty() || addr.isBlank()) {
                return false
            }
            //CIDR
            if (addr.indexOf("/") > 0) {
                val arr = addr.split("/")
                if (arr.count() == 2 && Integer.parseInt(arr[1]) > 0) {
                    addr = arr[0]
                }
            }

            // "::ffff:192.168.173.22"
            // "[::ffff:192.168.173.22]:80"
            if (addr.startsWith("::ffff:") && '.' in addr) {
                addr = addr.drop(7)
            } else if (addr.startsWith("[::ffff:") && '.' in addr) {
                addr = addr.drop(8).replace("]", "")
            }

            // addr = addr.toLowerCase()
            var octets = addr.split('.').toTypedArray()
            if (octets.size == 4) {
                if(octets[3].indexOf(":") > 0) {
                    addr = addr.substring(0, addr.indexOf(":"))
                }
                return isIpv4Address(addr)
            }

            // Ipv6addr [2001:abc::123]:8080
            return isIpv6Address(addr)
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    fun isPureIpAddress(value: String): Boolean {
        return (isIpv4Address(value) || isIpv6Address(value))
    }

    fun isIpv4Address(value: String): Boolean {
        val regV4 = Regex("^([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])\\.([01]?[0-9]?[0-9]|2[0-4][0-9]|25[0-5])$")
        return regV4.matches(value)
    }

    fun isIpv6Address(value: String): Boolean {
        var addr = value
        if (addr.indexOf("[") == 0 && addr.lastIndexOf("]") > 0) {
            addr = addr.drop(1)
            addr = addr.dropLast(addr.count() - addr.lastIndexOf("]"))
        }
        val regV6 = Regex("^((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*::((?:[0-9A-Fa-f]{1,4}))?((?::[0-9A-Fa-f]{1,4}))*|((?:[0-9A-Fa-f]{1,4}))((?::[0-9A-Fa-f]{1,4})){7}$")
        return regV6.matches(addr)
    }

    /**
     * parseInt
     */
    fun parseInt(str: String): Int {
        try {
            return Integer.parseInt(str)
        } catch (e: Exception) {
            e.printStackTrace()
            return 0
        }
    }


    /**
     * package path
     */
    fun packagePath(context: Context): String {
        var path = context.filesDir.toString()
        path = path.replace("files", "")
        //path += "tun2socks"

        return path
    }


    fun getVpnDnsServers(): List<String> {
        val vpnDns = AppConfig.DNS_AGENT
        return vpnDns.split(",").filter { isPureIpAddress(it) }
        // allow empty, in that case dns will use system default
    }


    /**
     * readTextFromAssets
     */
    fun readTextFromAssets(context: Context, fileName: String): String {
        val content = context.assets.open(fileName).bufferedReader().use {
            it.readText()
        }
        return content
    }


    /**
     * get remote dns servers from preference
     */
    fun getRemoteDnsServers(): List<String> {
        val remoteDns = AppConfig.DNS_AGENT
        val ret = remoteDns.split(",").filter { isPureIpAddress(it) || it.startsWith("https") }
        if (ret.isEmpty()) {
            return listOf(AppConfig.DNS_AGENT)
        }
        return ret
    }


    /**
     * get remote dns servers from preference
     */
    fun getDomesticDnsServers(): List<String> {
        val domesticDns = AppConfig.DNS_DIRECT
        val ret = domesticDns.split(",").filter { isPureIpAddress(it) || it.startsWith("https") }
        if (ret.isEmpty()) {
            return listOf(AppConfig.DNS_DIRECT)
        }
        return ret
    }

    fun base64Decode(str: String): String {
        return String(Base64.decode(str, Base64.DEFAULT))
    }


    fun convertIp(ip: Int): String {
        return String.format(
            "%s.%s.%s.%s", ip shr 24 and 0x00FF,
            ip shr 16 and 0x00FF, ip shr 8 and 0x00FF, ip and 0x00FF
        )
    }
}