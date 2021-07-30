package com.thoughtcrime.v2raylite.model

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.tencent.mmkv.MMKV
import com.thoughtcrime.v2raylite.bean.EConfigType
import com.thoughtcrime.v2raylite.bean.NodeConfig
import com.thoughtcrime.v2raylite.bean.NodeInfo
import com.thoughtcrime.v2raylite.bean.NodeSelectStatus
import com.thoughtcrime.v2raylite.bean.NodeState
import com.thoughtcrime.v2raylite.bean.ServerConfig
import com.thoughtcrime.v2raylite.bean.V2rayConfig
import com.thoughtcrime.v2raylite.util.MmkvManager
import com.thoughtcrime.v2raylite.util.Utils

class MainViewModel: ViewModel() {
    val ID_MAIN = "MAIN"
    val ID_SERVER_CONFIG = "SERVER_CONFIG"
    val KEY_SELECTED_SERVER = "SELECTED_SERVER"
    val KEY_ANG_CONFIGS = "ANG_CONFIGS"

    val mainStorage by lazy { MMKV.mmkvWithID(ID_MAIN, MMKV.MULTI_PROCESS_MODE) }
    val serverStorage by lazy { MMKV.mmkvWithID(ID_SERVER_CONFIG, MMKV.MULTI_PROCESS_MODE) }

    var currentSelectedNodeIndex = 0
    private set

    var nextSelectedNodeIndex by mutableStateOf(0)

    var nodeStateList = mutableStateListOf<NodeState>()

    var isRunning: Boolean by mutableStateOf(false)

    var weakNodeConfigList = listOf<NodeConfig>(
        NodeConfig(
            remarks = "俄罗斯",
            address = "zz.ru1v.se.sx.cn",
            port = "7001",
            alterid = "1",
            uuid = "f5262b6f-712c-37b9-aebb-2504a8087075"
        ),
        NodeConfig(
            remarks = "台湾",
            address = "zz.tw1v.se.sx.cn",
            port = "5001",
            alterid = "1",
            uuid = "f5262b6f-712c-37b9-aebb-2504a8087075"
        ),
        NodeConfig(
            remarks = "新加坡",
            address = "zz.sg1v.se.sx.cn",
            port = "3001",
            alterid = "1",
            uuid = "f5262b6f-712c-37b9-aebb-2504a8087075"
        )
    )

    suspend fun changeNode() {
        if (currentSelectedNodeIndex == nextSelectedNodeIndex) {
            return
        }
        execChangeNodeTransitionAnim()
        var guid = nodeStateList[currentSelectedNodeIndex].nodeInfo.guid
        mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
        Log.d("gzz", "change $guid")
    }

    private suspend fun execChangeNodeTransitionAnim() {
        if (nodeStateList.size == 0) return
        nodeStateList[currentSelectedNodeIndex].unSelect()
        currentSelectedNodeIndex = nextSelectedNodeIndex
        nodeStateList[currentSelectedNodeIndex].select()
    }

    fun generateV2rayNodeConfig() {
        removeServerList()
        weakNodeConfigList.forEach {
            val config = ServerConfig.create(it.configType)
            config.remarks = it.remarks
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
        vnext.address = nodeConfig.address.trim()
        vnext.port = Utils.parseInt(nodeConfig.port)
        vnext.users[0].id = nodeConfig.uuid.trim()
        vnext.users[0].alterId = Utils.parseInt(nodeConfig.alterid)
        vnext.users[0].security = nodeConfig.security
    }

    private fun saveStreamSettings(streamSetting: V2rayConfig.OutboundBean.StreamSettingsBean, serverConfig: ServerConfig, nodeConfig: NodeConfig) {
        var sni = streamSetting.populateTransportSettings(
            transport = nodeConfig.transport,
            headerType = nodeConfig.headerType,
            host = nodeConfig.requestHost,
            path = nodeConfig.path,
            seed = nodeConfig.path,
            quicSecurity = nodeConfig.requestHost,
            key = nodeConfig.path,
            mode = nodeConfig.headerType,
            serviceName = nodeConfig.path
        )

        streamSetting.populateTlsSettings(
            nodeConfig.streamSecurity,
            nodeConfig.allowInsecure,
            sni
        )
    }

    fun reloadServerList() {
        var serverList = MmkvManager.decodeServerList()
        serverList.forEach { guid ->
            MmkvManager.decodeServerConfig(guid)?.let {
                nodeStateList.add(
                    NodeState(
                        NodeInfo(
                            remark = it.remarks,
                            guid = guid
                        ),
                        nodeIndex = nodeStateList.size,
                        selectStatus = if (nodeStateList.isEmpty()) NodeSelectStatus.Selected else NodeSelectStatus.Unselected
                    ).also {
                        if (it.isSelected()) {
                            Log.d("gzz", "reload $guid")
                            mainStorage?.encode(MmkvManager.KEY_SELECTED_SERVER, guid)
                        }
                    }
                )
            }
        }
    }

    fun removeServerList() {
        var serverList = MmkvManager.decodeServerList()
        serverList.forEach { guid ->
            MmkvManager.removeServer(guid)
        }
        nodeStateList.clear()
        currentSelectedNodeIndex = 0
        nextSelectedNodeIndex = 0
    }

}