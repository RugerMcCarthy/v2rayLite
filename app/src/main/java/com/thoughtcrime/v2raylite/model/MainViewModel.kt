package com.thoughtcrime.v2raylite.model

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.material.ScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tencent.mmkv.MMKV
import com.thoughtcrime.v2raylite.App
import com.thoughtcrime.v2raylite.MainActivity
import com.thoughtcrime.v2raylite.R
import com.thoughtcrime.v2raylite.bean.AppConfig
import com.thoughtcrime.v2raylite.bean.EConfigType
import com.thoughtcrime.v2raylite.bean.NodeConfig
import com.thoughtcrime.v2raylite.bean.NodeInfo
import com.thoughtcrime.v2raylite.bean.NodeSelectStatus
import com.thoughtcrime.v2raylite.bean.NodeState
import com.thoughtcrime.v2raylite.bean.ServerConfig
import com.thoughtcrime.v2raylite.bean.V2rayConfig
import com.thoughtcrime.v2raylite.network.CardPlatformRepo
import com.thoughtcrime.v2raylite.service.V2RayServiceManager
import com.thoughtcrime.v2raylite.util.MessageUtil
import com.thoughtcrime.v2raylite.util.MmkvManager
import com.thoughtcrime.v2raylite.util.Utils
import com.thoughtcrime.v2raylite.util.toast
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.io.StringReader
import javax.inject.Inject

data class DeleteProxyNodeDialogState(val isShow: Boolean, val deleteNodeIndex: Int = - 1)

@HiltViewModel
class MainViewModel @Inject constructor(application: Application, private val cardPlatformRepo: CardPlatformRepo): AndroidViewModel(application) {
    val ID_MAIN = "MAIN"
    val ID_SERVER_CONFIG = "SERVER_CONFIG"
    val KEY_SELECTED_SERVER = "SELECTED_SERVER"
    val KEY_ANG_CONFIGS = "ANG_CONFIGS"

    var inputUidText by mutableStateOf("")
    var inputAliasText by mutableStateOf("")

    val mainStorage by lazy { MMKV.mmkvWithID(ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    val serverStorage by lazy { MMKV.mmkvWithID(ID_SERVER_CONFIG, MMKV.MULTI_PROCESS_MODE) }

    private var currentNodeConfig: NodeConfig? by mutableStateOf(null)

    var scaffoldState: ScaffoldState? = null

    var urlCheckDialogState by mutableStateOf(false)
    private set

    var deleteProxyNodeDialogState by mutableStateOf(DeleteProxyNodeDialogState(false, -1))
    private set

    var currentSelectedNodeIndex = 0
    private set

    var nextSelectedNodeIndex by mutableStateOf(0)

    var nodeStateList = mutableStateListOf<NodeState>()

    var isRunning: Boolean by mutableStateOf(false)
    private set

    private var _vpnEventFlow = MutableStateFlow(false)

    var vpnEventFlow = _vpnEventFlow.asStateFlow()

    var isChanging = false
    private set

    private val mMsgReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.getIntExtra("key", 0)) {
                AppConfig.MSG_STATE_RUNNING -> {
                    isRunning = true
                }
                AppConfig.MSG_STATE_NOT_RUNNING -> {
                    isRunning = false
                }
                AppConfig.MSG_STATE_START_SUCCESS -> {
                    isRunning = true
                }
                AppConfig.MSG_STATE_START_FAILURE -> {
                    isRunning = false
                }
                AppConfig.MSG_STATE_STOP_SUCCESS -> {
                    isRunning = false
                }
            }
        }
    }

    fun showToast(content: String) {
        viewModelScope.launch {
            scaffoldState?.snackbarHostState?.showSnackbar(content)
        }
    }

    fun startListenBroadcast() {
        getApplication<App>().registerReceiver(mMsgReceiver, IntentFilter(AppConfig.BROADCAST_ACTION_ACTIVITY))
        MessageUtil.sendMsg2Service(getApplication(), AppConfig.MSG_REGISTER_CLIENT, "")
    }

    override fun onCleared() {
        getApplication<App>().unregisterReceiver(mMsgReceiver)
        super.onCleared()
    }

    suspend fun changeProxyNode(context: Context) {
        isChanging = true
        if (currentSelectedNodeIndex == nextSelectedNodeIndex) {
            isChanging = false
            return
        }
        if (processChangeProxyNodeState(context)) {
            var guid = nodeStateList[currentSelectedNodeIndex].nodeInfo.guid
            mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
        }
        isChanging = false
        // Log.d("gzz", "change $guid")
    }

    private suspend fun processChangeProxyNodeState(context: Context): Boolean {
        if (nodeStateList.size == 0) return false
        nodeStateList[currentSelectedNodeIndex].unSelect()
        if (isRunning) {
            withContext(Dispatchers.Main) {
                stopV2Ray(context)
                delay(500)
                startV2Ray(context)
                currentSelectedNodeIndex = nextSelectedNodeIndex
                nodeStateList[currentSelectedNodeIndex].select()
            }
        } else {
            currentSelectedNodeIndex = nextSelectedNodeIndex
            nodeStateList[currentSelectedNodeIndex].select()
        }
        return true
    }

    private fun generateV2rayNodeConfig(nodeConfigList: List<NodeConfig>) {
        // removeServerList()
        nodeConfigList.forEach {
            val config = ServerConfig.create(it.configType)
            config.remarks = it.ps
            config.outboundBean?.settings?.vnext?.get(0)?.let { vnext ->
                saveVnext(vnext, config, it)
            }
            config.outboundBean?.streamSettings?.let { streamSettings->
                saveStreamSettings(streamSettings, config, it)
            }
            var guid = MmkvManager.encodeServerConfig("", config)
            nodeStateList.add(
                NodeState(
                    NodeInfo(
                        remark = config.remarks,
                        guid = guid
                    ),
                    nodeIndex = nodeStateList.size,
                    selectStatus = if (nodeStateList.isEmpty()) NodeSelectStatus.Selected else NodeSelectStatus.Unselected
                )
            )
        }
    }

    private fun saveVnext(vnext: V2rayConfig.OutboundBean.OutSettingsBean.VnextBean, serverConfig: ServerConfig, nodeConfig: NodeConfig) {
        vnext.address = nodeConfig.add.trim()
        vnext.port = Utils.parseInt(nodeConfig.port)
        vnext.users[0].id = nodeConfig.id.trim()
        vnext.users[0].alterId = Utils.parseInt(nodeConfig.aid)
        vnext.users[0].security = nodeConfig.security
    }

    private fun saveStreamSettings(streamSetting: V2rayConfig.OutboundBean.StreamSettingsBean, serverConfig: ServerConfig, nodeConfig: NodeConfig) {
        var sni = streamSetting.populateTransportSettings(
            transport = nodeConfig.net,
            headerType = nodeConfig.net,
            host = nodeConfig.host,
            path = nodeConfig.path,
            seed = nodeConfig.path,
            quicSecurity = nodeConfig.host,
            key = nodeConfig.path,
            mode = nodeConfig.type,
            serviceName = nodeConfig.path
        )

        streamSetting.populateTlsSettings(
            nodeConfig.streamSecurity,
            nodeConfig.allowInsecure,
            sni
        )
    }

    fun clearDeletedNode() {
        nodeStateList.forEach {
            if (it.isDeleted) {
                MmkvManager.removeServer(it.nodeInfo.guid)
            }
        }
        nodeStateList.clear()
    }

    fun reloadServerList() {
        var serverList = MmkvManager.decodeServerList()
        var selectedGuid = mainStorage?.decodeString(MmkvManager.KEY_SELECTED_SERVER)
        serverList.forEach { guid ->
            MmkvManager.decodeServerConfig(guid)?.let {
                nodeStateList.add(
                    NodeState(
                        NodeInfo(
                            remark = it.remarks,
                            guid = guid
                        ),
                        nodeIndex = nodeStateList.size,
                        selectStatus = if (selectedGuid == guid) NodeSelectStatus.Selected else NodeSelectStatus.Unselected
                    ).also {
                        if (it.isSelected()) {
                            Log.d("gzz", "reload $guid")
                            mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
                            currentSelectedNodeIndex = it.nodeIndex
                            nextSelectedNodeIndex = it.nodeIndex
                        }
                    }
                )
            }
        }
    }

    private fun removeServerList() {
        var serverList = MmkvManager.decodeServerList()
        serverList.forEach { guid ->
            MmkvManager.removeServer(guid)
        }
        nodeStateList.clear()
        currentSelectedNodeIndex = 0
        nextSelectedNodeIndex = 0
    }

    fun toggleVpn() {
        _vpnEventFlow.value = !isRunning
    }

    fun startV2Ray(context: Context) {
        V2RayServiceManager.startV2Ray(context,::showToast)
    }

    fun stopV2Ray(context: Context) {
        if (!isRunning) return
        showToast(context.getString(R.string.toast_services_stop))
        MessageUtil.sendMsg2Service(context, AppConfig.MSG_STATE_STOP, "")
    }

    fun showUrlCheckDialog() {
        urlCheckDialogState = true
    }

    fun hideUrlCheckDialog() {
        urlCheckDialogState = false
    }

    fun showDeleteProxyNodeDialog(context: Context, deleteNodeIndex: Int) {
        if (currentSelectedNodeIndex == deleteNodeIndex) {
            context.toast("不能删除已选中的节点")
            return
        }
        deleteProxyNodeDialogState = DeleteProxyNodeDialogState(true, deleteNodeIndex)
    }

    fun hideDeleteProxyNodeDialog() {
        deleteProxyNodeDialogState = DeleteProxyNodeDialogState(false)
    }

    fun deleteProxyNode(deleteNodeIndex: Int) {
        nodeStateList[deleteNodeIndex].delete()
    }

    fun parseURl(base64configInfo: String): Boolean {
        var configInfoJson = Utils.base64Decode(base64configInfo)
        try {
            var config = Gson().fromJson(configInfoJson, NodeConfig::class.java)
            generateV2rayNodeConfig(listOf(config))
            // Log.d("gzz", "${configs.toString()}")
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun parseUidByPlatform(context: Context, uid:String) {
        viewModelScope.launch(Dispatchers.Default) {
            var response = cardPlatformRepo.getPointConfigByUid(uid)
            if (response.code != 200 || !response.isSuccessful)  {
                context.toast("本地网络似乎出现了问题～")
                return@launch
            }
            val jsonObject =
                response.body?.let { Json.parseToJsonElement(it.string()) }
            val successCode = jsonObject?.jsonObject?.get("success")?.toString()
            val currentConfig = jsonObject?.jsonObject?.get("current")
            if (successCode != "0") {
                context.toast("输入UID有误～")
                return@launch
            }
            var uuid = currentConfig?.jsonObject?.get("uuid")?.toString()?.replace("\"", "")
            var add = currentConfig?.jsonObject?.get("IP")?.toString()?.replace("\"", "")
            var port = currentConfig?.jsonObject?.get("V2Port")?.toString()

            if (uuid == null || add == null || port == null) {
                context.toast("平台下发字段缺失～")
                return@launch
            }
            currentNodeConfig = NodeConfig(
                add = add,
                port = port,
                id = uuid,
            )
//            generateV2rayNodeConfig(listOf(config))
//            hideUrlCheckDialog()
        }
    }
    fun isNeedConfigAlias() = currentNodeConfig != null

    fun clearNodeConfig() {
        currentNodeConfig = null
    }

    fun configAlias(alias: String) {
        currentNodeConfig?.ps = alias
        generateV2rayNodeConfig(listOf(currentNodeConfig!!))
        clearInputText()
        clearNodeConfig()
        hideUrlCheckDialog()
    }

    fun clearInputText() {
        inputUidText = ""
        inputAliasText = ""
    }
}