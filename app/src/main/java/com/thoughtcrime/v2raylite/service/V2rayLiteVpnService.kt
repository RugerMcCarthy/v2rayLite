package com.thoughtcrime.v2raylite.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.StrictMode
import android.util.Log
import androidx.annotation.RequiresApi
import com.tencent.mmkv.MMKV
import com.thoughtcrime.v2raylite.R
import com.thoughtcrime.v2raylite.bean.AppConfig
import com.thoughtcrime.v2raylite.util.MmkvManager
import com.thoughtcrime.v2raylite.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.SoftReference

class V2rayLiteVpnService: VpnService(), ServiceControl {
    private val settingsStorage by lazy { MMKV.mmkvWithID(MmkvManager.ID_SETTING, MMKV.MULTI_PROCESS_MODE) }

    private lateinit var mInterface: ParcelFileDescriptor

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkRequest by lazy {
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
            .build()
    }

    private val connectivity by lazy { getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    @delegate:RequiresApi(Build.VERSION_CODES.P)
    private val defaultNetworkCallback by lazy {
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                setUnderlyingNetworks(arrayOf(network))
            }
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // it's a good idea to refresh capabilities
                setUnderlyingNetworks(arrayOf(network))
            }
            override fun onLost(network: Network) {
                setUnderlyingNetworks(null)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        V2RayServiceManager.serviceControl = SoftReference(this)
    }

    override fun onRevoke() {
        stopV2Ray()
    }

    override fun onLowMemory() {
        stopV2Ray()
        super.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopV2Ray()
    }

    private fun setup(parameters: String) {

        val prepare = prepare(this)
        if (prepare != null) {
            return
        }

        val builder = Builder()
        val enableLocalDns = settingsStorage?.decodeBool(AppConfig.PREF_LOCAL_DNS_ENABLED) ?: false
        val routingMode = settingsStorage?.decodeString(AppConfig.PREF_ROUTING_MODE) ?: "0"

        parameters.split(" ")
            .map { it.split(",") }
            .forEach {
                when (it[0][0]) {
                    'm' -> builder.setMtu(java.lang.Short.parseShort(it[1]).toInt())
                    's' -> builder.addSearchDomain(it[1])
                    'a' -> builder.addAddress(it[1], Integer.parseInt(it[2]))
                    'r' -> {
                        if (routingMode == "1" || routingMode == "3") {
                            if (it[1] == "::") { //not very elegant, should move Vpn setting in Kotlin, simplify go code
                                builder.addRoute("2000::", 3)
                            } else {
                                resources.getStringArray(R.array.bypass_private_ip_address).forEach { cidr ->
                                    val addr = cidr.split('/')
                                    builder.addRoute(addr[0], addr[1].toInt())
                                }
                            }
                        } else {
                            builder.addRoute(it[1], Integer.parseInt(it[2]))
                        }
                    }
                    'd' -> builder.addDnsServer(it[1])
                }
            }

        if(!enableLocalDns) {
            Utils.getVpnDnsServers()
                .forEach {
                    if (Utils.isPureIpAddress(it)) {
                        builder.addDnsServer(it)
                    }
                }
        }

        builder.setSession(V2RayServiceManager.currentConfig?.remarks.orEmpty())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            settingsStorage?.decodeBool(AppConfig.PREF_PER_APP_PROXY) == true) {
            val apps = settingsStorage?.decodeStringSet(AppConfig.PREF_PER_APP_PROXY_SET)
            val bypassApps = settingsStorage?.decodeBool(AppConfig.PREF_BYPASS_APPS) ?: false
            apps?.forEach {
                try {
                    if (bypassApps)
                        builder.addDisallowedApplication(it)
                    else
                        builder.addAllowedApplication(it)
                } catch (e: PackageManager.NameNotFoundException) {
                }
            }
        }

        try {
            mInterface.close()
        } catch (ignored: Exception) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectivity.requestNetwork(defaultNetworkRequest, defaultNetworkCallback)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        try {
            mInterface = builder.establish()!!
        } catch (e: Exception) {
            e.printStackTrace()
            stopV2Ray()
        }

        sendFd()
    }

    private fun sendFd() {
        val fd = mInterface.fileDescriptor
        val path = File(Utils.packagePath(applicationContext), "sock_path").absolutePath

        GlobalScope.launch(Dispatchers.IO) {
            var tries = 0
            while (true) try {
                Thread.sleep(1000L shl tries)
                Log.d(packageName, "sendFd tries: $tries")
                LocalSocket().use { localSocket ->
                    localSocket.connect(LocalSocketAddress(path, LocalSocketAddress.Namespace.FILESYSTEM))
                    localSocket.setFileDescriptorsForSend(arrayOf(fd))
                    localSocket.outputStream.write(42)
                }
                break
            } catch (e: Exception) {
                Log.d(packageName, e.toString())
                if (tries > 5) break
                tries += 1
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        V2RayServiceManager.startV2rayPoint()
        return START_STICKY
    }

    private fun stopV2Ray(isForced: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                connectivity.unregisterNetworkCallback(defaultNetworkCallback)
            } catch (ignored: Exception) {
            }
        }

        V2RayServiceManager.stopV2rayPoint()

        if (isForced) {
            stopSelf()

            try {
                mInterface.close()
            } catch (ignored: Exception) {
            }

        }
    }

    override fun getService(): Service {
        return this
    }

    override fun startService(parameters: String) {
        setup(parameters)
    }

    override fun stopService() {
        stopV2Ray(true)
    }

    override fun vpnProtect(socket: Int): Boolean {
        return protect(socket)
    }
}